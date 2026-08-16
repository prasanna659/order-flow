package com.orderflow.auth.config;

import com.orderflow.auth.oauth2.GoogleOidcUserService;
import com.orderflow.auth.oauth2.OAuth2AuthenticationFailureHandler;
import com.orderflow.auth.oauth2.OAuth2AuthenticationSuccessHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class SecurityConfig {

    private final GoogleOidcUserService googleOidcUserService;
    private final OAuth2AuthenticationSuccessHandler successHandler;
    private final OAuth2AuthenticationFailureHandler failureHandler;

    public SecurityConfig(GoogleOidcUserService googleOidcUserService,
                          OAuth2AuthenticationSuccessHandler successHandler,
                          OAuth2AuthenticationFailureHandler failureHandler) {
        this.googleOidcUserService = googleOidcUserService;
        this.successHandler         = successHandler;
        this.failureHandler         = failureHandler;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .csrf(AbstractHttpConfigurer::disable)
            // IF_REQUIRED: session only created for OAuth2 state storage
            .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED))
            .authorizeHttpRequests(auth -> auth.anyRequest().permitAll())
            .oauth2Login(oauth2 -> oauth2
                    .userInfoEndpoint(ui -> ui.oidcUserService(googleOidcUserService))
                    .successHandler(successHandler)
                    // Without a failureHandler, Spring redirects to /login?error
                    // which doesn't exist here → Whitelabel Error Page (500).
                    // Our handler redirects to the frontend /auth?error=... instead.
                    .failureHandler(failureHandler)
            );

        return http.build();
    }
}
