package nl.mpi.imdidiff;

import com.google.common.base.Converter;
import com.google.common.base.Predicate;
import com.google.common.collect.Collections2;
import java.io.IOException;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import java.util.List;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;
import org.custommonkey.xmlunit.DetailedDiff;
import org.custommonkey.xmlunit.Diff;
import org.custommonkey.xmlunit.Difference;
import org.custommonkey.xmlunit.XMLUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.SAXException;

/**
 *
 * @author Twan Goosen <twan.goosen@mpi.nl>
 */
public class NormalisingImdiDiffer implements ImdiDiffer {

    private final static Logger logger = LoggerFactory.getLogger(NormalisingImdiDiffer.class);
    private Transformer transformer;

    @Override
    public void initialise() {
        XMLUnit.setIgnoreAttributeOrder(true);
        XMLUnit.setIgnoreComments(true);
        XMLUnit.setIgnoreWhitespace(true);
        XMLUnit.setNormalizeWhitespace(true);
        
        try {
            final StreamSource xsltSource = new StreamSource(getClass().getResourceAsStream("/normaliseImdi.xsl"));
            transformer = TransformerFactory.newInstance().newTransformer(xsltSource);
        } catch (TransformerConfigurationException ex) {
            throw new RuntimeException(ex);
        }
    }

    @Override
    public Collection<String> compare(Path source, Path target) throws IOException, SAXException, TransformerException {

        String normalisedSource = normalise(source);
        String normalisedTarget = normalise(target);

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
                return a.toString();
            }

            @Override
            protected Difference doBackward(String b) {
                throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
            }
        });
    }

    private String normalise(Path source) throws IOException, TransformerException {
        final StringWriter writer = new StringWriter();
        transformer.transform(new StreamSource(Files.newBufferedReader(source, StandardCharsets.UTF_8)), new StreamResult(writer));
        return writer.toString();
    }

}
