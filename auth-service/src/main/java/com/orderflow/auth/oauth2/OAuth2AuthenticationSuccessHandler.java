package com.orderflow.auth.oauth2;

import com.orderflow.auth.entity.User;
import com.orderflow.auth.security.JwtUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;

/**
 * Called by Spring Security after a successful Google OAuth2 login.
 *
 * Generates the same JWT the existing login/register endpoints produce,
 * then redirects to the frontend with token + user info in query params.
 *
 * The frontend's /auth/callback route picks these up, stores them in
 * localStorage, and behaves identically to a normal login response.
 *
 * Redirect URL: http://localhost:3000/auth/callback?token=...&username=...&userId=...
 */
@Component
public class OAuth2AuthenticationSuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

    private static final Logger log = LoggerFactory.getLogger(OAuth2AuthenticationSuccessHandler.class);

    private final JwtUtil jwtUtil;

    @Value("${app.frontend-url:http://localhost:3000}")
    private String frontendUrl;

    public OAuth2AuthenticationSuccessHandler(JwtUtil jwtUtil) {
        this.jwtUtil = jwtUtil;
    }

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request,
                                        HttpServletResponse response,
                                        Authentication authentication) throws IOException {
        Object principal = authentication.getPrincipal();

        // Primary path: our custom wrapper populated by GoogleOidcUserService
        if (principal instanceof OrderFlowOAuth2User oAuth2User) {
            User user = oAuth2User.getLocalUser();
            redirectWithToken(request, response, user);
            return;
        }

        // Fallback: Spring returned a raw OidcUser — this should not happen in
        // normal operation, but guards against future Spring Security changes or
        // misconfiguration where GoogleOidcUserService is bypassed.
        if (principal instanceof OidcUser) {
            log.warn("OAUTH2_RAW_OIDC_USER principal was DefaultOidcUser, not OrderFlowOAuth2User — "
                    + "GoogleOidcUserService may not have been invoked. Check oidcUserService() wiring.");
            String redirectUrl = UriComponentsBuilder
                    .fromUriString(frontendUrl + "/auth")
                    .queryParam("error", "Authentication+configuration+error.+Please+contact+support.")
                    .build().toUriString();
            getRedirectStrategy().sendRedirect(request, response, redirectUrl);
            return;
        }

        log.error("OAUTH2_UNKNOWN_PRINCIPAL type={}", principal == null ? "null" : principal.getClass().getName());
        getRedirectStrategy().sendRedirect(request, response, frontendUrl + "/auth?error=Unexpected+authentication+error.");
    }

    private void redirectWithToken(HttpServletRequest request,
                                   HttpServletResponse response,
                                   User user) throws IOException {
        String token = jwtUtil.generateToken(user.getId(), user.getUsername());

        log.info("OAUTH2_SUCCESS userId={} username={} redirecting to frontend", user.getId(), user.getUsername());

        String redirectUrl = UriComponentsBuilder
                .fromUriString(frontendUrl + "/auth/callback")
                .queryParam("token",    token)
                .queryParam("username", user.getUsername())
                .queryParam("userId",   user.getId())
                .queryParam("email",    user.getEmail() != null ? user.getEmail() : "")
                .queryParam("picture",  user.getPictureUrl() != null ? user.getPictureUrl() : "")
                .build().toUriString();

        getRedirectStrategy().sendRedirect(request, response, redirectUrl);
    }
}
