package nl.mpi.imdidiff;

import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 *
 * @author Twan Goosen <twan.goosen@mpi.nl>
 */
public class ImdiDiffRunner {

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) throws IOException {
        if (args.length != 2) {
            System.err.println("Please provide two directory names as parameters");
            System.exit(1);
        }
        final Path dir1 = getDirectory(args[0]);
        final Path dir2 = getDirectory(args[1]);
        
        final ImdiDiffer differ = new ImdiDiffer(dir1, dir2);
        differ.diff();
    }

    private static Path getDirectory(String dir) {
        final Path dirFile = FileSystems.getDefault().getPath(dir);
        if (!Files.isDirectory(dirFile)) {
            System.err.println(String.format("'%s' is not an existing directory", dir));
            System.exit(2);
        }
        return dirFile;
    }

}
