package com.example.dataops.config;

import com.example.dataops.security.JwtAuthenticationFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {
    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    public SecurityConfig(JwtAuthenticationFilter jwtAuthenticationFilter) {
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        return http
            .csrf(csrf -> csrf.disable())
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/api/auth/**", "/api/health", "/swagger-ui/**", "/v3/api-docs/**").permitAll()
                .requestMatchers("/api/users/**").hasRole("ADMIN")
                .requestMatchers(HttpMethod.GET, "/api/products/**", "/api/agencies/**", "/api/stock/levels", "/api/dashboard/global")
                    .hasAnyRole("ADMIN", "DIRECTION", "RESPONSABLE_PRODUCTION", "RESPONSABLE_STOCK", "RESPONSABLE_ACHAT", "UTILISATEUR_SIMPLE", "MANAGER", "ANALYST")
                .requestMatchers("/api/dashboard/**", "/api/kpi/**", "/api/rapports/export/**", "/api/ai/benchmark/**")
                    .hasAnyRole("ADMIN", "DIRECTION", "MANAGER", "ANALYST")
                .requestMatchers("/api/sales/**", "/api/import/sales", "/api/ai/sales-anomalies")
                    .hasAnyRole("ADMIN", "DIRECTION", "RESPONSABLE_PRODUCTION", "MANAGER", "ANALYST")
                .requestMatchers("/api/stock/**", "/api/import/stocks", "/api/ai/stock-predictions")
                    .hasAnyRole("ADMIN", "DIRECTION", "RESPONSABLE_STOCK", "MANAGER", "ANALYST")
                .requestMatchers("/api/products/**", "/api/agencies/**")
                    .hasAnyRole("ADMIN", "DIRECTION", "RESPONSABLE_PRODUCTION", "RESPONSABLE_STOCK", "RESPONSABLE_ACHAT", "MANAGER", "ANALYST")
                .requestMatchers("/api/alertes/**", "/api/alerts/**")
                    .hasAnyRole("ADMIN", "DIRECTION", "RESPONSABLE_PRODUCTION", "RESPONSABLE_STOCK", "RESPONSABLE_QUALITE", "RESPONSABLE_ACHAT", "MANAGER", "ANALYST")
                .requestMatchers("/api/recommendations/**")
                    .hasAnyRole("ADMIN", "DIRECTION", "RESPONSABLE_STOCK", "RESPONSABLE_ACHAT", "MANAGER", "ANALYST")
                .requestMatchers("/api/regles-metier/**")
                    .hasAnyRole("ADMIN", "DIRECTION", "RESPONSABLE_PRODUCTION", "RESPONSABLE_STOCK", "RESPONSABLE_QUALITE", "RESPONSABLE_ACHAT", "MANAGER", "ANALYST")
                .requestMatchers("/api/catalog/**", "/api/data-quality/**", "/api/data-lineage/**", "/api/lineage/**", "/api/import-audit/**", "/api/governance/**", "/api/blockchain/**", "/api/historique/**")
                    .hasAnyRole("ADMIN", "DIRECTION", "MANAGER", "ANALYST")
                .anyRequest().authenticated()
            )
            .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
            .build();
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration configuration) throws Exception {
        return configuration.getAuthenticationManager();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(List.of("http://localhost:5173"));
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("Authorization", "Content-Type"));
        configuration.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}
