package com.fdic.tip.usertransactionlog.config;

import static com.fdic.tip.usertransactionlog.constants.SecurityRoles.*;
import static com.fdic.tip.usertransactionlog.constants.UserTransactionLogConstants.BASE_PATH;
import static com.fdic.tip.usertransactionlog.constants.UserTransactionLogConstants.EXPORT_PATH;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    /** Local testing: no authentication required. */
    @Bean
    @Profile("local")
    SecurityFilterChain localSecurityFilterChain(HttpSecurity http) throws Exception {
        http.csrf(AbstractHttpConfigurer::disable)
                .authorizeHttpRequests(auth -> auth.anyRequest().permitAll());
        return http.build();
    }

    /** Entra ID JWT (roles claim). URL-level rules; the export rule must come before the general view rule. */
    @Bean
    @Profile("!local")
    SecurityFilterChain secureSecurityFilterChain(HttpSecurity http) throws Exception {
        http.csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/actuator/health/**").permitAll()
                        .requestMatchers(HttpMethod.GET, BASE_PATH + EXPORT_PATH).hasAnyRole(EXPORT_ROLES)
                        .requestMatchers(HttpMethod.GET, BASE_PATH, BASE_PATH + "/**").hasAnyRole(VIEW_ROLES)
                        .anyRequest().denyAll())
                .oauth2ResourceServer(oauth -> oauth
                        .jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthenticationConverter())));
        return http.build();
    }

    private JwtAuthenticationConverter jwtAuthenticationConverter() {
        JwtGrantedAuthoritiesConverter authorities = new JwtGrantedAuthoritiesConverter();
        authorities.setAuthoritiesClaimName(ROLES_CLAIM);
        authorities.setAuthorityPrefix(ROLE_PREFIX);

        JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter(authorities);
        return converter;
    }
}
