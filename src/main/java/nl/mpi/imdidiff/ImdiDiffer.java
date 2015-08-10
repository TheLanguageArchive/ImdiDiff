/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package nl.mpi.imdidiff;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Collection;
import javax.xml.transform.TransformerException;
import org.xml.sax.SAXException;

/**
 *
 * @author Twan Goosen <twan.goosen@mpi.nl>
 */
public interface ImdiDiffer {

    /**
     * Wildcard to indicate that an entire path should be skipped
     */
    public static final String SKIP_WILDCARD = "*";

    void initialise();

    Collection<String> compare(Path source, Path target) throws IOException, SAXException, TransformerException;

}
