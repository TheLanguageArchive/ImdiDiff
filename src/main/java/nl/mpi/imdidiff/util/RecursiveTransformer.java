package nl.mpi.imdidiff.util;

import java.io.IOException;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import javax.xml.transform.Result;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;

/**
 *
 * @author Twan Goosen <twan.goosen@mpi.nl>
 */
public class RecursiveTransformer {

    private final Path stylesheet;
    private final Path inputPath;
    private final Path outputPath;
    private final String inputExtension;
    private final String outputExtension;

    public RecursiveTransformer(Path stylesheet, Path inputPath, Path outputPath, String inputExtension, String outputExtension) throws IOException {
        this.stylesheet = stylesheet;
        this.inputPath = inputPath;
        this.outputPath = outputPath;
        this.inputExtension = inputExtension;
        this.outputExtension = outputExtension;

        if (Files.exists(outputPath)) {
            if (!Files.isDirectory(outputPath)) {
                throw new RuntimeException("Output path needs to be a directory");
            }
        } else {
            System.err.println("Creating output directory " + outputPath);
            Files.createDirectories(outputPath);
        }
    }

    public void transform() throws IOException, TransformerConfigurationException {
        final SimpleFileVisitor<Path> visitor = new TransformationVisitor();
        Files.walkFileTree(inputPath, visitor);
    }

    private class TransformationVisitor extends SimpleFileVisitor<Path> {

        private final Transformer transformer;

        public TransformationVisitor() throws TransformerConfigurationException {
            final TransformerFactory tff = TransformerFactory.newInstance();
            Source stylesheetSource = new StreamSource(stylesheet.toFile());
            transformer = tff.newTransformer(stylesheetSource);
        }

        @Override
        public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
            System.err.println("Transforming files in " + dir.toString());
            return FileVisitResult.CONTINUE;
        }

        @Override
        public FileVisitResult visitFile(Path inFile, BasicFileAttributes attrs) throws IOException {
            final Path targetPath = getTargetPath(inFile);
            if (Files.exists(targetPath)) {
                System.err.println("FATAL: Target file already exists: " + targetPath.toString());
                throw new FileAlreadyExistsException(targetPath.toString());
            }

            final Source source = new StreamSource(inFile.toFile());
            final Result result = new StreamResult(targetPath.toFile());
            try {
                transformer.transform(source, result);
            } catch (TransformerException ex) {
                System.err.println("ERROR: Exception while transforming " + inFile.toString() + ":\n");
                ex.printStackTrace(System.err);
            }
            return FileVisitResult.CONTINUE;
        }

        private Path getTargetPath(Path inFile) {
            final Path relativePath = inputPath.relativize(inFile);
            final String rp = relativePath.toString();
            final int lastIndexOf = rp.lastIndexOf(inputExtension);
            if (lastIndexOf >= 0) {
                String targetRP = rp.substring(0, lastIndexOf) + outputExtension;
                Path targetPath = outputPath.resolve(targetRP);
                return targetPath;
            } else {
                // input extension not found
                return outputPath.resolve(relativePath);
            }
        }
    }

    public static void main(String[] args) throws IOException, TransformerConfigurationException {
        if (args.length != 5) {
            System.err.println("Usage: java " + RecursiveTransformer.class.getName() + "stylesheet input-dir output-dir input-extension output-extension");
            System.exit(1);
        }

        final FileSystem fs = FileSystems.getDefault();
        final RecursiveTransformer recursiveTransformer
                = new RecursiveTransformer(
                        fs.getPath(args[0]),
                        fs.getPath(args[1]),
                        fs.getPath(args[2]),
                        args[3],
                        args[4]);

        recursiveTransformer.transform();
    }
}
