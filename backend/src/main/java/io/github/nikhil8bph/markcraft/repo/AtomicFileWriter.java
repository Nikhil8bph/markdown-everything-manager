package io.github.nikhil8bph.markcraft.repo;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class AtomicFileWriter {

    private static final Logger LOGGER = LoggerFactory.getLogger(AtomicFileWriter.class);

    public void write(Path destination, byte[] content, boolean create) throws IOException {
        Path temporary = Files.createTempFile(destination.getParent(), ".markcraft-", ".tmp");
        try {
            try (FileChannel channel = FileChannel.open(temporary, StandardOpenOption.WRITE,
                    StandardOpenOption.TRUNCATE_EXISTING)) {
                ByteBuffer buffer = ByteBuffer.wrap(content);
                while (buffer.hasRemaining()) channel.write(buffer);
                channel.force(true);
            }
            commit(temporary, destination, create);
        } finally {
            try {
                Files.deleteIfExists(temporary);
            } catch (IOException exception) {
                LOGGER.warn("Could not remove a staged vault write", exception);
            }
        }
    }

    protected void commit(Path temporary, Path destination, boolean create) throws IOException {
        if (create) {
            Files.createLink(destination, temporary);
        } else {
            Files.move(temporary, destination, StandardCopyOption.ATOMIC_MOVE,
                    StandardCopyOption.REPLACE_EXISTING);
        }
    }
}
