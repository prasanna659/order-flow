package com.orderflow.auth.config;

import com.orderflow.auth.oauth2.GoogleOidcUserService;
import com.orderflow.auth.oauth2.HttpCookieOAuth2AuthorizationRequestRepository;
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
    private final HttpCookieOAuth2AuthorizationRequestRepository cookieAuthRequestRepository;

    public SecurityConfig(GoogleOidcUserService googleOidcUserService,
                          OAuth2AuthenticationSuccessHandler successHandler,
                          OAuth2AuthenticationFailureHandler failureHandler,
                          HttpCookieOAuth2AuthorizationRequestRepository cookieAuthRequestRepository) {
        this.googleOidcUserService        = googleOidcUserService;
        this.successHandler               = successHandler;
        this.failureHandler               = failureHandler;
        this.cookieAuthRequestRepository  = cookieAuthRequestRepository;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .csrf(AbstractHttpConfigurer::disable)
            // STATELESS: no HttpSession at all — OAuth2 state lives in a cookie.
            // This is required for Railway (and any multi-instance deployment) where
            // the session from the /oauth2/authorization request won't survive to the
            // /login/oauth2/code/google callback if a different instance handles it.
            .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth.anyRequest().permitAll())
            .oauth2Login(oauth2 -> oauth2
                    // Replace the default InMemoryAuthorizationRequestRepository (session-backed)
                    // with our cookie-based implementation so state survives across instances.
                    .authorizationEndpoint(endpoint -> endpoint
                            .authorizationRequestRepository(cookieAuthRequestRepository)
                    )
                    .userInfoEndpoint(ui -> ui.oidcUserService(googleOidcUserService))
                    .successHandler(successHandler)
                    .failureHandler(failureHandler)
            );

        return http.build();
    }
}
