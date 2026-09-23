package io.github.nikhil8bph.markcraft.facade;

import io.github.nikhil8bph.markcraft.dtos.response.VaultNode;
import io.github.nikhil8bph.markcraft.exception.VaultApiException;
import io.github.nikhil8bph.markcraft.repo.FilesystemVaultRepository;
import io.github.nikhil8bph.markcraft.validation.VaultPathValidator;
import java.io.IOException;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.NoSuchFileException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

@Component
public class VaultItemFacade {

    private final FilesystemVaultRepository repository;

    public VaultItemFacade(FilesystemVaultRepository repository) {
        this.repository = repository;
    }

    public VaultNode createFolder(String path) {
        try {
            return repository.createFolder(path);
        } catch (FileAlreadyExistsException exception) {
            throw destinationExists();
        } catch (NoSuchFileException exception) {
            throw VaultPathValidator.notFound();
        } catch (IOException exception) {
            throw VaultPathValidator.storageFailure();
        }
    }

    public VaultNode move(String from, String to, String revision) {
        try {
            return repository.moveItem(from, to, revision);
        } catch (FileAlreadyExistsException exception) {
            throw destinationExists();
        } catch (NoSuchFileException exception) {
            throw VaultPathValidator.notFound();
        } catch (IOException exception) {
            throw VaultPathValidator.storageFailure();
        }
    }

    public void delete(String path, String revision) {
        try {
            repository.deleteItem(path, revision);
        } catch (NoSuchFileException exception) {
            throw VaultPathValidator.notFound();
        } catch (IOException exception) {
            throw VaultPathValidator.storageFailure();
        }
    }

    private static VaultApiException destinationExists() {
        return new VaultApiException(HttpStatus.CONFLICT, "PATH_EXISTS", "Destination already exists");
    }
}
