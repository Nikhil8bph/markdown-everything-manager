package io.github.nikhil8bph.markcraft.repo;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.github.nikhil8bph.markcraft.config.VaultProperties;
import io.github.nikhil8bph.markcraft.validation.VaultPathValidator;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class AtomicFileWriterTests {

    @TempDir
    Path root;

    @Test
    void failedCommitRetainsPreviousContentAndCleansStagedFile() throws Exception {
        Files.writeString(root.resolve("idea.md"), "previous");
        AtomicFileWriter failingWriter = new AtomicFileWriter() {
            @Override
            protected void commit(Path temporary, Path destination, boolean create) throws IOException {
                assertThat(Files.readString(temporary)).isEqualTo("replacement");
                throw new IOException("induced commit failure");
            }
        };
        FilesystemVaultRepository repository = new FilesystemVaultRepository(
                new VaultPathValidator(new VaultProperties(root)), failingWriter);
        String revision = repository.document("idea.md").revision();

        assertThatThrownBy(() -> repository.writeDocument("idea.md",
                "replacement".getBytes(StandardCharsets.UTF_8), revision, false))
                .isInstanceOf(IOException.class).hasMessageContaining("induced commit failure");
        assertThat(Files.readString(root.resolve("idea.md"))).isEqualTo("previous");
        try (var entries = Files.list(root)) {
            assertThat(entries.map(path -> path.getFileName().toString())).containsExactly("idea.md");
        }
    }
}
