package io.github.nikhil8bph.markcraft.dtos.response;

import java.time.Instant;
import java.util.List;

public record UploadResponse(boolean success, List<UploadResult> data, Instant timestamp) {

    public static UploadResponse of(List<UploadResult> results) {
        boolean complete = results.stream().allMatch(result -> result.status().equals("created")
                || result.status().equals("replaced"));
        return new UploadResponse(complete, results, Instant.now());
    }
}
