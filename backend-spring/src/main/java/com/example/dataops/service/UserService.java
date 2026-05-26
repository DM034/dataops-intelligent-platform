package com.example.dataops.service;

import com.example.dataops.dto.UserDtos;
import com.example.dataops.dto.UserResponse;
import com.example.dataops.exception.BusinessException;
import com.example.dataops.exception.ResourceNotFoundException;
import com.example.dataops.mapper.DataopsMapper;
import com.example.dataops.model.AppUser;
import com.example.dataops.model.UserRole;
import com.example.dataops.repository.AppUserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class UserService {
    private final AppUserRepository repository;
    private final PasswordEncoder passwordEncoder;
    private final DataopsMapper mapper;
    private final BlockchainService blockchainService;

    public UserService(AppUserRepository repository, PasswordEncoder passwordEncoder, DataopsMapper mapper, BlockchainService blockchainService) {
        this.repository = repository;
        this.passwordEncoder = passwordEncoder;
        this.mapper = mapper;
        this.blockchainService = blockchainService;
    }

    @Transactional(readOnly = true)
    public List<UserResponse> findAll() {
        return repository.findAll().stream().map(mapper::toUserResponse).toList();
    }

    @Transactional(readOnly = true)
    public UserResponse findById(Long id) {
        return mapper.toUserResponse(getEntity(id));
    }

    @Transactional
    public UserResponse create(UserDtos.CreateUserRequest request) {
        if (repository.existsByUsername(request.username())) {
            throw new BusinessException("Username already exists");
        }
        if (repository.existsByEmail(request.email())) {
            throw new BusinessException("Email already exists");
        }
        AppUser user = new AppUser();
        user.setUsername(request.username());
        user.setEmail(request.email());
        user.setFullName(request.fullName());
        user.setPasswordHash(passwordEncoder.encode(request.password()));
        user.setRole(request.role() == null ? UserRole.ANALYST : request.role());
        user.setActive(request.active() == null || request.active());
        AppUser saved = repository.save(user);
        blockchainService.append("USER_CREATED", "system", "userId=" + saved.getId());
        return mapper.toUserResponse(saved);
    }

    @Transactional
    public UserResponse update(Long id, UserDtos.UpdateUserRequest request) {
        AppUser user = getEntity(id);
        if (request.email() != null) {
            user.setEmail(request.email());
        }
        if (request.fullName() != null) {
            user.setFullName(request.fullName());
        }
        if (request.password() != null && !request.password().isBlank()) {
            user.setPasswordHash(passwordEncoder.encode(request.password()));
        }
        if (request.role() != null) {
            user.setRole(request.role());
        }
        if (request.active() != null) {
            user.setActive(request.active());
        }
        blockchainService.append("USER_UPDATED", "system", "userId=" + id);
        return mapper.toUserResponse(user);
    }

    @Transactional
    public void delete(Long id) {
        repository.delete(getEntity(id));
        blockchainService.append("USER_DELETED", "system", "userId=" + id);
    }

    private AppUser getEntity(Long id) {
        return repository.findById(id).orElseThrow(() -> new ResourceNotFoundException("User not found: " + id));
    }
}

