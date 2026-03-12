package com.payvue.app.infrastructure.security;

import com.payvue.app.domain.security.SessionTokenRepository;
import com.payvue.app.domain.security.TokenProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

@Configuration
@EnableWebSecurity
public class SecurityConfiguration {

    private static final String AUTHORIZATION_HEADER_PREFIX = "Bearer ";

    @Bean
    public TokenHeaderResolver tokenHeaderResolver() {
        return request -> {
            String header = request.getHeader(HttpHeaders.AUTHORIZATION);
            if (header == null || !header.startsWith(AUTHORIZATION_HEADER_PREFIX)) {
                return null;
            }
            return header.substring(AUTHORIZATION_HEADER_PREFIX.length());
        };
    }

    @Bean
    public JwtAuthenticationFilter jwtAuthenticationFilter(
            TokenProvider jwtProvider,
            UserDetailsService userDetailService,
            SessionTokenRepository sessionTokenRepository,
            TokenHeaderResolver tokenHeaderResolver
    ) {
        return new JwtAuthenticationFilter(jwtProvider, userDetailService, sessionTokenRepository, tokenHeaderResolver);
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOrigins(List.of("http://localhost:3000"));
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS"));
        config.setAllowedHeaders(List.of("*"));
        config.setAllowCredentials(true);
        config.setExposedHeaders(List.of(HttpHeaders.AUTHORIZATION));

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }

    @Bean
    @Order(1)
    SecurityFilterChain swaggerFilterChain(HttpSecurity http) throws Exception {
        return http
                .securityMatcher("/swagger-ui/**", "/v3/api-docs/**")
                .csrf(AbstractHttpConfigurer::disable)
                .authorizeHttpRequests(auth -> auth.anyRequest().permitAll())
                .build();
    }

    @Bean
    @Order(2)
    SecurityFilterChain oauth2FilterChain(
            HttpSecurity http,
            OAuth2AuthenticationSuccessHandler oauth2SuccessHandler
    ) throws Exception {
        return http
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .csrf(AbstractHttpConfigurer::disable)
                .securityMatcher("/oauth2/**", "/login/oauth2/**", "/auth/oauth/**")
                .headers(headers -> headers
                        .crossOriginOpenerPolicy(coop -> coop.policy(
                                org.springframework.security.web.header.writers.CrossOriginOpenerPolicyHeaderWriter
                                        .CrossOriginOpenerPolicy.SAME_ORIGIN_ALLOW_POPUPS
                        ))
                )
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED))
                .authorizeHttpRequests(auth -> auth.anyRequest().permitAll())
                .oauth2Login(oauth -> oauth
                        .successHandler(oauth2SuccessHandler)
                )
                .build();
    }

    @Bean
    @Order(3)
    SecurityFilterChain apiFilterChain(
            HttpSecurity http,
            JwtAuthenticationFilter jwtFilter,
            RestAccessDeniedHandler accessDeniedHandler,
            RestAuthenticationEntryPoint authenticationEntryPoint
    ) throws Exception {
        return http
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(HttpMethod.GET, "/payment/redirect/confirm/**").permitAll()
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        .requestMatchers(
                                "/api/auth/**",
                                "/auth/oauth/**",
                                "/favicon.ico",
                                "/user/auth/login",
                                "/user/account/create",
                                "/user/account/exist"
                        ).permitAll()
                        .anyRequest().authenticated()
                )
                .exceptionHandling(exceptions -> exceptions
                        .accessDeniedHandler(accessDeniedHandler)
                        .authenticationEntryPoint(authenticationEntryPoint)
                )
                .httpBasic(AbstractHttpConfigurer::disable)
                .formLogin(AbstractHttpConfigurer::disable)
                .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class)
                .build();
    }
}
