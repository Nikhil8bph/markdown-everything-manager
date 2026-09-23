package io.github.nikhil8bph.markcraft.validation;

import io.github.nikhil8bph.markcraft.exception.VaultApiException;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import org.springframework.http.HttpStatus;

public final class DocumentContentPolicy {

    public static final long MAX_BYTES = 25_000_000L;

    private DocumentContentPolicy() {
    }

    public static byte[] utf8(String content) {
        try {
            ByteBuffer encoded = StandardCharsets.UTF_8.newEncoder()
                    .onMalformedInput(CodingErrorAction.REPORT)
                    .onUnmappableCharacter(CodingErrorAction.REPORT)
                    .encode(CharBuffer.wrap(content));
            byte[] result = new byte[encoded.remaining()];
            encoded.get(result);
            return result;
        } catch (CharacterCodingException exception) {
            throw new VaultApiException(HttpStatus.BAD_REQUEST, "INVALID_REQUEST",
                    "Document content must be valid UTF-8");
        }
    }

    public static void requireWithinLimit(long bytes) {
        if (bytes > MAX_BYTES) {
            throw new VaultApiException(HttpStatus.PAYLOAD_TOO_LARGE, "PAYLOAD_TOO_LARGE",
                    "Content exceeds the 25,000,000-byte limit");
        }
    }
}
