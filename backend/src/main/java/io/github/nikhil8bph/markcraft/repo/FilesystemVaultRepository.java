package io.github.nikhil8bph.markcraft.repo;

import io.github.nikhil8bph.markcraft.dtos.response.VaultDocument;
import io.github.nikhil8bph.markcraft.dtos.response.VaultNode;
import io.github.nikhil8bph.markcraft.dtos.request.UploadFileRequest;
import io.github.nikhil8bph.markcraft.dtos.response.ErrorInfo;
import io.github.nikhil8bph.markcraft.dtos.response.UploadResult;
import io.github.nikhil8bph.markcraft.validation.DocumentContentPolicy;
import io.github.nikhil8bph.markcraft.validation.VaultPathValidator;
import java.io.IOException;
import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.StandardCharsets;
import java.nio.charset.CodingErrorAction;
import java.nio.channels.Channels;
import java.nio.file.Files;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.FileVisitResult;
import java.nio.file.LinkOption;
import java.nio.file.NoSuchFileException;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HexFormat;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Repository;
import org.springframework.http.HttpStatus;
import io.github.nikhil8bph.markcraft.exception.VaultApiException;

@Repository
public class FilesystemVaultRepository {

    private static final Logger LOGGER = LoggerFactory.getLogger(FilesystemVaultRepository.class);
    private static final int MAX_READ_BYTES = (int) DocumentContentPolicy.MAX_BYTES;
    private static final Comparator<Path> DISPLAY_ORDER = Comparator
            .comparingInt((Path path) -> Files.isDirectory(path, LinkOption.NOFOLLOW_LINKS) ? 0 : 1)
            .thenComparing(path -> path.getFileName().toString(), FilesystemVaultRepository::compareNatural);

    private final VaultPathValidator validator;
    private final AtomicFileWriter writer;

    public FilesystemVaultRepository(VaultPathValidator validator, AtomicFileWriter writer) {
        this.validator = validator;
        this.writer = writer;
    }

    public List<VaultNode> tree() throws IOException {
        return children(validator.root());
    }

    public VaultDocument document(String relativePath) throws IOException {
        Path path = validator.document(relativePath);
        if (!Files.isRegularFile(path, LinkOption.NOFOLLOW_LINKS)) {
            throw new NoSuchFileException(relativePath);
        }
        FileSnapshot snapshot = readFile(path);
        return new VaultDocument(relativePath, path.getFileName().toString(), decode(snapshot.bytes()),
                snapshot.attributes().lastModifiedTime().toInstant(), snapshot.bytes().length, sha256(snapshot.bytes()));
    }

    public VaultNode item(String relativePath) throws IOException {
        boolean file = relativePath != null && relativePath.toLowerCase(Locale.ROOT).endsWith(".md");
        Path path = file ? validator.document(relativePath) : validator.folder(relativePath);
        if ((file && !Files.isRegularFile(path, LinkOption.NOFOLLOW_LINKS))
                || (!file && !Files.isDirectory(path, LinkOption.NOFOLLOW_LINKS))) {
            throw new NoSuchFileException(relativePath);
        }
        return nodeAt(path, relativePath);
    }

    public synchronized VaultNode createFolder(String relativePath) throws IOException {
        Path path = validator.folder(relativePath);
        if (!Files.isDirectory(path.getParent(), LinkOption.NOFOLLOW_LINKS)) {
            throw VaultPathValidator.notFound();
        }
        if (Files.exists(path, LinkOption.NOFOLLOW_LINKS)) {
            throw destinationExists();
        }
        Files.createDirectory(path);
        return item(relativePath);
    }

    public synchronized VaultNode moveItem(String from, String to, String expectedRevision) throws IOException {
        VaultNode source = item(from);
        Path sourcePath = source.type().equals("file") ? validator.document(from) : validator.folder(from);
        Path destination = source.type().equals("file") ? validator.document(to) : validator.folder(to);
        if (source.type().equals("folder") && !destination.equals(sourcePath)
                && destination.startsWith(sourcePath)) {
            throw VaultPathValidator.invalidPath();
        }
        if (!Files.isDirectory(destination.getParent(), LinkOption.NOFOLLOW_LINKS)) {
            throw VaultPathValidator.notFound();
        }
        if (Files.exists(destination, LinkOption.NOFOLLOW_LINKS)) {
            throw destinationExists();
        }
        if (!source.revision().equals(expectedRevision)) {
            throw revisionConflict();
        }
        // Recheck both boundaries and source revision immediately before moving.
        validator.itemPath(from, source.type().equals("file"));
        validator.itemPath(to, source.type().equals("file"));
        if (!item(from).revision().equals(expectedRevision)) {
            throw revisionConflict();
        }
        Files.move(sourcePath, destination);
        return item(to);
    }

