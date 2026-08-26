package com.orderflow.auth.oauth2;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.oauth2.client.web.AuthorizationRequestRepository;
import org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationRequest;
import org.springframework.stereotype.Component;
import org.springframework.util.SerializationUtils;

import java.util.Base64;
import java.util.Optional;

/**
 * Stores the OAuth2 authorization request in a short-lived cookie instead of
 * the HttpSession.
 *
 * WHY THIS IS NEEDED:
 * Spring Security's default InMemoryAuthorizationRequestRepository ties state
 * to the HttpSession. On Railway the session is gone by the time Google redirects
 * back (stateless deployment / different instance), causing:
 *   [authorization_request_not_found]
 *
 * Storing the serialized OAuth2AuthorizationRequest in a cookie solves this
 * because the browser sends the cookie on every request regardless of instance.
 *
 * WHY SameSite=None; Secure (not Lax):
 * The Google → auth-service callback is a cross-site navigation.
 * SameSite=Lax should theoretically send the cookie on top-level GET redirects,
 * but behind Railway's reverse proxy the browser sometimes classifies it as
 * a cross-site context and drops Lax cookies. SameSite=None; Secure is the
 * only setting that guarantees the cookie survives the cross-site redirect in
 * all browsers and proxy configurations.
 *
 * COOKIE PROPERTIES:
 *   - HttpOnly      — no JS access (XSS protection)
 *   - Secure        — HTTPS only (required by SameSite=None)
 *   - SameSite=None — sent on all cross-site requests
 *   - Max-Age=180   — 3 minutes: enough for the OAuth2 round trip, then auto-expires
 *   - Path=/        — accessible on both /oauth2/** and /login/oauth2/**
 */
@Component
public class HttpCookieOAuth2AuthorizationRequestRepository
        implements AuthorizationRequestRepository<OAuth2AuthorizationRequest> {

    private static final Logger log = LoggerFactory.getLogger(HttpCookieOAuth2AuthorizationRequestRepository.class);

    public static final String COOKIE_NAME   = "oauth2_auth_request";
    public static final int    COOKIE_EXPIRY = 180; // seconds

    // ── Read ────────────────────────────────────────────────────────────

    @Override
    public OAuth2AuthorizationRequest loadAuthorizationRequest(HttpServletRequest request) {
        return readCookie(request, COOKIE_NAME)
                .map(this::deserialize)
                .orElse(null);
    }

    // ── Save ────────────────────────────────────────────────────────────

    @Override
    public void saveAuthorizationRequest(OAuth2AuthorizationRequest authorizationRequest,
                                         HttpServletRequest request,
                                         HttpServletResponse response) {
        if (authorizationRequest == null) {
            deleteCookie(response, COOKIE_NAME);
            return;
        }
        String value = serialize(authorizationRequest);
        // Must use addHeader directly — Jakarta Servlet Cookie API has no setSameSite().
        // SameSite=None requires Secure; both are needed for cross-site Railway deployment.
        response.addHeader("Set-Cookie",
                String.format("%s=%s; Path=/; HttpOnly; Secure; SameSite=None; Max-Age=%d",
                        COOKIE_NAME, value, COOKIE_EXPIRY));
        log.debug("OAUTH2_COOKIE_SAVED state={}", authorizationRequest.getState());
    }

    // ── Remove ──────────────────────────────────────────────────────────

    @Override
    public OAuth2AuthorizationRequest removeAuthorizationRequest(HttpServletRequest request,
                                                                  HttpServletResponse response) {
        OAuth2AuthorizationRequest existing = loadAuthorizationRequest(request);
        if (existing != null) {
            deleteCookie(response, COOKIE_NAME);
            log.debug("OAUTH2_COOKIE_REMOVED state={}", existing.getState());
        } else {
            log.warn("OAUTH2_COOKIE_NOT_FOUND — cookie was missing on callback. " +
                    "Check that Set-Cookie was sent on the /oauth2/authorization request " +
                    "and that the browser is sending it back on /login/oauth2/code/google.");
        }
        return existing;
    }

    // ── Helpers ─────────────────────────────────────────────────────────

    private Optional<String> readCookie(HttpServletRequest request, String name) {
        Cookie[] cookies = request.getCookies();
        if (cookies == null) {
            log.debug("OAUTH2_COOKIE_READ no cookies present on request to {}", request.getRequestURI());
            return Optional.empty();
        }
        for (Cookie c : cookies) {
            if (name.equals(c.getName())) {
                return Optional.of(c.getValue());
            }
        }
        return Optional.empty();
    }

    private void deleteCookie(HttpServletResponse response, String name) {
        response.addHeader("Set-Cookie",
                String.format("%s=; Path=/; HttpOnly; Secure; SameSite=None; Max-Age=0", name));
    }

    private String serialize(OAuth2AuthorizationRequest request) {
        return Base64.getUrlEncoder().encodeToString(SerializationUtils.serialize(request));
    }

    @SuppressWarnings("unchecked")
    private OAuth2AuthorizationRequest deserialize(String value) {
        try {
            byte[] bytes = Base64.getUrlDecoder().decode(value);
            return (OAuth2AuthorizationRequest) SerializationUtils.deserialize(bytes);
        } catch (Exception e) {
            log.warn("OAUTH2_COOKIE_DESERIALIZE_FAILED: {}", e.getMessage());
            return null;
        }
    }
}
