package io.github.nikhil8bph.markcraft.config;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

@Component
public class VaultInitializer implements ApplicationRunner {

    private final VaultProperties properties;

    public VaultInitializer(VaultProperties properties) {
        this.properties = properties;
    }

    @Override
    public void run(ApplicationArguments args) throws IOException {
        Path root = properties.root().toAbsolutePath().normalize();
        if (Files.isSymbolicLink(root)) {
            throw new IOException("Vault root must not be a symbolic link");
        }
        Files.createDirectories(root);
        if (!Files.isDirectory(root, LinkOption.NOFOLLOW_LINKS)) {
            throw new IOException("Vault root must be a directory");
        }
    }
}
