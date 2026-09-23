package io.github.nikhil8bph.markcraft.service;

import io.github.nikhil8bph.markcraft.dtos.response.VaultDocument;
import io.github.nikhil8bph.markcraft.exception.VaultApiException;
import io.github.nikhil8bph.markcraft.facade.VaultWriteFacade;
import io.github.nikhil8bph.markcraft.validation.VaultPathValidator;
import io.github.nikhil8bph.markcraft.validation.RevisionPrecondition;
import io.github.nikhil8bph.markcraft.validation.DocumentContentPolicy;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

@Service
public class VaultWriteService {


    private final VaultWriteFacade facade;
    private final OkfNormalizer normalizer;
    private final VaultPathValidator pathValidator;

    public VaultWriteService(VaultWriteFacade facade, OkfNormalizer normalizer, VaultPathValidator pathValidator) {
        this.facade = facade;
        this.normalizer = normalizer;
        this.pathValidator = pathValidator;
    }

    public VaultDocument save(String path, String content, String ifMatch, String ifNoneMatch) {
        boolean create = ifNoneMatch != null;
        if (ifMatch == null && ifNoneMatch == null) {
            throw new VaultApiException(HttpStatus.BAD_REQUEST, "PRECONDITION_REQUIRED",
                    "A conditional header is required");
        }
        if ((ifMatch != null && ifNoneMatch != null)
                || (create && !ifNoneMatch.equals("*"))) {
            throw new VaultApiException(HttpStatus.BAD_REQUEST, "INVALID_REQUEST",
                    "Invalid conditional headers");
        }
        String expectedRevision = create ? null : RevisionPrecondition.require(ifMatch);
        pathValidator.document(path);
        byte[] original = DocumentContentPolicy.utf8(content);
        DocumentContentPolicy.requireWithinLimit(original.length);
        String filename = path.substring(path.lastIndexOf('/') + 1);
        String normalized = normalizer.normalize(content, filename);
        byte[] persisted = DocumentContentPolicy.utf8(normalized);
        DocumentContentPolicy.requireWithinLimit(persisted.length);
        return facade.write(path, persisted, expectedRevision, create);
    }
}
