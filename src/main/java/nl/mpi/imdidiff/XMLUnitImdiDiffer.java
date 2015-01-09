package nl.mpi.imdidiff;

import com.google.common.base.Converter;
import com.google.common.base.Predicate;
import com.google.common.base.Strings;
import com.google.common.collect.Collections2;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;
import org.custommonkey.xmlunit.DetailedDiff;
import org.custommonkey.xmlunit.Diff;
import org.custommonkey.xmlunit.Difference;
import org.custommonkey.xmlunit.DifferenceConstants;
import org.custommonkey.xmlunit.DifferenceListener;
import org.custommonkey.xmlunit.XMLUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Attr;
import org.w3c.dom.Node;
import org.w3c.dom.Text;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

/**
 *
 * @author Twan Goosen <twan.goosen@mpi.nl>
 */
public class XMLUnitImdiDiffer implements ImdiDiffer {

    public final static Set<Pattern> PATHS_TO_IGNORE = ImmutableSet.of(
            Pattern.compile(".*/@Type"),
            Pattern.compile(".*/@Link"),
            Pattern.compile("/METATRANSCRIPT\\[1\\]/@Originator"),
            Pattern.compile("/METATRANSCRIPT\\[1\\]/@Version")
    );

    public final static Set<Integer> DIFFERENCES_TO_IGNORE = ImmutableSet.of(
            DifferenceConstants.ELEMENT_NUM_ATTRIBUTES_ID,
            DifferenceConstants.CHILD_NODELIST_LENGTH_ID,
            DifferenceConstants.HAS_CHILD_NODES_ID,
            DifferenceConstants.ATTR_NAME_NOT_FOUND_ID
    );

    private final static Logger logger = LoggerFactory.getLogger(XMLUnitImdiDiffer.class);
    private final DifferenceListener diffListener;

    public XMLUnitImdiDiffer() {
        this.diffListener = new ImdiDifferenceListener();
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

        @Override
        public int differenceFound(Difference difference) {
            final String controlLocation = difference.getControlNodeDetail().getXpathLocation();
            final String testLocation = difference.getTestNodeDetail().getXpathLocation();
            if (controlLocation != null && !controlLocation.equals(testLocation)) {
                // paths must be the same
                return RETURN_IGNORE_DIFFERENCE_NODES_SIMILAR;
            } else if (DIFFERENCES_TO_IGNORE.contains(difference.getId())) {
                // must not be an ignored difference
                return RETURN_IGNORE_DIFFERENCE_NODES_SIMILAR;
            }
            if (matchesIgnoredPathPatterns(controlLocation) || matchesIgnoredPathPatterns(testLocation)) {
                // must not be an ignored path
                return RETURN_IGNORE_DIFFERENCE_NODES_SIMILAR;
            } else {
                final Node controlNode = difference.getControlNodeDetail().getNode();
                final Node testNode = difference.getTestNodeDetail().getNode();
                if (archiveHandlePostfixAdded(controlLocation, controlNode, testNode)) {
                    // format identifier added to handle is ok
                    return RETURN_IGNORE_DIFFERENCE_NODES_SIMILAR;
                }
                if (resourceLinkContentSimilar(controlNode, testNode)) {
                    // ignore changed path (mainly relative to absolute)
                    return RETURN_IGNORE_DIFFERENCE_NODES_SIMILAR;
                }
                if (skippedEmptyValue(controlNode, testNode)) {
                    // ignore skipped empty values
                    return RETURN_IGNORE_DIFFERENCE_NODES_SIMILAR;
                }
                if (languageCodeUpgrade(controlNode, testNode)) {
                    // ignore language code change 639-2 -> 639-3
                    return RETURN_IGNORE_DIFFERENCE_NODES_SIMILAR;
                }
            }
            return DifferenceListener.RETURN_ACCEPT_DIFFERENCE;

        }

        private boolean matchesIgnoredPathPatterns(final String xpathLocation) {
            if (xpathLocation == null) {
                return false;
            } else {
                // does any of the skipped path patterns match the provided path?
                return Iterables.any(PATHS_TO_IGNORE, new Predicate<Pattern>() {

                    @Override
                    public boolean apply(Pattern input) {
                        return input.matcher(xpathLocation).matches();
                    }

                });
            }
        }

        private boolean skippedEmptyValue(Node controlNode, Node testNode) {
            return controlNode != null
                    && Strings.isNullOrEmpty(controlNode.getNodeValue())
                    && testNode == null;
        }

        private boolean languageCodeUpgrade(Node controlNode, Node testNode) {
            return controlNode instanceof Attr && testNode instanceof Attr
                    && controlNode.getLocalName().equals("LanguageId")
                    && controlNode.getNodeValue().startsWith("ISO639-2")
                    && testNode.getNodeValue().startsWith("ISO639-3")
                    // language code must be the same
                    && controlNode.getNodeValue().replace("ISO639-2", "").equals(testNode.getNodeValue().replace("ISO639-3", ""));
        }

        private boolean archiveHandlePostfixAdded(String controlLocation, Node controlNode, Node testNode) {
            return "/METATRANSCRIPT[1]/@ArchiveHandle".equals(controlLocation)
                    && (controlNode.getNodeValue() + "@format=imdi").equals(testNode.getNodeValue());
        }

        private boolean resourceLinkContentSimilar(Node controlNode, Node testNode) {
            if (controlNode instanceof Text
                    && "ResourceLink".equals(controlNode.getParentNode().getNodeName())
                    && "ResourceLink".equals(testNode.getParentNode().getNodeName())) {
                final String controlNodeValue = controlNode.getNodeValue();
                final String testNodeValue = testNode.getNodeValue();

                // strip everything up to last slash
                return controlNodeValue.replaceAll(".*/", "").equals(testNodeValue.replaceAll(".*/", ""));
            }

            return false;

        }

        @Override
        public void skippedComparison(Node arg0, Node arg1) {
            logger.trace("Skipped comparison for {} and {}", arg0.getNodeName(), arg1.getNodeName());
        }

    }

}
