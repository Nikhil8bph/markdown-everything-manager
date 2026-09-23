package io.github.nikhil8bph.markcraft.dtos.response;

import java.util.List;

public record ErrorInfo(String code, String message, List<ErrorDetail> details) {
}
