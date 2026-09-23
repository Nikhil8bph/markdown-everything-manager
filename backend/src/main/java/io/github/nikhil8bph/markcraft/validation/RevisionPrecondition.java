package io.github.nikhil8bph.markcraft.validation;

import io.github.nikhil8bph.markcraft.exception.VaultApiException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.http.HttpStatus;

public final class RevisionPrecondition {

    private static final Pattern QUOTED_REVISION = Pattern.compile("\"([0-9a-f]{64})\"");

    private RevisionPrecondition() {
    }

    public static String require(String ifMatch) {
        if (ifMatch == null) {
            throw new VaultApiException(HttpStatus.BAD_REQUEST, "PRECONDITION_REQUIRED",
                    "A current If-Match revision is required");
        }
        Matcher matcher = QUOTED_REVISION.matcher(ifMatch);
        if (!matcher.matches()) {
            throw new VaultApiException(HttpStatus.BAD_REQUEST, "INVALID_REQUEST",
                    "If-Match must contain one quoted revision");
        }
        return matcher.group(1);
    }
}
