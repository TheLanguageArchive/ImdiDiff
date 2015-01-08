package nl.mpi.imdidiff;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;
import org.custommonkey.xmlunit.Diff;
import org.custommonkey.xmlunit.XMLUnit;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

/**
 *
 * @author Twan Goosen <twan.goosen@mpi.nl>
 */
public class ImdiDifferImpl implements ImdiDiffer {

    @Override
    public List<String> compare(Path source, Path target) throws IOException, SAXException {
        final InputSource sourceStream = new InputSource(Files.newBufferedReader(source));
        final InputSource targetStream = new InputSource(Files.newBufferedReader(target));
        final Diff diff = XMLUnit.compareXML(sourceStream, targetStream);
        //TODO: get *all* differences instead
        return Collections.singletonList(diff.toString());
    }

}
