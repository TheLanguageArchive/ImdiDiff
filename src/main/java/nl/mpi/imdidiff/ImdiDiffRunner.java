package nl.mpi.imdidiff;

import com.google.common.collect.ImmutableSet;
import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;
import java.util.regex.Pattern;

/**
 *
 * @author Twan Goosen <twan.goosen@mpi.nl>
 */
public class ImdiDiffRunner {

    public final static Set<Pattern> PATHS_TO_IGNORE = ImmutableSet.of();

    /**
     * @param args the command line arguments
     * @throws java.io.IOException in case of read failure or missing file
     */
    public static void main(String[] args) throws IOException {
        if (args.length != 2) {
            System.err.println("Please provide two directory names as parameters");
            System.exit(1);
        }
        final Path dir1 = getDirectory(args[0]);
        final Path dir2 = getDirectory(args[1]);

        final ImdiDiffer differ = new ImdiDifferImpl(PATHS_TO_IGNORE);
        differ.initialise();

        final ImdiDiffVisitor visitor = new ImdiDiffVisitor(dir1, dir2, differ);
        visitor.walk();
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
