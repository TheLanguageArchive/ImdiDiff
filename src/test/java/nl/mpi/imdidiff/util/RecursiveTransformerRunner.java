package nl.mpi.imdidiff.util;

import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Path;

/**
 *
 * @author Twan Goosen <twan.goosen@mpi.nl>
 */
public class RecursiveTransformerRunner {

    public static void main(String[] args) throws Exception {
        final FileSystem fs = FileSystems.getDefault();
        final Path stylesheet = fs.getPath("/Users/twan/git/MetadataTranslator/Translator/src/main/resources/templates/cmdi2imdi/cmdi2imdiMaster.xslt");
        final Path in = fs.getPath("/Users/twan/git/cmdi-conversion-checker/cmdi");
        final Path out = fs.getPath("/Users/twan/git/cmdi-conversion-checker/imdi-out");
        final String inExt = ".cmdi";
        final String outExt = ".imdi";
        final RecursiveTransformer transformer = new RecursiveTransformer(stylesheet, in, out, inExt, outExt);
        transformer.transform();
    }
}