    public synchronized void deleteItem(String relativePath, String expectedRevision) throws IOException {
        VaultNode source = item(relativePath);
        if (!source.revision().equals(expectedRevision)) {
            throw revisionConflict();
        }
        Path path = source.type().equals("file") ? validator.document(relativePath) : validator.folder(relativePath);
        if (!item(relativePath).revision().equals(expectedRevision)) {
            throw revisionConflict();
        }
        if (source.type().equals("file")) {
            Files.delete(path);
            return;
        }
        Files.walkFileTree(path, new SimpleFileVisitor<>() {
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attributes) throws IOException {
                Files.delete(file);
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult postVisitDirectory(Path directory, IOException exception) throws IOException {
                if (exception != null) throw exception;
                Files.delete(directory);
                return FileVisitResult.CONTINUE;
            }
        });
    }

    private static VaultApiException destinationExists() {
        return new VaultApiException(HttpStatus.CONFLICT, "PATH_EXISTS", "Destination already exists");
    }

    private static VaultApiException revisionConflict() {
        return new VaultApiException(HttpStatus.PRECONDITION_FAILED, "REVISION_CONFLICT",
                "Item revision has changed");
    }

    public synchronized VaultDocument writeDocument(String relativePath, byte[] bytes,
                                                       String expectedRevision, boolean create) throws IOException {
        Path path = preflightDocument(relativePath, expectedRevision, create);
        // Recheck the path boundary directly before changing the filesystem.
        validator.document(relativePath);
        try {
            writer.write(path, bytes, create);
        } catch (FileAlreadyExistsException exception) {
            throw pathExists();
        }
        return document(relativePath);
    }

    public synchronized List<UploadResult> uploadDocuments(String folder, List<UploadFileRequest> files)
            throws IOException {
        Path targetFolder = folder.isEmpty() ? validator.root() : validator.folder(folder);
        if (!Files.isDirectory(targetFolder, LinkOption.NOFOLLOW_LINKS)) {
            throw VaultPathValidator.notFound();
        }
        for (UploadFileRequest file : files) {
            preflightDocument(uploadPath(folder, file.name()), file.expectedRevision(),
                    file.expectedRevision() == null);
        }

        List<UploadResult> results = new ArrayList<>(files.size());
        for (int index = 0; index < files.size(); index++) {
            UploadFileRequest file = files.get(index);
            String path = uploadPath(folder, file.name());
            boolean create = file.expectedRevision() == null;
            try {
                Path destination = preflightDocument(path, file.expectedRevision(), create);
                validator.document(path);
                byte[] content = DocumentContentPolicy.utf8(file.content());
                try {
                    writer.write(destination, content, create);
                } catch (FileAlreadyExistsException exception) {
                    throw pathExists();
                }
                results.add(UploadResult.committed(path, !create, sha256(content)));
            } catch (IOException | VaultApiException exception) {
                LOGGER.warn("Batch upload stopped after storage or revision failure", exception);
                ErrorInfo error = exception instanceof VaultApiException api
                        ? new ErrorInfo(api.code(), api.getMessage(), List.of())
                        : new ErrorInfo("STORAGE_FAILURE", "Vault storage is unavailable", List.of());
                results.add(UploadResult.failed(path, error));
                for (int remaining = index + 1; remaining < files.size(); remaining++) {
                    results.add(UploadResult.notAttempted(uploadPath(folder, files.get(remaining).name())));
                }
                break;
            }
        }
        return List.copyOf(results);
    }

    private Path preflightDocument(String relativePath, String expectedRevision, boolean create) throws IOException {
        Path path = validator.document(relativePath);
        Path parent = path.getParent();
        if (!Files.isDirectory(parent, LinkOption.NOFOLLOW_LINKS)) {
            throw VaultPathValidator.notFound();
        }
        boolean exists = Files.exists(path, LinkOption.NOFOLLOW_LINKS);
        if (create && exists) {
            throw pathExists();
        }
        if (!create && !Files.isRegularFile(path, LinkOption.NOFOLLOW_LINKS)) {
            throw VaultPathValidator.notFound();
        }
        if (!create && !document(relativePath).revision().equals(expectedRevision)) {
            throw new VaultApiException(HttpStatus.PRECONDITION_FAILED, "REVISION_CONFLICT",
                    "Document revision has changed");
        }
        return path;
    }

    private static String uploadPath(String folder, String name) {
        return folder.isEmpty() ? name : folder + "/" + name;
    }

    private static VaultApiException pathExists() {
        return new VaultApiException(HttpStatus.PRECONDITION_FAILED, "PATH_EXISTS",
                "Document already exists");
    }

    private List<VaultNode> children(Path directory) throws IOException {
        ensureInsideRoot(directory);
        List<Path> entries;
        try (var stream = Files.list(directory)) {
            entries = stream.filter(this::isVisibleItem).sorted(DISPLAY_ORDER).toList();
        }
        List<VaultNode> result = new ArrayList<>(entries.size());
        for (Path entry : entries) {
            ensureInsideRoot(entry);
            String relativePath = validator.root().relativize(entry).toString().replace(entry.getFileSystem().getSeparator(), "/");
            result.add(nodeAt(entry, relativePath));
        }
        return result;
    }

