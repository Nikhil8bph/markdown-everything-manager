package io.github.nikhil8bph.markcraft.service;

import io.github.nikhil8bph.markcraft.dtos.response.VaultDocument;
import io.github.nikhil8bph.markcraft.dtos.response.VaultNode;
import io.github.nikhil8bph.markcraft.facade.VaultReadFacade;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class VaultReadService {

    private final VaultReadFacade facade;

    public VaultReadService(VaultReadFacade facade) {
        this.facade = facade;
    }

    public List<VaultNode> tree() {
        return facade.tree();
    }

    public VaultDocument document(String path) {
        return facade.document(path);
    }
}
