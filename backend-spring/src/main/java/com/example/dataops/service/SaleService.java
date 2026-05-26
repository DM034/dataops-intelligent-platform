package com.example.dataops.service;

import com.example.dataops.dto.SaleDtos;
import com.example.dataops.exception.ResourceNotFoundException;
import com.example.dataops.mapper.DataopsMapper;
import com.example.dataops.model.Agency;
import com.example.dataops.model.Product;
import com.example.dataops.model.Sale;
import com.example.dataops.repository.SaleRepository;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
public class SaleService {
    private final SaleRepository repository;
    private final AgencyService agencyService;
    private final ProductService productService;
    private final DataopsMapper mapper;
    private final BlockchainService blockchainService;

    public SaleService(SaleRepository repository, AgencyService agencyService, ProductService productService, DataopsMapper mapper, BlockchainService blockchainService) {
        this.repository = repository;
        this.agencyService = agencyService;
        this.productService = productService;
        this.mapper = mapper;
        this.blockchainService = blockchainService;
    }

    @Transactional(readOnly = true)
    public List<SaleDtos.SaleResponse> findAll() {
        return repository.findAll().stream().map(mapper::toSaleResponse).toList();
    }

    @Transactional(readOnly = true)
    public SaleDtos.SaleResponse findById(Long id) {
        return mapper.toSaleResponse(getEntity(id));
    }

    @Transactional
    public SaleDtos.SaleResponse create(SaleDtos.SaleRequest request) {
        Sale sale = buildSale(request, new Sale());
        Sale saved = repository.save(sale);
        blockchainService.addBlock("CREATE", "SALE", saved.getId(), currentUserId(), saleData(saved));
        return mapper.toSaleResponse(saved);
    }

    @Transactional
    public SaleDtos.SaleResponse update(Long id, SaleDtos.SaleRequest request) {
        Sale sale = buildSale(request, getEntity(id));
        blockchainService.addBlock("UPDATE", "SALE", id, currentUserId(), saleData(sale));
        return mapper.toSaleResponse(sale);
    }

    @Transactional
    public void delete(Long id) {
        repository.delete(getEntity(id));
        blockchainService.append("SALE_DELETED", "system", "saleId=" + id);
    }

    private Sale getEntity(Long id) {
        return repository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Sale not found: " + id));
    }

    private Sale buildSale(SaleDtos.SaleRequest request, Sale sale) {
        Agency agency = agencyService.getEntity(request.agencyId());
        Product product = productService.getEntity(request.productId());
        BigDecimal unitPrice = request.unitPrice() == null ? product.getUnitPrice() : request.unitPrice();
        sale.setAgency(agency);
        sale.setProduct(product);
        sale.setQuantity(request.quantity());
        sale.setUnitPrice(unitPrice);
        sale.setTotalAmount(unitPrice.multiply(BigDecimal.valueOf(request.quantity())));
        sale.setSaleDate(request.saleDate());
        sale.setReference(request.reference());
        return sale;
    }

    private String saleData(Sale sale) {
        return sale.getAgency().getId() + "|" + sale.getProduct().getId() + "|" + sale.getQuantity() + "|" + sale.getUnitPrice() + "|" + sale.getTotalAmount() + "|" + sale.getSaleDate() + "|" + sale.getReference();
    }

    private String currentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return authentication == null ? "system" : authentication.getName();
    }
}
