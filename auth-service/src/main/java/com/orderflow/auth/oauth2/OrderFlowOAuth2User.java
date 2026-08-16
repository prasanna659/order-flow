package com.orderflow.auth.oauth2;

import com.orderflow.auth.entity.User;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.core.oidc.OidcIdToken;
import org.springframework.security.oauth2.core.oidc.OidcUserInfo;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;

import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * Wraps the raw Google OidcUser with our own User entity so the
 * OAuth2AuthenticationSuccessHandler can access the local userId/username
 * needed to generate a JWT without another DB lookup.
 *
 * Implements OidcUser fully (including getClaims()) so Spring Security 6's
 * OidcAuthorizationCodeAuthenticationProvider can store this as the principal
 * without falling back to DefaultOidcUser.
 */
public class OrderFlowOAuth2User implements OidcUser {

    private final OidcUser delegate;
    private final User localUser;

    public OrderFlowOAuth2User(OidcUser delegate, User localUser) {
        this.delegate  = delegate;
        this.localUser = localUser;
    }

    // -----------------------------------------------------------------------
    // Application-level accessor
    // -----------------------------------------------------------------------

    public User getLocalUser() {
        return localUser;
    }

    // -----------------------------------------------------------------------
    // OidcUser — required by Spring Security 6's OidcAuthorizationCodeAuthenticationProvider
    // -----------------------------------------------------------------------

    /**
     * Returns the ID-token claims map. Spring Security 6 calls this internally;
     * omitting it causes a fallback to DefaultOidcUser in some code paths.
     */
    @Override
    public Map<String, Object> getClaims() {
        return delegate.getClaims();
    }

    @Override
    public OidcIdToken getIdToken() {
        return delegate.getIdToken();
    }

    @Override
    public OidcUserInfo getUserInfo() {
        return delegate.getUserInfo();
    }

    // -----------------------------------------------------------------------
    // OAuth2User
    // -----------------------------------------------------------------------

    @Override
    public Map<String, Object> getAttributes() {
        return delegate.getAttributes();
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority("ROLE_USER"));
    }

    /** Returns our local username, not the Google subject. */
    @Override
    public String getName() {
        return localUser.getUsername();
    }
}
