package nl.mpi.imdidiff;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ImmutableListMultimap;
import com.google.common.collect.Multimap;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/**
 *
 * @author Twan Goosen <twan.goosen@mpi.nl>
 */
public class ImdiDiffRunner {

    /**
     * @param args the command line arguments
     * @throws java.io.IOException in case of read failure or missing file
     */
    public static void main(String[] args) throws IOException {
        if (args.length < 2 || args.length > 3) {
            System.err.println("Usage: <jar> imdi-src imdi-target [exclude-list-file]");
            System.exit(1);
        }
        final Path dir1 = getDirectory(args[0]);
        final Path dir2 = getDirectory(args[1]);

        // map File path -> XPath (null value for key means ignore entire file)
        // use map type that can contain null!!
        final Multimap<Path, String> ignorePaths;
        if (args.length > 2) {
            ignorePaths = getIgnorePaths(args[2]);
        } else {
            ignorePaths = ImmutableListMultimap.of();
        }

        final ImdiDiffer differ = new NormalisingImdiDiffer(ignorePaths);
        differ.initialise();

        final ImdiDiffVisitor visitor = new ImdiDiffVisitor(dir1, dir2, differ, ignorePaths);
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

    private static Multimap<Path, String> getIgnorePaths(String file) throws IOException {
        final Path ignoreListFile = FileSystems.getDefault().getPath(file);
        if (!(Files.exists(ignoreListFile) && Files.isReadable(ignoreListFile))) {
            System.err.println(String.format("Could not read exclude list '%s'", ignoreListFile));
            System.exit(3);
        }

        final List<String> pathStrings = Files.readAllLines(ignoreListFile, Charset.defaultCharset());

        final Multimap<Path, String> paths = ArrayListMultimap.create();
        for (String pathString : pathStrings) {
            final String[] tokens = pathString.split("\\s", 2);
            if (tokens.length == 1) {
                paths.put(FileSystems.getDefault().getPath(tokens[0]), ImdiDiffer.SKIP_WILDCARD);
            } else {
                paths.put(FileSystems.getDefault().getPath(tokens[0]), tokens[1]);
            }
        }
        System.err.println(String.format("Found %d patterns to exclude from diff", paths.values().size()));
        return paths;
    }

}
