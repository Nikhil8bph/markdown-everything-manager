package io.github.nikhil8bph.markcraft.facade;

import io.github.nikhil8bph.markcraft.dtos.response.VaultDocument;
import io.github.nikhil8bph.markcraft.dtos.response.VaultNode;
import io.github.nikhil8bph.markcraft.repo.FilesystemVaultRepository;
import io.github.nikhil8bph.markcraft.validation.VaultPathValidator;
import java.io.IOException;
import java.nio.file.NoSuchFileException;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class VaultReadFacade {

    private final FilesystemVaultRepository repository;

    public VaultReadFacade(FilesystemVaultRepository repository) {
        this.repository = repository;
    }

    public List<VaultNode> tree() {
        try {
            return repository.tree();
        } catch (IOException exception) {
            throw VaultPathValidator.storageFailure();
        }
    }

    public VaultDocument document(String path) {
        try {
            return repository.document(path);
        } catch (NoSuchFileException exception) {
            throw VaultPathValidator.notFound();
        } catch (IOException exception) {
            throw VaultPathValidator.storageFailure();
        }
    }
}
