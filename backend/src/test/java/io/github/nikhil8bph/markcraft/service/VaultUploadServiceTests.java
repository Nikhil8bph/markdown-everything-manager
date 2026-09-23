package io.github.nikhil8bph.markcraft.service;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import io.github.nikhil8bph.markcraft.config.VaultProperties;
import io.github.nikhil8bph.markcraft.dtos.request.UploadFileRequest;
import io.github.nikhil8bph.markcraft.dtos.request.UploadRequest;
import io.github.nikhil8bph.markcraft.exception.VaultApiException;
import io.github.nikhil8bph.markcraft.dtos.response.UploadResult;
import io.github.nikhil8bph.markcraft.facade.VaultUploadFacade;
import io.github.nikhil8bph.markcraft.validation.VaultPathValidator;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class VaultUploadServiceTests {

    @TempDir
    Path root;

    @Test
    void rejectsBatchWhoseNormalizedUtf8ContentExceedsLimitBeforeStorage() {
        VaultUploadFacade facade = mock(VaultUploadFacade.class);
        VaultUploadService service = new VaultUploadService(facade,
                new VaultPathValidator(new VaultProperties(root)), new OkfNormalizer());
        UploadRequest request = new UploadRequest("", List.of(
                new UploadFileRequest("large.md", "x".repeat(25_000_000), null)));

        assertThatThrownBy(() -> service.upload(request))
                .isInstanceOf(VaultApiException.class)
                .extracting("code").isEqualTo("PAYLOAD_TOO_LARGE");
        verifyNoInteractions(facade);
    }

    @Test
    void acceptsExactlyTwentyFiveMillionPersistedUtf8Bytes() {
        VaultUploadFacade facade = mock(VaultUploadFacade.class);
        VaultUploadService service = new VaultUploadService(facade,
                new VaultPathValidator(new VaultProperties(root)), new OkfNormalizer());
        String prefix = "---\ntype: concept\n---\n";
        String content = prefix + "x".repeat(25_000_000 - prefix.length());
        when(facade.upload(eq(""), anyList())).thenReturn(List.of(
                UploadResult.committed("large.md", false, "0".repeat(64))));

        var response = service.upload(new UploadRequest("", List.of(
                new UploadFileRequest("large.md", content, null))));
        assertThat(response.success()).isTrue();
        assertThat(response.data()).hasSize(1);
    }
}
