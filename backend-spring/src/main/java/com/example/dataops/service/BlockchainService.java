package com.example.dataops.service;

import com.example.dataops.dto.BlockchainDtos;
import com.example.dataops.mapper.DataopsMapper;
import com.example.dataops.model.BlockchainBlock;
import com.example.dataops.repository.BlockchainBlockRepository;
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

    private final BlockchainBlockRepository repository;
    private final DataopsMapper mapper;

    public BlockchainService(BlockchainBlockRepository repository, DataopsMapper mapper) {
        this.repository = repository;
        this.mapper = mapper;
    }

    @Transactional
    public BlockchainDtos.BlockchainBlockResponse append(String action, String actor, String payload) {
        BlockchainBlock previous = repository.findTopByOrderByBlockIndexDesc().orElse(null);
        long nextIndex = previous == null ? 0 : previous.getBlockIndex() + 1;
        String previousHash = previous == null ? GENESIS_HASH : previous.getHash();

        BlockchainBlock block = new BlockchainBlock();
        block.setBlockIndex(nextIndex);
        block.setTimestamp(Instant.now());
        block.setAction(action);
        block.setActor(actor);
        block.setPayload(payload);
        block.setPreviousHash(previousHash);
        block.setHash(calculateHash(block));
        return mapper.toBlockchainBlockResponse(repository.save(block));
    }

    @Transactional(readOnly = true)
    public List<BlockchainDtos.BlockchainBlockResponse> findAll() {
        return repository.findAll().stream().map(mapper::toBlockchainBlockResponse).toList();
    }

    @Transactional(readOnly = true)
    public BlockchainDtos.ChainValidationResponse validateChain() {
        List<BlockchainBlock> blocks = repository.findAll().stream()
            .sorted((left, right) -> left.getBlockIndex().compareTo(right.getBlockIndex()))
            .toList();

        String previousHash = GENESIS_HASH;
        for (BlockchainBlock block : blocks) {
            if (!previousHash.equals(block.getPreviousHash())) {
                return new BlockchainDtos.ChainValidationResponse(false, "Invalid previous hash at block " + block.getBlockIndex());
            }
            if (!calculateHash(block).equals(block.getHash())) {
                return new BlockchainDtos.ChainValidationResponse(false, "Invalid hash at block " + block.getBlockIndex());
            }
            previousHash = block.getHash();
        }
        return new BlockchainDtos.ChainValidationResponse(true, "Private audit chain is valid");
    }

    private String calculateHash(BlockchainBlock block) {
        String data = block.getBlockIndex() + "|" + block.getTimestamp() + "|" + block.getAction() + "|" + block.getActor() + "|" + block.getPayload() + "|" + block.getPreviousHash();
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(data.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 unavailable", exception);
        }
    }
}

