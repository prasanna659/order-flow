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
import org.springframework.security.oauth2.client.web.OAuth2LoginAuthenticationFilter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.config.annotation.ObjectPostProcessor;

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
        this.googleOidcUserService       = googleOidcUserService;
        this.successHandler              = successHandler;
        this.failureHandler              = failureHandler;
        this.cookieAuthRequestRepository = cookieAuthRequestRepository;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .csrf(AbstractHttpConfigurer::disable)
            // STATELESS: no HttpSession at all — OAuth2 state is stored in a cookie.
            .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth.anyRequest().permitAll())
            .oauth2Login(oauth2 -> oauth2
                    // Wire cookie repo into the outbound redirect filter (builds the Google URL).
                    .authorizationEndpoint(endpoint -> endpoint
                            .authorizationRequestRepository(cookieAuthRequestRepository)
                    )
                    // Wire cookie repo into the inbound callback filter (handles Google's redirect back).
                    // ObjectPostProcessor is the only reliable way to reach OAuth2LoginAuthenticationFilter
                    // because Spring Security constructs it internally and doesn't expose a setter on the DSL.
                    // Without this, the callback filter uses its own default session-backed repository,
                    // finds no session, and throws [authorization_request_not_found].
                    .withObjectPostProcessor(new ObjectPostProcessor<OAuth2LoginAuthenticationFilter>() {
                        @Override
                        public <O extends OAuth2LoginAuthenticationFilter> O postProcess(O filter) {
                            filter.setAuthorizationRequestRepository(cookieAuthRequestRepository);
                            return filter;
                        }
                    })
                    .userInfoEndpoint(ui -> ui.oidcUserService(googleOidcUserService))
                    .successHandler(successHandler)
                    .failureHandler(failureHandler)
            );

        return http.build();
    }
}
