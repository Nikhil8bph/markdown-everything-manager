package io.github.nikhil8bph.markcraft.facade;

import io.github.nikhil8bph.markcraft.dtos.response.VaultDocument;
import io.github.nikhil8bph.markcraft.repo.FilesystemVaultRepository;
import io.github.nikhil8bph.markcraft.validation.VaultPathValidator;
import java.io.IOException;
import org.springframework.stereotype.Component;

@Component
public class VaultWriteFacade {

    private final FilesystemVaultRepository repository;

    public VaultWriteFacade(FilesystemVaultRepository repository) {
        this.repository = repository;
    }

    public VaultDocument write(String path, byte[] content, String expectedRevision, boolean create) {
        try {
            return repository.writeDocument(path, content, expectedRevision, create);
        } catch (IOException exception) {
            throw VaultPathValidator.storageFailure();
        }
    }
}
