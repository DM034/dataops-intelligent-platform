package com.example.dataops.controller;

import com.example.dataops.dto.BlockchainDtos;
import com.example.dataops.service.BlockchainService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/blockchain")
public class BlockchainController {
    private final BlockchainService service;

    public BlockchainController(BlockchainService service) {
        this.service = service;
    }

    @GetMapping
    public List<BlockchainDtos.BlockchainBlockResponse> blocks() {
        return service.findAll();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public BlockchainDtos.BlockchainBlockResponse append(@Valid @RequestBody BlockchainDtos.AuditEventRequest request) {
        return service.addBlock(request.action(), request.entityType(), request.entityId(), request.userId(), request.data());
    }

    @GetMapping("/verify")
    public BlockchainDtos.ChainValidationResponse validate() {
        return service.verifyChain();
    }
}
