package nl.mpi.imdidiff;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Collection;
import org.xml.sax.SAXException;

/**
 *
 * @author Twan Goosen <twan.goosen@mpi.nl>
 */
public class NormalisingImdiDiffer implements ImdiDiffer {

    @Override
    public void initialise() {
        throw new UnsupportedOperationException("Not supported yet."); 
    }

    @Override
    public Collection<String> compare(Path source, Path target) throws IOException, SAXException {
        // normalise both xmls, then compare
        throw new UnsupportedOperationException("Not supported yet."); 
    }
    
}
