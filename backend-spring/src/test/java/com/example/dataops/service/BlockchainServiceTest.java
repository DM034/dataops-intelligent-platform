package com.example.dataops.service;

import com.example.dataops.mapper.DataopsMapper;
import com.example.dataops.model.BlockchainBlock;
import com.example.dataops.repository.BlockchainRepository;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class BlockchainServiceTest {
    private final BlockchainRepository repository = mock(BlockchainRepository.class);
    private final BlockchainService service = new BlockchainService(repository, new DataopsMapper());

    @Test
    void addBlockUsesTechnicalEntityIdWhenEntityIdIsMissing() {
        when(repository.findTopByOrderByIdDesc()).thenReturn(Optional.empty());
        when(repository.save(any(BlockchainBlock.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var block = service.addBlock("AUDIT_EVENT", "AUDIT", null, "admin", "payload");

        assertThat(block.entityId()).isZero();
        assertThat(block.previousHash()).isEqualTo("0");
        assertThat(block.currentHash()).isNotBlank();
    }

    @Test
    void repairChainRebuildsPreviousAndCurrentHashes() {
        BlockchainBlock first = block(1L, "SALE_CREATED", "SALE", 10L, "admin", "hash-data-1");
        BlockchainBlock second = block(2L, "STOCK_UPDATED", "STOCK", 20L, "admin", "hash-data-2");
        first.setPreviousHash("broken");
        first.setCurrentHash("broken");
        second.setPreviousHash("broken");
        second.setCurrentHash("broken");
        List<BlockchainBlock> blocks = List.of(first, second);

        when(repository.findAll()).thenReturn(blocks);

        var response = service.repairChain();

        assertThat(response.valid()).isTrue();
        assertThat(first.getPreviousHash()).isEqualTo("0");
        assertThat(second.getPreviousHash()).isEqualTo(first.getCurrentHash());
        assertThat(first.getCurrentHash()).isEqualTo(service.calculateHash(first));
        assertThat(second.getCurrentHash()).isEqualTo(service.calculateHash(second));
        verify(repository).saveAll(blocks);
    }

    private BlockchainBlock block(Long id, String action, String entityType, Long entityId, String userId, String dataHash) {
        BlockchainBlock block = new BlockchainBlock();
        ReflectionTestUtils.setField(block, "id", id);
        block.setTimestamp(Instant.parse("2026-01-01T00:00:00Z").plusSeconds(id).truncatedTo(ChronoUnit.MICROS));
        block.setAction(action);
        block.setEntityType(entityType);
        block.setEntityId(entityId);
        block.setUserId(userId);
        block.setDataHash(dataHash);
        return block;
    }
}
