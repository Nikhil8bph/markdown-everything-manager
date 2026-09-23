package io.github.nikhil8bph.markcraft.controller;

import io.github.nikhil8bph.markcraft.dtos.request.PutDocumentRequest;
import io.github.nikhil8bph.markcraft.dtos.response.SuccessResponse;
import io.github.nikhil8bph.markcraft.dtos.response.VaultDocument;
import io.github.nikhil8bph.markcraft.service.VaultWriteService;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/vault")
public class VaultWriteController {

    private final VaultWriteService service;

    public VaultWriteController(VaultWriteService service) {
        this.service = service;
    }

    @PutMapping("/documents")
    public ResponseEntity<SuccessResponse<VaultDocument>> putDocument(
            @RequestParam String path,
            @RequestHeader(value = HttpHeaders.IF_MATCH, required = false) String ifMatch,
            @RequestHeader(value = HttpHeaders.IF_NONE_MATCH, required = false) String ifNoneMatch,
            @Valid @RequestBody PutDocumentRequest request) {
        VaultDocument document = service.save(path, request.content(), ifMatch, ifNoneMatch);
        return ResponseEntity.status(ifNoneMatch != null ? HttpStatus.CREATED : HttpStatus.OK)
                .header(HttpHeaders.ETAG, '"' + document.revision() + '"')
                .body(SuccessResponse.of(document));
    }
}
