package com.example.dataops.service;

import com.example.dataops.dto.AlertDtos;
import com.example.dataops.exception.ResourceNotFoundException;
import com.example.dataops.mapper.DataopsMapper;
import com.example.dataops.model.Alert;
import com.example.dataops.repository.AlertRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class AlertService {
    private final AlertRepository repository;
    private final DataopsMapper mapper;
    private final BlockchainService blockchainService;

    public AlertService(AlertRepository repository, DataopsMapper mapper, BlockchainService blockchainService) {
        this.repository = repository;
        this.mapper = mapper;
        this.blockchainService = blockchainService;
    }

    @Transactional(readOnly = true)
    public List<AlertDtos.AlertResponse> findAll(boolean activeOnly) {
        return (activeOnly ? repository.findByResolvedFalseOrderByCreatedAtDesc() : repository.findAll())
            .stream().map(mapper::toAlertResponse).toList();
    }

    @Transactional
    public AlertDtos.AlertResponse create(AlertDtos.AlertRequest request) {
        Alert alert = new Alert();
        alert.setSeverity(request.severity());
        alert.setTitle(request.title());
        alert.setMessage(request.message());
        Alert saved = repository.save(alert);
        blockchainService.append("ALERT_CREATED", "system", "alertId=" + saved.getId());
        return mapper.toAlertResponse(saved);
    }

    @Transactional
    public AlertDtos.AlertResponse resolve(Long id) {
        Alert alert = repository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Alert not found: " + id));
        alert.setResolved(true);
        blockchainService.append("ALERT_RESOLVED", "system", "alertId=" + id);
        return mapper.toAlertResponse(alert);
    }

    @Transactional(readOnly = true)
    public long activeCount() {
        return repository.findByResolvedFalseOrderByCreatedAtDesc().size();
    }
}

