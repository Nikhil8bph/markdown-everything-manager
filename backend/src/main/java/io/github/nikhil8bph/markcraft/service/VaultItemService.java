package io.github.nikhil8bph.markcraft.service;

import io.github.nikhil8bph.markcraft.dtos.response.VaultNode;
import io.github.nikhil8bph.markcraft.facade.VaultItemFacade;
import io.github.nikhil8bph.markcraft.validation.RevisionPrecondition;
import org.springframework.stereotype.Service;

@Service
public class VaultItemService {

    private final VaultItemFacade facade;

    public VaultItemService(VaultItemFacade facade) {
        this.facade = facade;
    }

    public VaultNode createFolder(String path) {
        return facade.createFolder(path);
    }

    public VaultNode move(String from, String to, String ifMatch) {
        return facade.move(from, to, RevisionPrecondition.require(ifMatch));
    }

    public void delete(String path, String ifMatch) {
        facade.delete(path, RevisionPrecondition.require(ifMatch));
    }
}
