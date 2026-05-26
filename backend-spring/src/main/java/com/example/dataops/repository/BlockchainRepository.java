package com.example.dataops.repository;

import com.example.dataops.model.BlockchainBlock;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface BlockchainRepository extends JpaRepository<BlockchainBlock, Long> {
    Optional<BlockchainBlock> findTopByOrderByIdDesc();
}

