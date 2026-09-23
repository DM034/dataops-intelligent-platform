package com.example.dataops.service;

import com.example.dataops.dto.AuthDtos;
import com.example.dataops.mapper.DataopsMapper;
import com.example.dataops.model.AppUser;
import com.example.dataops.model.UserRole;
import com.example.dataops.repository.AppUserRepository;
import com.example.dataops.security.JwtService;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AuthServiceTest {
    private final AppUserRepository userRepository = mock(AppUserRepository.class);
    private final PasswordEncoder passwordEncoder = mock(PasswordEncoder.class);
    private final AuthenticationManager authenticationManager = mock(AuthenticationManager.class);
    private final JwtService jwtService = new JwtService("0123456789abcdef0123456789abcdef", 3600000);
    private final BlockchainService blockchainService = mock(BlockchainService.class);
    private final JournalActiviteService journalActiviteService = mock(JournalActiviteService.class);
    private final AuthService service = new AuthService(
        userRepository,
        passwordEncoder,
        authenticationManager,
        jwtService,
        new DataopsMapper(),
        blockchainService,
        journalActiviteService
    );

    @Test
    void registerAlwaysCreatesSimpleUserEvenWhenAdminRoleIsRequested() {
        when(userRepository.existsByUsername("intrus")).thenReturn(false);
        when(userRepository.existsByEmail("intrus@example.com")).thenReturn(false);
        when(passwordEncoder.encode("Password123")).thenReturn("encoded");
        when(userRepository.save(any(AppUser.class))).thenAnswer(invocation -> {
            AppUser user = invocation.getArgument(0);
            ReflectionTestUtils.setField(user, "id", 42L);
            return user;
        });

        var response = service.register(new AuthDtos.RegisterRequest(
            "intrus",
            "intrus@example.com",
            "Intrus Test",
            "Password123",
            UserRole.ADMIN
        ));

        assertThat(response.user().role()).isEqualTo(UserRole.UTILISATEUR_SIMPLE);
        verify(blockchainService).addBlock("USER_REGISTERED", "USER", 42L, "intrus", "userId=42");
    }
}
