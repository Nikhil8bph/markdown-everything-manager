package io.github.nikhil8bph.markcraft.controller;

import io.github.nikhil8bph.markcraft.dtos.response.SuccessResponse;
import io.github.nikhil8bph.markcraft.dtos.response.VaultDocument;
import io.github.nikhil8bph.markcraft.dtos.response.VaultNode;
import io.github.nikhil8bph.markcraft.service.VaultReadService;
import java.util.List;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/vault")
public class VaultReadController {

    private final VaultReadService service;

    public VaultReadController(VaultReadService service) {
        this.service = service;
    }

    @GetMapping("/tree")
    public SuccessResponse<List<VaultNode>> getVaultTree() {
        return SuccessResponse.of(service.tree());
    }

    @GetMapping("/documents")
    public ResponseEntity<SuccessResponse<VaultDocument>> getDocument(@RequestParam String path) {
        VaultDocument document = service.document(path);
        return ResponseEntity.ok()
                .header(HttpHeaders.ETAG, '"' + document.revision() + '"')
                .body(SuccessResponse.of(document));
    }

}
