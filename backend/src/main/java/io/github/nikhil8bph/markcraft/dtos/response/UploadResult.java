package io.github.nikhil8bph.markcraft.dtos.response;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record UploadResult(String path, String status, String revision, ErrorInfo error) {

    public static UploadResult committed(String path, boolean replaced, String revision) {
        return new UploadResult(path, replaced ? "replaced" : "created", revision, null);
    }

    public static UploadResult failed(String path, ErrorInfo error) {
        return new UploadResult(path, "failed", null, error);
    }

    public static UploadResult notAttempted(String path) {
        return new UploadResult(path, "notAttempted", null, null);
    }
}
