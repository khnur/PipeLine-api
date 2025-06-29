package kz.nu.pipeline.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import kz.nu.pipeline.model.User;
import org.springframework.stereotype.Component;

import java.security.Key;
import java.util.Date;
import java.util.function.Function;

@Component
public class JwtUtil {
    private static final long JWT_TOKEN_VALIDITY = 24 * 60 * 60 * 1000;

    private final Key key = Keys.secretKeyFor(SignatureAlgorithm.HS512);

    /**
     * Generate a JWT token for a user.
     *
     * @param user the user
     * @return the JWT token
     */
    public String generateToken(User user) {
        return Jwts.builder()
                .setSubject(user.getUsername())
                .setIssuedAt(new Date(System.currentTimeMillis()))
                .setExpiration(new Date(System.currentTimeMillis() + JWT_TOKEN_VALIDITY))
                .signWith(key)
                .compact();
    }

    /**
     * Validate a JWT token.
     *
     * @param token the token to validate
     * @return true if the token is valid, false otherwise
     */
    public Boolean validateToken(String token) {
        final Date expiration = this.getClaimFromToken(token, Claims::getExpiration);
        return expiration.after(new Date());
    }

    /**
     * Get the email from a JWT token.
     *
     * @param token the token
     * @return the email
     */
    public String getUsernameFromToken(String token) {
        return this.getClaimFromToken(token, Claims::getSubject);
    }

    /**
     * Get a claim from a JWT token.
     *
     * @param token          the token
     * @param claimsResolver the claims resolver
     * @param <T>            the type of the claim
     * @return the claim
     */
    public <T> T getClaimFromToken(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = Jwts.parserBuilder()
                .setSigningKey(key)
                .build()
                .parseClaimsJws(token)
                .getBody();
        return claimsResolver.apply(claims);
    }

}