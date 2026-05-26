package com.example.dataops.service;

import com.example.dataops.dto.StockDtos;
import com.example.dataops.exception.ResourceNotFoundException;
import com.example.dataops.mapper.DataopsMapper;
import com.example.dataops.model.StockMovement;
import com.example.dataops.repository.StockMovementRepository;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigInteger;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class StockService {
    private final StockMovementRepository repository;
    private final AgencyService agencyService;
    private final ProductService productService;
    private final DataopsMapper mapper;
    private final BlockchainService blockchainService;

    public StockService(StockMovementRepository repository, AgencyService agencyService, ProductService productService, DataopsMapper mapper, BlockchainService blockchainService) {
        this.repository = repository;
        this.agencyService = agencyService;
        this.productService = productService;
        this.mapper = mapper;
        this.blockchainService = blockchainService;
    }

    @Transactional(readOnly = true)
    public List<StockDtos.StockMovementResponse> findAll() {
        return repository.findAll().stream().map(mapper::toStockMovementResponse).toList();
    }

    @Transactional(readOnly = true)
    public StockDtos.StockMovementResponse findById(Long id) {
        return mapper.toStockMovementResponse(getEntity(id));
    }

    @Transactional(readOnly = true)
    public List<StockDtos.StockLevelResponse> stockLevels() {
        return repository.stockLevels().stream()
            .map(row -> new StockDtos.StockLevelResponse((String) row[0], (String) row[1], toLong(row[2])))
            .toList();
    }

    @Transactional
    public StockDtos.StockMovementResponse create(StockDtos.StockMovementRequest request) {
        StockMovement movement = new StockMovement();
        movement.setAgency(agencyService.getEntity(request.agencyId()));
        movement.setProduct(productService.getEntity(request.productId()));
        movement.setType(request.type());
        movement.setQuantity(request.quantity());
        movement.setMovementDate(request.movementDate() == null ? LocalDateTime.now() : request.movementDate());
        movement.setReason(request.reason());
        StockMovement saved = repository.save(movement);
        blockchainService.addBlock("CREATE", "STOCK", saved.getId(), currentUserId(), stockData(saved));
        return mapper.toStockMovementResponse(saved);
    }

    @Transactional
    public StockDtos.StockMovementResponse update(Long id, StockDtos.StockMovementRequest request) {
        StockMovement movement = getEntity(id);
        movement.setAgency(agencyService.getEntity(request.agencyId()));
        movement.setProduct(productService.getEntity(request.productId()));
        movement.setType(request.type());
        movement.setQuantity(request.quantity());
        movement.setMovementDate(request.movementDate() == null ? movement.getMovementDate() : request.movementDate());
        movement.setReason(request.reason());
        blockchainService.addBlock("UPDATE", "STOCK", id, currentUserId(), stockData(movement));
        return mapper.toStockMovementResponse(movement);
    }

    @Transactional
    public void delete(Long id) {
        repository.delete(getEntity(id));
        blockchainService.append("STOCK_MOVEMENT_DELETED", "system", "stockMovementId=" + id);
    }

    private StockMovement getEntity(Long id) {
        return repository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Stock movement not found: " + id));
    }

    private Long toLong(Object value) {
        if (value instanceof Long typed) {
            return typed;
        }
        if (value instanceof Integer typed) {
            return typed.longValue();
        }
        if (value instanceof BigInteger typed) {
            return typed.longValue();
        }
        if (value instanceof Number typed) {
            return typed.longValue();
        }
        return 0L;
    }

    private String stockData(StockMovement movement) {
        return movement.getAgency().getId() + "|" + movement.getProduct().getId() + "|" + movement.getType() + "|" + movement.getQuantity() + "|" + movement.getMovementDate() + "|" + movement.getReason();
    }

    private String currentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return authentication == null ? "system" : authentication.getName();
    }
}
