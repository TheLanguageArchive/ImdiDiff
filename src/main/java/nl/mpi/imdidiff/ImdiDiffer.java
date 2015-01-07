package nl.mpi.imdidiff;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author Twan Goosen <twan.goosen@mpi.nl>
 */
class ImdiDiffer {

    private final static Logger logger = LoggerFactory.getLogger(ImdiDiffer.class);

    private final Path dir1;
    private final Path dir2;

    /**
     *
     * @param dir1 source directory; assumed to be an existing directory on the
     * filesystem
     * @param dir2 target directory; assumed to be an existing directory on the
     * filesystem that reflects the structure of dir1
     */
    public ImdiDiffer(Path dir1, Path dir2) {
        this.dir1 = dir1;
        this.dir2 = dir2;
    }

    void diff() throws IOException {
        Files.walkFileTree(dir1, new ImdiDiffVisitor());
    }

    private class ImdiDiffVisitor extends SimpleFileVisitor<Path> {

        @Override
        public FileVisitResult visitFile(Path source, BasicFileAttributes attrs) throws IOException {
            final Path relativePath = dir1.relativize(source);
            final Path target = dir2.resolve(relativePath);
            if (Files.exists(target)) {
                logger.info("Comparing %s to %s", source, target);
                return FileVisitResult.CONTINUE;
            } else {
                throw new FileNotFoundException(target.toString());
            }
        }

    }

}
