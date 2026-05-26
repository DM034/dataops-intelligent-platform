package com.example.dataops.service;

import com.example.dataops.dto.AuthDtos;
import com.example.dataops.exception.BusinessException;
import com.example.dataops.mapper.DataopsMapper;
import com.example.dataops.model.AppUser;
import com.example.dataops.model.UserRole;
import com.example.dataops.repository.AppUserRepository;
import com.example.dataops.security.JwtService;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {
    private final AppUserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final DataopsMapper mapper;
    private final BlockchainService blockchainService;

    public AuthService(AppUserRepository userRepository, PasswordEncoder passwordEncoder, AuthenticationManager authenticationManager, JwtService jwtService, DataopsMapper mapper, BlockchainService blockchainService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.authenticationManager = authenticationManager;
        this.jwtService = jwtService;
        this.mapper = mapper;
        this.blockchainService = blockchainService;
    }

    @Transactional
    public AuthDtos.TokenResponse register(AuthDtos.RegisterRequest request) {
        if (userRepository.existsByUsername(request.username())) {
            throw new BusinessException("Username already exists");
        }
        if (userRepository.existsByEmail(request.email())) {
            throw new BusinessException("Email already exists");
        }

        AppUser user = new AppUser();
        user.setUsername(request.username());
        user.setEmail(request.email());
        user.setFullName(request.fullName());
        user.setPasswordHash(passwordEncoder.encode(request.password()));
        user.setRole(request.role() == null ? UserRole.ANALYST : request.role());
        AppUser saved = userRepository.save(user);
        blockchainService.append("USER_REGISTERED", saved.getUsername(), "userId=" + saved.getId());
        return tokenFor(saved);
    }

    @Transactional(readOnly = true)
    public AuthDtos.TokenResponse login(AuthDtos.LoginRequest request) {
        authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(request.username(), request.password()));
        AppUser user = userRepository.findByUsername(request.username()).orElseThrow(() -> new BusinessException("Invalid credentials"));
        return tokenFor(user);
    }

    private AuthDtos.TokenResponse tokenFor(AppUser user) {
        return new AuthDtos.TokenResponse(jwtService.generateToken(user.getUsername()), "Bearer", mapper.toUserResponse(user));
    }
}

