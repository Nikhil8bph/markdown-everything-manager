package io.github.nikhil8bph.markcraft.service;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;

import io.github.nikhil8bph.markcraft.config.VaultProperties;
import io.github.nikhil8bph.markcraft.exception.VaultApiException;
import io.github.nikhil8bph.markcraft.facade.VaultWriteFacade;
import io.github.nikhil8bph.markcraft.validation.VaultPathValidator;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class VaultWriteServiceTests {

    @TempDir
    Path root;

    @Test
    void rejectsOversizedInputAndNormalizedOutputBeforeStorage() {
        VaultWriteFacade facade = mock(VaultWriteFacade.class);
        VaultWriteService service = new VaultWriteService(facade, new OkfNormalizer(),
                new VaultPathValidator(new VaultProperties(root)));

        assertThatThrownBy(() -> service.save("large.md", "x".repeat(25_000_001), null, "*"))
                .isInstanceOf(VaultApiException.class).extracting("code").isEqualTo("PAYLOAD_TOO_LARGE");
        assertThatThrownBy(() -> service.save("large.md", "x".repeat(25_000_000), null, "*"))
                .isInstanceOf(VaultApiException.class).extracting("code").isEqualTo("PAYLOAD_TOO_LARGE");
        verifyNoInteractions(facade);
    }
}
