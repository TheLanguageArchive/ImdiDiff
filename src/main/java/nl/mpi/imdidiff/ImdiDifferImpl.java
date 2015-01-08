package nl.mpi.imdidiff;

import com.google.common.base.Converter;
import com.google.common.base.Predicate;
import com.google.common.collect.Collections2;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import org.custommonkey.xmlunit.DetailedDiff;
import org.custommonkey.xmlunit.Diff;
import org.custommonkey.xmlunit.Difference;
import org.custommonkey.xmlunit.DifferenceListener;
import org.custommonkey.xmlunit.XMLUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Node;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

/**
 *
 * @author Twan Goosen <twan.goosen@mpi.nl>
 */
public class ImdiDifferImpl implements ImdiDiffer {

    private final static Logger logger = LoggerFactory.getLogger(ImdiDifferImpl.class);
    private final DifferenceListener diffListener;

    public ImdiDifferImpl(Set<String> pathsToIgnore) {
        this.diffListener = new ImdiDifferenceListener(pathsToIgnore);
    }

    @Override
    public void initialise() {
        XMLUnit.setIgnoreAttributeOrder(true);
        XMLUnit.setIgnoreComments(true);
        XMLUnit.setIgnoreWhitespace(true);
        XMLUnit.setNormalizeWhitespace(true);
    }

    @Override
    public Collection<String> compare(Path source, Path target) throws IOException, SAXException {
        final InputSource sourceStream = new InputSource(Files.newBufferedReader(source));
        final InputSource targetStream = new InputSource(Files.newBufferedReader(target));

        final Diff diff = XMLUnit.compareXML(sourceStream, targetStream);
        final DetailedDiff detailedDiff = new DetailedDiff(diff);
        detailedDiff.overrideDifferenceListener(diffListener);

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

    private static class ImdiDifferenceListener implements DifferenceListener {

        private final Set<String> skippedPaths;

        public ImdiDifferenceListener(Set<String> skippedPaths) {
            this.skippedPaths = skippedPaths;
        }

        @Override
        public int differenceFound(Difference difference) {
            if (skippedPaths.contains(difference.getTestNodeDetail().getXpathLocation())) {
                return RETURN_IGNORE_DIFFERENCE_NODES_SIMILAR;
            } else {
                return DifferenceListener.RETURN_ACCEPT_DIFFERENCE;
            }
        }

        @Override
        public void skippedComparison(Node arg0, Node arg1) {
            logger.trace("Skipping comparison for {} and {}", arg0.getNodeName(), arg1.getNodeName());
        }

    }

}
