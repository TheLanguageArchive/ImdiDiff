package nl.mpi.imdidiff;

import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Collection;
import javax.xml.transform.TransformerException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.SAXException;

/**
 * File visitor that walks two parallel IMDI directory hierarchies and compares
 * all encountered IMDI files
 *
 * @author Twan Goosen <twan.goosen@mpi.nl>
 */
class ImdiDiffVisitor extends SimpleFileVisitor<Path> {

    private final static Logger logger = LoggerFactory.getLogger(ImdiDiffVisitor.class);

    private final Path sourceDir;
    private final Path targetDir;
    private final ImdiDiffer imdiDiffer;
    private final Collection<Path> ignorepaths;

    private int diffCount;
    private int fileCount;
    private int diffFileCount;

    /**
     *
     * @param source source directory; assumed to be an existing directory on
     * the filesystem
     * @param target target directory; assumed to be an existing directory on
     * the filesystem that reflects the structure of dir1
     * @param imdiDiffer an initialised IMDI comparator
     */
    public ImdiDiffVisitor(Path source, Path target, ImdiDiffer imdiDiffer, Collection<Path> ignorepaths) {
        this.sourceDir = source;
        this.targetDir = target;
        this.imdiDiffer = imdiDiffer;
        this.ignorepaths = ignorepaths;
    }

    void walk() throws IOException {
        diffCount = fileCount = diffFileCount = 0;
        Files.walkFileTree(sourceDir, this);
        logger.info("Total number of differences found: {} in {} of {} files", diffCount, diffFileCount, fileCount);
    }

    @Override
    public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
        if (shouldSkip(dir)) {
            logger.debug("Skipping excluded directory {}", dir);
            return FileVisitResult.SKIP_SUBTREE;
        } else {
            return FileVisitResult.CONTINUE;
        }
    }

    @Override
    public FileVisitResult visitFile(Path source, BasicFileAttributes attrs) throws IOException {
        if (shouldSkip(source)) {
            logger.debug("Skipping excluded file {}", source);
            return FileVisitResult.CONTINUE;
        }
        if (!(attrs.isRegularFile() && isImdiFile(source))) {
            logger.debug("Skipping non-IMDI file {}", source);
            return FileVisitResult.CONTINUE;
        }

        fileCount++;

        // construct target path for comparison (same relative path in target dir)
        final Path relativePath = sourceDir.relativize(source);
        final Path target = targetDir.resolve(relativePath);

        if (Files.exists(target)) {
            logger.debug("Comparing {} to {}", source, target);
            try {
                final Collection<String> differences = imdiDiffer.compare(source, target);
                if (differences.size() > 0) {
                    logger.info("Found {} differences for {}", differences.size(), relativePath);
                    for (String diff : differences) {
                        logger.warn("{}: {}", relativePath, diff);
                    }
                    diffFileCount++;
                    diffCount += differences.size();
                }
            } catch (SAXException ex) {
                logger.error("Fatal error while parsing, file skipped. Source: {}", source, ex);
            } catch (TransformerException ex) {
                logger.error("Fatal error while transforming, file skipped. Source: {}", source, ex);
            }
        } else {
            logger.warn("No matching file found in target directory for {}\n\t(expected to find {})", source, target);
        }
        return FileVisitResult.CONTINUE;
    }

    private boolean shouldSkip(Path source) {
        return ignorepaths.contains(source) || ignorepaths.contains(source.toAbsolutePath());
    }

    private boolean isImdiFile(Path source) {
        return source.getFileName().toString().toLowerCase().endsWith(".imdi");
    }

}
