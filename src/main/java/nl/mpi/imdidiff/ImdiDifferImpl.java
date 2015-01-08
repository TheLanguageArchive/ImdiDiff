package nl.mpi.imdidiff;

import com.google.common.base.Converter;
import com.google.common.collect.Lists;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.custommonkey.xmlunit.DetailedDiff;
import org.custommonkey.xmlunit.Diff;
import org.custommonkey.xmlunit.Difference;
import org.custommonkey.xmlunit.XMLUnit;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

/**
 *
 * @author Twan Goosen <twan.goosen@mpi.nl>
 */
public class ImdiDifferImpl implements ImdiDiffer {


    @Override
    public void initialise() {
        XMLUnit.setIgnoreAttributeOrder(true);
        XMLUnit.setIgnoreComments(true);
        XMLUnit.setIgnoreWhitespace(true);
    }
    
    @Override
    public List<String> compare(Path source, Path target) throws IOException, SAXException {
        final InputSource sourceStream = new InputSource(Files.newBufferedReader(source));
        final InputSource targetStream = new InputSource(Files.newBufferedReader(target));
        final Diff diff = XMLUnit.compareXML(sourceStream, targetStream);
        final DetailedDiff detailedDiff = new DetailedDiff(diff);
        final List<Difference> differences = detailedDiff.getAllDifferences();
        return Lists.transform(differences, new Converter<Difference, String>() {

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

}
