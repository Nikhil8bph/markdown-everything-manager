package io.github.nikhil8bph.markcraft.service;

import io.github.nikhil8bph.markcraft.dtos.request.UploadFileRequest;
import io.github.nikhil8bph.markcraft.dtos.request.UploadRequest;
import io.github.nikhil8bph.markcraft.dtos.response.UploadResponse;
import io.github.nikhil8bph.markcraft.exception.VaultApiException;
import io.github.nikhil8bph.markcraft.facade.VaultUploadFacade;
import io.github.nikhil8bph.markcraft.validation.DocumentContentPolicy;
import io.github.nikhil8bph.markcraft.validation.VaultPathValidator;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

@Service
public class VaultUploadService {

    private static final Pattern REVISION = Pattern.compile("[0-9a-f]{64}");

    private final VaultUploadFacade facade;
    private final VaultPathValidator paths;
    private final OkfNormalizer normalizer;

    public VaultUploadService(VaultUploadFacade facade, VaultPathValidator paths, OkfNormalizer normalizer) {
        this.facade = facade;
        this.paths = paths;
        this.normalizer = normalizer;
    }

    public UploadResponse upload(UploadRequest request) {
        String folder = request.folder();
        if (folder.isEmpty()) {
            paths.root();
        } else {
            paths.folder(folder);
        }
        if (request.files().isEmpty()) {
            throw new VaultApiException(HttpStatus.BAD_REQUEST, "INVALID_REQUEST", "Upload batch must contain files");
        }

        Set<String> seen = new HashSet<>();
        List<UploadFileRequest> prepared = new ArrayList<>(request.files().size());
        long rawBytes = 0;
        long persistedBytes = 0;
        for (UploadFileRequest file : request.files()) {
            String name = file.name();
            if (!paths.isVisibleEntryName(name)) {
                throw VaultPathValidator.invalidPath();
            }
            if (!name.toLowerCase(Locale.ROOT).endsWith(".md")) {
                throw new VaultApiException(HttpStatus.UNPROCESSABLE_ENTITY, "UNSUPPORTED_FILE",
                        "Upload filenames must end in .md");
            }
            String path = folder.isEmpty() ? name : folder + "/" + name;
            paths.document(path);
            if (!seen.add(path)) {
                throw new VaultApiException(HttpStatus.CONFLICT, "DUPLICATE_BATCH_PATH",
                        "Upload batch contains a duplicate path");
            }
            String revision = file.expectedRevision();
            if (revision != null && !REVISION.matcher(revision).matches()) {
                throw new VaultApiException(HttpStatus.BAD_REQUEST, "INVALID_REQUEST",
                        "Expected revision must be an unquoted SHA-256 digest");
            }
            rawBytes += DocumentContentPolicy.utf8(file.content()).length;
            DocumentContentPolicy.requireWithinLimit(rawBytes);
            String normalized = normalizer.normalize(file.content(), name);
            persistedBytes += DocumentContentPolicy.utf8(normalized).length;
            DocumentContentPolicy.requireWithinLimit(persistedBytes);
            prepared.add(new UploadFileRequest(name, normalized, revision));
        }
        return UploadResponse.of(facade.upload(folder, List.copyOf(prepared)));
    }
}
