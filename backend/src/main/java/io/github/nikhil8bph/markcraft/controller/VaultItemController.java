package io.github.nikhil8bph.markcraft.controller;

import io.github.nikhil8bph.markcraft.dtos.request.CreateFolderRequest;
import io.github.nikhil8bph.markcraft.dtos.request.MoveRequest;
import io.github.nikhil8bph.markcraft.dtos.response.SuccessResponse;
import io.github.nikhil8bph.markcraft.dtos.response.VaultNode;
import io.github.nikhil8bph.markcraft.service.VaultItemService;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/vault")
public class VaultItemController {

    private final VaultItemService service;

    public VaultItemController(VaultItemService service) {
        this.service = service;
    }

    @PostMapping("/folders")
    public ResponseEntity<SuccessResponse<VaultNode>> createFolder(@Valid @RequestBody CreateFolderRequest request) {
        return ResponseEntity.status(201).body(SuccessResponse.of(service.createFolder(request.path())));
    }

    @PostMapping("/moves")
    public SuccessResponse<VaultNode> moveItem(@Valid @RequestBody MoveRequest request,
                                               @RequestHeader(value = HttpHeaders.IF_MATCH, required = false) String ifMatch) {
        return SuccessResponse.of(service.move(request.from(), request.to(), ifMatch));
    }

    @DeleteMapping("/items")
    public ResponseEntity<Void> deleteItem(@RequestParam String path,
                                           @RequestHeader(value = HttpHeaders.IF_MATCH, required = false) String ifMatch) {
        service.delete(path, ifMatch);
        return ResponseEntity.noContent().build();
    }
}
