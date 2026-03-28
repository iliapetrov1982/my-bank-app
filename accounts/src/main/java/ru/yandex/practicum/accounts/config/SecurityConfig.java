package ru.yandex.practicum.accounts.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        // Эндпоинты для пользователя (User JWT — Authorization Code Flow)
                        .requestMatchers(HttpMethod.GET, "/api/accounts/me").hasAuthority("SCOPE_accounts.read")
                        .requestMatchers(HttpMethod.PUT, "/api/accounts/me").hasAuthority("SCOPE_accounts.write")
                        .requestMatchers(HttpMethod.GET, "/api/accounts").hasAuthority("SCOPE_accounts.read")
                        // Эндпоинты для микросервисов (Service JWT — Client Credentials Flow)
                        .requestMatchers("/api/accounts/*/deposit").hasAuthority("SCOPE_accounts.write")
                        .requestMatchers("/api/accounts/*/withdraw").hasAuthority("SCOPE_accounts.write")
                        .anyRequest().authenticated()
                )
                .oauth2ResourceServer(oauth2 ->
                        oauth2.jwt(jwt -> {}));
        return http.build();
    }
}