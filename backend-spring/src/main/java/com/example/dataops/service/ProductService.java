package com.example.dataops.service;

import com.example.dataops.dto.ProductDtos;
import com.example.dataops.exception.BusinessException;
import com.example.dataops.exception.ResourceNotFoundException;
import com.example.dataops.mapper.DataopsMapper;
import com.example.dataops.model.Product;
import com.example.dataops.repository.ProductRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class ProductService {
    private final ProductRepository repository;
    private final DataopsMapper mapper;
    private final BlockchainService blockchainService;

    public ProductService(ProductRepository repository, DataopsMapper mapper, BlockchainService blockchainService) {
        this.repository = repository;
        this.mapper = mapper;
        this.blockchainService = blockchainService;
    }

    @Transactional(readOnly = true)
    public List<ProductDtos.ProductResponse> findAll() {
        return repository.findAll().stream().map(mapper::toProductResponse).toList();
    }

    @Transactional(readOnly = true)
    public ProductDtos.ProductResponse findById(Long id) {
        return mapper.toProductResponse(getEntity(id));
    }

    @Transactional
    public ProductDtos.ProductResponse create(ProductDtos.ProductRequest request) {
        if (repository.existsBySku(request.sku())) {
            throw new BusinessException("Product SKU already exists");
        }
        Product product = new Product();
        apply(request, product);
        Product saved = repository.save(product);
        blockchainService.append("PRODUCT_CREATED", "system", "productId=" + saved.getId());
        return mapper.toProductResponse(saved);
    }

    @Transactional
    public ProductDtos.ProductResponse update(Long id, ProductDtos.ProductRequest request) {
        Product product = getEntity(id);
        apply(request, product);
        blockchainService.append("PRODUCT_UPDATED", "system", "productId=" + id);
        return mapper.toProductResponse(product);
    }

    @Transactional
    public void delete(Long id) {
        repository.delete(getEntity(id));
        blockchainService.append("PRODUCT_DELETED", "system", "productId=" + id);
    }

    public Product getEntity(Long id) {
        return repository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Product not found: " + id));
    }

    public Product getBySku(String sku) {
        return repository.findBySku(sku).orElseThrow(() -> new ResourceNotFoundException("Product not found: " + sku));
    }

    private void apply(ProductDtos.ProductRequest request, Product product) {
        product.setSku(request.sku());
        product.setName(request.name());
        product.setCategory(request.category());
        product.setUnitPrice(request.unitPrice());
        product.setActive(request.active() == null || request.active());
    }
}

