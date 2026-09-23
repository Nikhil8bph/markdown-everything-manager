package io.github.nikhil8bph.markcraft.controller;

import io.github.nikhil8bph.markcraft.dtos.request.UploadRequest;
import io.github.nikhil8bph.markcraft.dtos.response.UploadResponse;
import io.github.nikhil8bph.markcraft.service.VaultUploadService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/vault")
public class VaultUploadController {

    private final VaultUploadService service;

    public VaultUploadController(VaultUploadService service) {
        this.service = service;
    }

    @PostMapping("/uploads")
    public ResponseEntity<UploadResponse> uploadDocuments(@Valid @RequestBody UploadRequest request) {
        UploadResponse response = service.upload(request);
        return ResponseEntity.status(response.success() ? HttpStatus.OK : HttpStatus.MULTI_STATUS).body(response);
    }
}
