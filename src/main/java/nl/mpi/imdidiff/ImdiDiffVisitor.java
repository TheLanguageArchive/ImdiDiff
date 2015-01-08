package nl.mpi.imdidiff;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.List;
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

    /**
     *
     * @param source source directory; assumed to be an existing directory on
     * the filesystem
     * @param target target directory; assumed to be an existing directory on
     * the filesystem that reflects the structure of dir1
     * @param imdiDiffer an initialised IMDI comparator
     */
    public ImdiDiffVisitor(Path source, Path target, ImdiDiffer imdiDiffer) {
        this.sourceDir = source;
        this.targetDir = target;
        this.imdiDiffer = imdiDiffer;
    }
    
    void walk() throws IOException {
        Files.walkFileTree(sourceDir, this);
    }
    
    @Override
    public FileVisitResult visitFile(Path source, BasicFileAttributes attrs) throws IOException {
        if (!isImdiFile(source)) {
            logger.debug("Skipping non-IMDI file {}", source);
            return FileVisitResult.CONTINUE;
        }

        // construct target path for comparison (same relative path in target dir)
        final Path relativePath = sourceDir.relativize(source);
        final Path target = targetDir.resolve(relativePath);
        
        if (Files.exists(target)) {
            logger.debug("Comparing {} to {}", source, target);
            try {
                final List<String> differences = imdiDiffer.compare(source, target);
                logger.info("Found {} differences for {}", differences.size(), relativePath);
                for (String diff : differences) {
                    logger.warn("{}: {}", relativePath, diff);
                }
                return FileVisitResult.CONTINUE;
            } catch (SAXException ex) {
                logger.error("Fatal error while parsing: {}", ex.getMessage());
                throw new RuntimeException(ex);
            }
        } else {
            logger.error("No matching file found in target directory for {}\n\t(expected to find {})", source, target);
            throw new FileNotFoundException(target.toString());
        }
    }
    
    private boolean isImdiFile(Path source) {
        return source.getFileName().toString().toLowerCase().endsWith(".imdi");
    }
    
}
