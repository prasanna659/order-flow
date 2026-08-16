package com.orderflow.auth.oauth2;

import com.orderflow.auth.entity.User;
import com.orderflow.auth.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserRequest;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserService;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

/**
 * Loads and provisions a local User after Google OAuth2 (OIDC) succeeds.
 *
 * This extends OidcUserService instead of DefaultOAuth2UserService because
 * Google uses OpenID Connect, which returns OidcUser instances.
 *
 * All exceptions are caught and re-thrown as OAuth2AuthenticationException so
 * Spring Security's OAuth2 error path handles them cleanly (redirecting to the
 * failure handler) instead of falling through to the Whitelabel error page.
 */
@Service
public class GoogleOidcUserService extends OidcUserService {

    private static final Logger log = LoggerFactory.getLogger(GoogleOidcUserService.class);

    private final UserRepository userRepository;

    public GoogleOidcUserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    @Transactional
    public OrderFlowOAuth2User loadUser(OidcUserRequest userRequest) throws OAuth2AuthenticationException {
        OidcUser oidcUser;
        try {
            oidcUser = super.loadUser(userRequest);
        } catch (OAuth2AuthenticationException e) {
            throw e;
        } catch (Exception e) {
            log.error("OAUTH2_FETCH_USER_FAILED error={}", e.getMessage(), e);
            throw new OAuth2AuthenticationException(
                    new OAuth2Error("fetch_user_failed", "Failed to fetch user info from Google: " + e.getMessage(), null));
        }

        // Extract attributes from OIDC user
        String googleId = oidcUser.getSubject(); // 'sub' claim
        String email = oidcUser.getEmail();
        String name = oidcUser.getFullName();
        String picture = oidcUser.getPicture();

        // Guard: Google should always return sub and email, but be defensive
        if (googleId == null || googleId.isBlank()) {
            throw new OAuth2AuthenticationException(
                    new OAuth2Error("missing_google_id", "Google did not return a user ID (sub)", null));
        }
        if (email == null || email.isBlank()) {
            throw new OAuth2AuthenticationException(
                    new OAuth2Error("missing_email", "Google did not return an email address. Check your OAuth2 scope includes 'email'.", null));
        }

        log.info("OAUTH2_GOOGLE_USER googleId={} email={}", googleId, email);

        try {
            User user = findOrCreateUser(googleId, email, name, picture);
            log.info("OAUTH2_USER_RESOLVED userId={} username={} provider={}",
                    user.getId(), user.getUsername(), user.getProvider());
            return new OrderFlowOAuth2User(oidcUser, user);
        } catch (Exception e) {
            log.error("OAUTH2_USER_PROVISION_FAILED googleId={} email={} error={}", googleId, email, e.getMessage(), e);
            throw new OAuth2AuthenticationException(
                    new OAuth2Error("user_provision_failed",
                            "Failed to create or load user account: " + e.getMessage(), null));
        }
    }

    private User findOrCreateUser(String googleId, String email, String name, String picture) {
        // 1. Returning user — look up by Google ID
        Optional<User> byGoogleId = userRepository.findByGoogleId(googleId);
        if (byGoogleId.isPresent()) {
            User user = byGoogleId.get();
            user.setName(name);
            user.setPictureUrl(picture);
            return userRepository.save(user);
        }

        // 2. Existing local account with same email — link Google to it
        Optional<User> byEmail = userRepository.findByEmail(email);
        if (byEmail.isPresent()) {
            User user = byEmail.get();
            log.info("OAUTH2_LINKING_GOOGLE_TO_EXISTING userId={} email={}", user.getId(), email);
            user.setGoogleId(googleId);
            user.setName(name);
            user.setPictureUrl(picture);
            user.setProvider("google");
            return userRepository.save(user);
        }

        // 3. Brand-new user
        User user = new User();
        user.setGoogleId(googleId);
        user.setEmail(email);
        user.setName(name != null ? name : email.split("@")[0]);
        user.setPictureUrl(picture);
        user.setProvider("google");
        // Blank password — Google users never use password login
        user.setPassword("");
        user.setUsername(deriveUsername(email));

        User saved = userRepository.save(user);
        log.info("OAUTH2_NEW_USER_CREATED userId={} username={}", saved.getId(), saved.getUsername());
        return saved;
    }

    private String deriveUsername(String email) {
        String base = email.split("@")[0]
                .replaceAll("[^a-zA-Z0-9_]", "_")
                .toLowerCase();

        if (base.isBlank()) base = "user";

        if (!userRepository.existsByUsername(base)) {
            return base;
        }
        return base + "_" + UUID.randomUUID().toString().replace("-", "").substring(0, 6);
    }
}
