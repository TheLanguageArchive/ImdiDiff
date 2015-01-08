/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package nl.mpi.imdidiff;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import org.xml.sax.SAXException;

/**
 *
 * @author Twan Goosen <twan.goosen@mpi.nl>
 */
public interface ImdiDiffer {

    List<String> compare(Path source, Path target) throws IOException, SAXException;
    
}
