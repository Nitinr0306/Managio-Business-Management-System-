package com.managio.platform.security;

import com.managio.platform.security.Principals.CustomerPrincipal;
import com.managio.platform.security.Principals.UserPrincipal;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.InvalidBearerTokenException;

/** Authentication whose principal is a {@link UserPrincipal} or a {@link CustomerPrincipal}. */
public class PrincipalAuthentication extends AbstractAuthenticationToken {

    public static final String ROLE_USER = "ROLE_USER";
    public static final String ROLE_CUSTOMER = "ROLE_CUSTOMER";

    private final Object principal;
    private final Jwt token;

    private PrincipalAuthentication(Object principal, Jwt token, String authority) {
        super(AuthorityUtils.createAuthorityList(authority));
        this.principal = principal;
        this.token = token;
        setAuthenticated(true);
    }

    @Override
    public Object getPrincipal() {
        return principal;
    }

    @Override
    public Object getCredentials() {
        return token;
    }

    /** Maps a validated access token to the principal type named in its {@code typ} claim. */
    public static class FromJwt implements Converter<Jwt, AbstractAuthenticationToken> {
        @Override
        public AbstractAuthenticationToken convert(Jwt jwt) {
            String type = jwt.getClaimAsString(AccessTokens.TYPE_CLAIM);
            long subject = parseId(jwt.getSubject());
            if (AccessTokens.TYPE_USER.equals(type)) {
                return new PrincipalAuthentication(new UserPrincipal(subject, jwt.getClaimAsString("email")), jwt, ROLE_USER);
            }
            if (AccessTokens.TYPE_CUSTOMER.equals(type)) {
                Object business = jwt.getClaim(AccessTokens.BUSINESS_CLAIM);
                if (!(business instanceof Number businessId)) {
                    throw new InvalidBearerTokenException("Customer token has no business");
                }
                return new PrincipalAuthentication(new CustomerPrincipal(subject, businessId.longValue()), jwt, ROLE_CUSTOMER);
            }
            throw new InvalidBearerTokenException("Unknown token type");
        }

        private static long parseId(String subject) {
            try {
                return Long.parseLong(subject);
            } catch (NumberFormatException e) {
                throw new InvalidBearerTokenException("Invalid token subject");
            }
        }
    }
}
