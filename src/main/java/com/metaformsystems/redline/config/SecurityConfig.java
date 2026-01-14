package com.metaformsystems.redline.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.annotation.web.configurers.HeadersConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.web.SecurityFilterChain;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .authorizeHttpRequests(authorize -> authorize
                        .requestMatchers("/api/public/**", "/api/ui/**", "/h2-console/**", "/actuator/health/**").permitAll()
                        .anyRequest().authenticated()
                )
                .oauth2ResourceServer(oauth2 -> oauth2
                        .jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthenticationConverter()))
                )
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                )
                .csrf(AbstractHttpConfigurer::disable);

        // Allow H2 console in frames (dev only)
        http.headers(headers -> headers.frameOptions(HeadersConfigurer.FrameOptionsConfig::sameOrigin));

        return http.build();
    }

    @Bean
    public JwtAuthenticationConverter jwtAuthenticationConverter() {
        JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter(jwt -> {
            JwtGrantedAuthoritiesConverter defaultConverter = new JwtGrantedAuthoritiesConverter();
            Collection<GrantedAuthority> defaultAuthorities = defaultConverter.convert(jwt);

            // Extract realm roles from Keycloak token
            Map<String, Object> realmAccess = jwt.getClaim("realm_access");
            Collection<GrantedAuthority> realmRoles = List.of();
            if (realmAccess != null && realmAccess.get("roles") instanceof List<?> roles) {
                realmRoles = roles.stream()
                        .filter(role -> role instanceof String)
                        .map(role -> new SimpleGrantedAuthority("ROLE_" + role))
                        .map(GrantedAuthority.class::cast)
                        .toList();
            }

            // Extract resource roles from Keycloak token
            Map<String, Object> resourceAccess = jwt.getClaim("resource_access");
            Collection<GrantedAuthority> resourceRoles = List.of();
            if (resourceAccess != null) {
                resourceRoles = resourceAccess.values().stream()
                        .filter(resource -> resource instanceof Map<?, ?>)
                        .map(resource -> (Map<String, Object>) resource)
                        .filter(resource -> resource.containsKey("roles"))
                        .flatMap(resource -> {
                            Object rolesObj = resource.get("roles");
                            if (rolesObj instanceof List<?> roles) {
                                return roles.stream()
                                        .filter(role -> role instanceof String)
                                        .map(role -> new SimpleGrantedAuthority("ROLE_" + role));
                            }
                            return Stream.empty();
                        })
                        .map(GrantedAuthority.class::cast)
                        .toList();
            }

            return Stream.of(defaultAuthorities, realmRoles, resourceRoles)
                    .flatMap(Collection::stream)
                    .toList();
        });

        return converter;
    }
}
