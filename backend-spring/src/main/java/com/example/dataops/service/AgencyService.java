package com.example.dataops.service;

import com.example.dataops.dto.AgencyDtos;
import com.example.dataops.exception.BusinessException;
import com.example.dataops.exception.ResourceNotFoundException;
import com.example.dataops.mapper.DataopsMapper;
import com.example.dataops.model.Agency;
import com.example.dataops.repository.AgencyRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class AgencyService {
    private final AgencyRepository repository;
    private final DataopsMapper mapper;
    private final BlockchainService blockchainService;

    public AgencyService(AgencyRepository repository, DataopsMapper mapper, BlockchainService blockchainService) {
        this.repository = repository;
        this.mapper = mapper;
        this.blockchainService = blockchainService;
    }

    @Transactional(readOnly = true)
    public List<AgencyDtos.AgencyResponse> findAll() {
        return repository.findAll().stream().map(mapper::toAgencyResponse).toList();
    }

    @Transactional(readOnly = true)
    public AgencyDtos.AgencyResponse findById(Long id) {
        return mapper.toAgencyResponse(getEntity(id));
    }

    @Transactional
    public AgencyDtos.AgencyResponse create(AgencyDtos.AgencyRequest request) {
        if (repository.existsByCode(request.code())) {
            throw new BusinessException("Agency code already exists");
        }
        Agency agency = new Agency();
        apply(request, agency);
        Agency saved = repository.save(agency);
        blockchainService.append("AGENCY_CREATED", "system", "agencyId=" + saved.getId());
        return mapper.toAgencyResponse(saved);
    }

    @Transactional
    public AgencyDtos.AgencyResponse update(Long id, AgencyDtos.AgencyRequest request) {
        Agency agency = getEntity(id);
        apply(request, agency);
        blockchainService.append("AGENCY_UPDATED", "system", "agencyId=" + id);
        return mapper.toAgencyResponse(agency);
    }

    @Transactional
    public void delete(Long id) {
        repository.delete(getEntity(id));
        blockchainService.append("AGENCY_DELETED", "system", "agencyId=" + id);
    }

    public Agency getEntity(Long id) {
        return repository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Agency not found: " + id));
    }

    public Agency getByCode(String code) {
        return repository.findByCode(code).orElseThrow(() -> new ResourceNotFoundException("Agency not found: " + code));
    }

    private void apply(AgencyDtos.AgencyRequest request, Agency agency) {
        agency.setCode(request.code());
        agency.setName(request.name());
        agency.setCity(request.city());
        agency.setActive(request.active() == null || request.active());
    }
}

