package com.example.dataops.service;

import com.example.dataops.dto.BlockchainDtos;
import com.example.dataops.mapper.DataopsMapper;
import com.example.dataops.model.BlockchainBlock;
import com.example.dataops.repository.BlockchainRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.HexFormat;
import java.util.List;

@Service
public class BlockchainService {
    private static final String GENESIS_HASH = "0";

    private final BlockchainRepository repository;
    private final DataopsMapper mapper;

    public BlockchainService(BlockchainRepository repository, DataopsMapper mapper) {
        this.repository = repository;
        this.mapper = mapper;
    }

    @Transactional
    public BlockchainDtos.BlockchainBlockResponse addBlock(String action, String entityType, Long entityId, String userId, String data) {
        BlockchainBlock previous = repository.findTopByOrderByIdDesc().orElse(null);
        String previousHash = previous == null ? GENESIS_HASH : previous.getCurrentHash();

        BlockchainBlock block = new BlockchainBlock();
        block.setTimestamp(Instant.now());
        block.setAction(action);
        block.setEntityType(entityType);
        block.setEntityId(entityId);
        block.setUserId(userId == null || userId.isBlank() ? "system" : userId);
        block.setDataHash(sha256(data == null ? "" : data));
        block.setPreviousHash(previousHash);
        block.setCurrentHash(calculateHash(block));
        return mapper.toBlockchainBlockResponse(repository.save(block));
    }

    @Transactional
    public BlockchainDtos.BlockchainBlockResponse append(String action, String actor, String payload) {
        return addBlock(action, "AUDIT", null, actor, payload);
    }

    @Transactional(readOnly = true)
    public List<BlockchainDtos.BlockchainBlockResponse> findAll() {
        return repository.findAll().stream().map(mapper::toBlockchainBlockResponse).toList();
    }

    @Transactional(readOnly = true)
    public BlockchainDtos.ChainValidationResponse verifyChain() {
        List<BlockchainBlock> blocks = repository.findAll().stream()
            .sorted((left, right) -> left.getId().compareTo(right.getId()))
            .toList();

        String previousHash = GENESIS_HASH;
        for (BlockchainBlock block : blocks) {
            if (!previousHash.equals(block.getPreviousHash())) {
                return new BlockchainDtos.ChainValidationResponse(false, "Invalid previous hash at block " + block.getId());
            }
            if (!calculateHash(block).equals(block.getCurrentHash())) {
                return new BlockchainDtos.ChainValidationResponse(false, "Invalid hash at block " + block.getId());
            }
            previousHash = block.getCurrentHash();
        }
        return new BlockchainDtos.ChainValidationResponse(true, "Private audit chain is valid");
    }

    @Transactional(readOnly = true)
    public BlockchainDtos.ChainValidationResponse validateChain() {
        return verifyChain();
    }

    public String calculateHash(BlockchainBlock block) {
        String data = block.getTimestamp()
            + "|" + block.getAction()
            + "|" + block.getEntityType()
            + "|" + block.getEntityId()
            + "|" + block.getUserId()
            + "|" + block.getDataHash()
            + "|" + block.getPreviousHash();
        return sha256(data);
    }

    private String sha256(String data) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(data.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 unavailable", exception);
        }
    }
}
