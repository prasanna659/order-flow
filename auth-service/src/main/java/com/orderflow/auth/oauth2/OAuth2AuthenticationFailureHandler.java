package com.orderflow.auth.oauth2;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationFailureHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

/**
 * Called by Spring Security when OAuth2 authentication fails for any reason:
 *   - Google access denied by user
 *   - State parameter mismatch (session expired between redirect and callback)
 *   - User provisioning failure (DB error)
 *   - Any exception thrown from GoogleOAuth2UserService
 *
 * Instead of redirecting to Spring's default /login?error (which doesn't exist
 * on this service and causes the Whitelabel Error Page), we redirect to the
 * frontend's /auth route with an error query param so the UI can display it.
 *
 * Redirect URL: http://localhost:3000/auth?error=<encoded-message>
 */
@Component
public class OAuth2AuthenticationFailureHandler extends SimpleUrlAuthenticationFailureHandler {

    private static final Logger log = LoggerFactory.getLogger(OAuth2AuthenticationFailureHandler.class);

    @Value("${app.frontend-url:http://localhost:3000}")
    private String frontendUrl;

    @Override
    public void onAuthenticationFailure(HttpServletRequest request,
                                        HttpServletResponse response,
                                        AuthenticationException exception) throws IOException {
        String message = exception.getMessage();
        if (message == null || message.isBlank()) {
            message = "Google sign-in failed. Please try again.";
        }

        log.error("OAUTH2_FAILURE path={} error={}", request.getRequestURI(), message, exception);

        // Encode the message so it survives as a URL query parameter
        String encoded = URLEncoder.encode(message, StandardCharsets.UTF_8);

        String redirectUrl = UriComponentsBuilder
                .fromUriString(frontendUrl + "/auth")
                .queryParam("error", encoded)
                .build().toUriString();

        getRedirectStrategy().sendRedirect(request, response, redirectUrl);
    }
}
