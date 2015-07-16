package nl.mpi.imdidiff;

import com.google.common.base.Converter;
import com.google.common.collect.Collections2;
import com.google.common.collect.ImmutableSet;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.util.Collection;
import java.util.Collections;
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

        final Collection<Path> ignorePaths;
        if (args.length > 2) {
            ignorePaths = getIgnorePaths(args[2]);
        } else {
            ignorePaths = Collections.emptySet();
        }

        final ImdiDiffer differ = new NormalisingImdiDiffer();
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

    private static Collection<Path> getIgnorePaths(String file) throws IOException {
        final Path ignoreListFile = FileSystems.getDefault().getPath(file);
        if (!(Files.exists(ignoreListFile) && Files.isReadable(ignoreListFile))) {
            System.err.println(String.format("Could not read exclude list '%s'", ignoreListFile));
            System.exit(3);
        }

        final List<String> pathStrings = Files.readAllLines(ignoreListFile, Charset.defaultCharset());
        final Collection<Path> paths = Collections2.transform(pathStrings, new Converter<String, Path>() {

            @Override
            protected Path doForward(String a) {
                try {
                    return FileSystems.getDefault().getPath(a);
                } catch (InvalidPathException ex) {
                    System.err.println(a + ": " + ex.getMessage());
                    return null;
                }
            }

            @Override
            protected String doBackward(Path b) {
                return b.toString();
            }
        });
        System.err.print(String.format("Found %d paths to exclude from diff", paths.size()));
        return ImmutableSet.copyOf(paths);
    }

}
