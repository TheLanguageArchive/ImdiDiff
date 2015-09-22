package nl.mpi.imdidiff;

import com.google.common.collect.Multimap;
import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Collection;
import java.util.regex.Pattern;
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

    public static final Pattern DIFF_PATH_PATTERN = Pattern.compile(".* at ([^\\s]+)$");

    private final Path sourceDir;
    private final Path targetDir;
    private final ImdiDiffer imdiDiffer;
    private final Multimap<Path, String> ignorepaths;

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
    public ImdiDiffVisitor(Path source, Path target, ImdiDiffer imdiDiffer, Multimap<Path, String> ignorepaths) {
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

        // perform comparison (logging all differences)
        if (Files.exists(target)) {
            logger.debug("Comparing {} to {}", source, target);
            compare(source, target, relativePath);
        } else {
            logger.warn("No matching file found in target directory for {}\n\t(expected to find {})", source, target);
        }
        return FileVisitResult.CONTINUE;
    }

    /**
     * Compares source and target files using the {@link ImdiDiffer}, and sends
     * all differences found to the logger at warn level
     *
     * @param source source to compare
     * @param target target to compare to
     * @param relativePath relative path that applies to both source and target
     * @throws IOException if differ fails to read either file
     */
    private void compare(Path source, final Path target, final Path relativePath) throws IOException {
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
            logger.error("Fatal error while parsing. Skipped file: {}", source.getFileName(), ex);
        } catch (TransformerException ex) {
            logger.error("Fatal error while transforming. Skipped file: {}", source.getFileName(), ex);
        }
    }

    private boolean shouldSkip(Path source) {
        return shouldSkip(source, ImdiDiffer.SKIP_WILDCARD);
    }

    private boolean shouldSkip(Path source, String nodePath) {
        // skip node path if an entry exists for the file
        return ImdiDiffRunner.matchesIgnorePath(ignorepaths, source, nodePath, null);
    }

    private boolean isImdiFile(Path source) {
        return source.getFileName().toString().toLowerCase().endsWith(".imdi");
    }
    
}
