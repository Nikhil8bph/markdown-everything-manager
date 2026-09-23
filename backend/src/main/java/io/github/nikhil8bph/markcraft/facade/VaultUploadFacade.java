package io.github.nikhil8bph.markcraft.facade;

import io.github.nikhil8bph.markcraft.dtos.request.UploadFileRequest;
import io.github.nikhil8bph.markcraft.dtos.response.UploadResult;
import io.github.nikhil8bph.markcraft.repo.FilesystemVaultRepository;
import io.github.nikhil8bph.markcraft.validation.VaultPathValidator;
import java.io.IOException;
import java.nio.file.NoSuchFileException;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class VaultUploadFacade {

    private final FilesystemVaultRepository repository;

    public VaultUploadFacade(FilesystemVaultRepository repository) {
        this.repository = repository;
    }

    public List<UploadResult> upload(String folder, List<UploadFileRequest> files) {
        try {
            return repository.uploadDocuments(folder, files);
        } catch (NoSuchFileException exception) {
            throw VaultPathValidator.notFound();
        } catch (IOException exception) {
            throw VaultPathValidator.storageFailure();
        }
    }
}