    private VaultNode nodeAt(Path path, String relativePath) throws IOException {
        ensureInsideRoot(path);
        BasicFileAttributes attributes = Files.readAttributes(path, BasicFileAttributes.class, LinkOption.NOFOLLOW_LINKS);
        if (attributes.isDirectory()) {
            List<VaultNode> descendants = children(path);
            return new VaultNode(path.getFileName().toString(), relativePath, "folder",
                    attributes.lastModifiedTime().toInstant(), folderRevision(descendants), null, descendants);
        }
        if (attributes.isRegularFile()) {
            FileSnapshot snapshot = readFile(path);
            return new VaultNode(path.getFileName().toString(), relativePath, "file",
                    snapshot.attributes().lastModifiedTime().toInstant(), sha256(snapshot.bytes()),
                    (long) snapshot.bytes().length, null);
        }
        throw new NoSuchFileException(relativePath);
    }

    private boolean isVisibleItem(Path path) {
        String name = path.getFileName().toString();
        if (!validator.isVisibleEntryName(name) || Files.isSymbolicLink(path)) {
            return false;
        }
        String relativePath = validator.root().relativize(path).toString();
        if (relativePath.length() > 1024) {
            return false;
        }
        if (Files.isDirectory(path, LinkOption.NOFOLLOW_LINKS)) {
            return !name.toLowerCase(Locale.ROOT).endsWith(".md");
        }
        return Files.isRegularFile(path, LinkOption.NOFOLLOW_LINKS)
                && name.toLowerCase(Locale.ROOT).endsWith(".md");
    }

    private FileSnapshot readFile(Path path) throws IOException {
        ensureInsideRoot(path);
        BasicFileAttributes attributes = Files.readAttributes(path, BasicFileAttributes.class, LinkOption.NOFOLLOW_LINKS);
        if (!attributes.isRegularFile()) {
            throw new NoSuchFileException(path.toString());
        }
        Set<OpenOption> options = Set.of(StandardOpenOption.READ, LinkOption.NOFOLLOW_LINKS);
        try (var channel = Files.newByteChannel(path, options);
             var stream = Channels.newInputStream(channel)) {
            byte[] bytes = stream.readNBytes(MAX_READ_BYTES + 1);
            if (bytes.length > MAX_READ_BYTES) {
                throw new IOException("Vault document exceeds supported read size");
            }
            return new FileSnapshot(bytes, attributes);
        }
    }

    private void ensureInsideRoot(Path path) throws IOException {
        Path root = validator.root();
        Path real = path.toRealPath();
        if (!real.startsWith(root) || Files.isSymbolicLink(path)) {
            throw VaultPathValidator.invalidPath();
        }
    }

    private static String decode(byte[] bytes) throws CharacterCodingException {
        return StandardCharsets.UTF_8.newDecoder()
                .onMalformedInput(CodingErrorAction.REPORT)
                .onUnmappableCharacter(CodingErrorAction.REPORT)
                .decode(ByteBuffer.wrap(bytes)).toString();
    }

    private static String folderRevision(List<VaultNode> descendants) {
        List<String> entries = new ArrayList<>();
        collectDescendants(entries, descendants);
        MessageDigest digest = newDigest();
        entries.stream().sorted().forEach(entry -> digest.update(entry.getBytes(StandardCharsets.UTF_8)));
        return HexFormat.of().formatHex(digest.digest());
    }

    private static void collectDescendants(List<String> entries, List<VaultNode> descendants) {
        for (VaultNode descendant : descendants) {
            entries.add(descendant.path() + '\0'
                    + (descendant.type().equals("file") ? descendant.revision() : "") + '\n');
            if (descendant.children() != null) {
                collectDescendants(entries, descendant.children());
            }
        }
    }

    private static String sha256(byte[] bytes) {
        return HexFormat.of().formatHex(newDigest().digest(bytes));
    }

    private static MessageDigest newDigest() {
        try {
            return MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is unavailable", exception);
        }
    }

    private static int compareNatural(String left, String right) {
        int first = 0;
        int second = 0;
        while (first < left.length() && second < right.length()) {
            char a = left.charAt(first);
            char b = right.charAt(second);
            if (Character.isDigit(a) && Character.isDigit(b)) {
                int firstEnd = first;
                int secondEnd = second;
                while (firstEnd < left.length() && Character.isDigit(left.charAt(firstEnd))) firstEnd++;
                while (secondEnd < right.length() && Character.isDigit(right.charAt(secondEnd))) secondEnd++;
                int result = new BigInteger(left.substring(first, firstEnd))
                        .compareTo(new BigInteger(right.substring(second, secondEnd)));
                if (result != 0) return result;
                first = firstEnd;
                second = secondEnd;
            } else {
                int result = Character.compare(Character.toLowerCase(a), Character.toLowerCase(b));
                if (result != 0) return result;
                first++;
                second++;
            }
        }
        int result = Integer.compare(left.length() - first, right.length() - second);
        return result != 0 ? result : left.compareTo(right);
    }

    private record FileSnapshot(byte[] bytes, BasicFileAttributes attributes) {
    }
}
