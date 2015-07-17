package nl.mpi.imdidiff;

import com.google.common.base.Converter;
import com.google.common.base.Predicate;
import com.google.common.collect.Collections2;
import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import java.util.List;
import javax.xml.transform.ErrorListener;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.TransformerFactoryConfigurationError;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;
import net.sf.saxon.TransformerFactoryImpl;
import org.custommonkey.xmlunit.DetailedDiff;
import org.custommonkey.xmlunit.Diff;
import org.custommonkey.xmlunit.Difference;
import org.custommonkey.xmlunit.XMLUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

/**
 *
 * @author Twan Goosen <twan.goosen@mpi.nl>
 */
public class NormalisingImdiDiffer implements ImdiDiffer {

    private final static Logger logger = LoggerFactory.getLogger(NormalisingImdiDiffer.class);
    private final static String SAXON_MESSAGE_EMITTER_CLASSNAME = "net.sf.saxon.serialize.MessageWarner";
    private Transformer transformer;

    @Override
    public void initialise() {
        XMLUnit.setIgnoreAttributeOrder(true);
        XMLUnit.setIgnoreComments(true);
        XMLUnit.setIgnoreWhitespace(true);
        XMLUnit.setNormalizeWhitespace(true);

        try {
            final StreamSource xsltSource = new StreamSource(getClass().getResourceAsStream("/normaliseImdi.xsl"));
            transformer = getTransformerFactory().newTransformer(xsltSource);
        } catch (TransformerConfigurationException ex) {
            throw new RuntimeException(ex);
        }
    }

    @Override
    public Collection<String> compare(Path source, Path target) throws IOException, SAXException, TransformerException {

        final InputSource normalisedSource = normalise(source);
        final InputSource normalisedTarget = normalise(target);

        final Diff diff = XMLUnit.compareXML(normalisedSource, normalisedTarget);
        final DetailedDiff detailedDiff = new DetailedDiff(diff);
//        detailedDiff.overrideDifferenceListener(diffListener);

        final List<Difference> differences = detailedDiff.getAllDifferences();

        // filter out acceptable similarities
        final Collection<Difference> unsimilar = Collections2.filter(differences, new Predicate<Difference>() {

            @Override
            public boolean apply(Difference input) {
                return !input.isRecoverable();
            }
        });

        // apply toString to all differences
        return Collections2.transform(unsimilar, new Converter<Difference, String>() {

            @Override
            protected String doForward(Difference a) {
                return String.format("ID%d - %s", a.getId(), a.toString());
            }

            @Override
            protected Difference doBackward(String b) {
                throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
            }
        });
    }

    private InputSource normalise(Path input) throws IOException, TransformerException {
        // create input source from file
        final StreamSource inputSource = new StreamSource(Files.newBufferedReader(input, StandardCharsets.UTF_8));
        inputSource.setSystemId(input.toUri().toString());
        inputSource.setPublicId(input.getFileName().toString());
        
        // normalisation transformation
        final StringWriter writer = new StringWriter();
        transformer.transform(inputSource, new StreamResult(writer));

        // create a new input source
        final InputSource normalised = new InputSource(new StringReader(writer.toString()));
        normalised.setSystemId(input.toUri().toString());
        normalised.setPublicId("normalised-" + input.getFileName().toString());
        return normalised;
    }

    private TransformerFactory getTransformerFactory() throws TransformerFactoryConfigurationError {
        final TransformerFactory factory = TransformerFactory.newInstance();
        if (factory instanceof TransformerFactoryImpl) {
            configureSaxonTransfomerFactory((TransformerFactoryImpl) factory);
        }
        return factory;
    }

    private void configureSaxonTransfomerFactory(final TransformerFactoryImpl saxonFactory) throws IllegalArgumentException {
        // log saxon warnings and errors to our local logger
        saxonFactory.setErrorListener(new ErrorListener() {

            @Override
            public void warning(TransformerException exception) throws TransformerException {
                logger.warn("Saxon warning", exception);
            }

            @Override
            public void error(TransformerException exception) throws TransformerException {
                logger.error("Saxon error", exception);
            }

            @Override
            public void fatalError(TransformerException exception) throws TransformerException {
                logger.error("Saxon FATAL", exception);
            }
        });

        // make all saxon internal log message to to error listener
        saxonFactory.getConfiguration().setMessageEmitterClass(SAXON_MESSAGE_EMITTER_CLASSNAME);
    }
}
