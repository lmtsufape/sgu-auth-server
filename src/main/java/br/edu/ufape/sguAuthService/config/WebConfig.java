package br.edu.ufape.sguAuthService.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;


@EnableWebSecurity
@Configuration
public class WebConfig {


    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS)  // Stateless session management
                )
                .authorizeHttpRequests(authz -> authz
                        .requestMatchers(HttpMethod.POST,"/usuario").permitAll()
                        .requestMatchers("/security/**").permitAll()
                        .requestMatchers("/api-doc/**").permitAll()
                        .requestMatchers("/swagger-ui/**").permitAll()
                        .requestMatchers("/login").permitAll()
                        .requestMatchers("/refresh").permitAll()
                        .requestMatchers("/logout").permitAll()
                        .requestMatchers("/reset-password").permitAll()
                        .requestMatchers(HttpMethod.GET, "/tipoEtnia").permitAll()
                        .requestMatchers(HttpMethod.POST, "/aluno/public/batch").permitAll()
                        .anyRequest().authenticated()
                ).oauth2ResourceServer(auth -> auth.jwt(token -> token.jwtAuthenticationConverter(new KeycloakJwtAuthenticationConverter())));
        return http.build();
    }

}