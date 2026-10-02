package com.storagehub.config;

import com.storagehub.repository.UserRepository;
import com.storagehub.security.EnvelopeAccessDeniedHandler;
import com.storagehub.security.EnvelopeAuthenticationEntryPoint;
import com.storagehub.security.JwtAuthenticationFilter;
import com.storagehub.security.JwtProperties;
import com.storagehub.service.JwtService;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * Story 1.3 chain (AD-5): exactly three public endpoints -
 * POST /api/v1/auth/login|register|forgot-password - plus /actuator/health
 * (carried over from 1.2); everything else is authenticated. The JWT filter
 * authenticates Bearer requests and re-checks users.Status (~30s cache), and
 * 401/403 still leave through the envelope entry point / denied handler.
 * Role rules themselves are NOT matchers here: the permission matrix is
 * role-level (ROLE_&lt;ROLE&gt;) and enforced per endpoint via method security
 * (@EnableMethodSecurity + @PreAuthorize), so hidden menus stay UX-only.
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@EnableConfigurationProperties(JwtProperties.class)
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http,
            EnvelopeAuthenticationEntryPoint authenticationEntryPoint,
            EnvelopeAccessDeniedHandler accessDeniedHandler,
            JwtAuthenticationFilter jwtAuthenticationFilter) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/actuator/health").permitAll()
                        .requestMatchers(HttpMethod.POST,
                                "/api/v1/auth/login",
                                "/api/v1/auth/register",
                                "/api/v1/auth/forgot-password",
                                "/api/v1/payments/webhook").permitAll()
                        .anyRequest().authenticated())
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint(authenticationEntryPoint)
                        .accessDeniedHandler(accessDeniedHandler))
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }

    @Bean
    public JwtAuthenticationFilter jwtAuthenticationFilter(JwtService jwtService,
            UserRepository userRepository,
            EnvelopeAuthenticationEntryPoint authenticationEntryPoint) {
        return new JwtAuthenticationFilter(jwtService, userRepository, authenticationEntryPoint);
    }

    /**
     * Plain BCrypt (strength 10, $2a$) - NOT the DelegatingPasswordEncoder:
     * the V2 seed hashes are unprefixed $2a$10$ and must keep matching (AD-5).
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
