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

/**
 * Stores the OAuth2 authorization request in a short-lived cookie instead of
 * the HttpSession.
 *
 * WHY THIS IS NEEDED:
 * Spring Security's default InMemoryAuthorizationRequestRepository ties state
 * to the HttpSession. On Railway (and any stateless/multi-instance deployment)
 * the session is gone by the time Google redirects back, causing:
 *   "authorization_request_not_found" → 401 → frontend 404 on /auth?error=...
 *
 * Storing the serialized OAuth2AuthorizationRequest in a cookie solves this
 * because cookies are sent by the browser on every request regardless of which
 * server instance handles it.
 *
 * COOKIE PROPERTIES:
 *   - HttpOnly: not accessible via JS (XSS protection)
 *   - Secure: HTTPS only in production
 *   - SameSite=Lax: sent on top-level navigations (the OAuth2 redirect IS a
 *     top-level navigation, so Lax works and is safer than None)
 *   - Max-Age: 3 minutes — long enough for the OAuth2 round trip, short enough
 *     to not linger
 *   - Path=/: must be accessible on both /oauth2/** and /login/oauth2/**
 */
@Component
public class HttpCookieOAuth2AuthorizationRequestRepository
        implements AuthorizationRequestRepository<OAuth2AuthorizationRequest> {

    private static final Logger log = LoggerFactory.getLogger(HttpCookieOAuth2AuthorizationRequestRepository.class);

    public static final String COOKIE_NAME    = "oauth2_auth_request";
    public static final int    COOKIE_EXPIRY  = 180; // 3 minutes

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
            deleteCookie(request, response, COOKIE_NAME);
            return;
        }
        String value = serialize(authorizationRequest);
        Cookie cookie = new Cookie(COOKIE_NAME, value);
        cookie.setPath("/");
        cookie.setHttpOnly(true);
        cookie.setMaxAge(COOKIE_EXPIRY);
        cookie.setSecure(true); // Railway is always HTTPS

        // SameSite=Lax allows the cookie to be sent on top-level cross-site
        // navigations (the Google → auth-service redirect). Must be set via
        // the Set-Cookie header directly because the Servlet Cookie API has
        // no setSameSite() in Java EE / Jakarta EE 9.
        response.addHeader("Set-Cookie",
                String.format("%s=%s; Path=/; HttpOnly; Secure; SameSite=Lax; Max-Age=%d",
                        COOKIE_NAME, value, COOKIE_EXPIRY));
    }

    // ── Remove ──────────────────────────────────────────────────────────

    @Override
    public OAuth2AuthorizationRequest removeAuthorizationRequest(HttpServletRequest request,
                                                                  HttpServletResponse response) {
        OAuth2AuthorizationRequest existing = loadAuthorizationRequest(request);
        deleteCookie(request, response, COOKIE_NAME);
        return existing;
    }

    // ── Helpers ─────────────────────────────────────────────────────────

    private java.util.Optional<String> readCookie(HttpServletRequest request, String name) {
        Cookie[] cookies = request.getCookies();
        if (cookies == null) return java.util.Optional.empty();
        for (Cookie c : cookies) {
            if (name.equals(c.getName())) {
                return java.util.Optional.of(c.getValue());
            }
        }
        return java.util.Optional.empty();
    }

    private void deleteCookie(HttpServletRequest request, HttpServletResponse response, String name) {
        Cookie[] cookies = request.getCookies();
        if (cookies == null) return;
        for (Cookie c : cookies) {
            if (name.equals(c.getName())) {
                // Expire via header so we can set SameSite consistently
                response.addHeader("Set-Cookie",
                        String.format("%s=; Path=/; HttpOnly; Secure; SameSite=Lax; Max-Age=0", name));
                return;
            }
        }
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
            log.warn("Failed to deserialize OAuth2 authorization request from cookie: {}", e.getMessage());
            return null;
        }
    }
}
