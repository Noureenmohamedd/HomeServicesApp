package com.example.offerservice.config;

import com.example.offerservice.service.JwtService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtService jwtService;

    public JwtAuthenticationFilter(JwtService jwtService) {
        this.jwtService = jwtService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        System.out.println("JWT FILTER HIT");
        String token = extractBearerToken(request.getHeader("Authorization"));
        System.out.println("TOKEN RECEIVED = " + token);

        if (token != null && SecurityContextHolder.getContext().getAuthentication() == null) {
            var claimsOptional = jwtService.validateToken(token);
            System.out.println("CLAIMS PRESENT = " + claimsOptional.isPresent());

            claimsOptional.ifPresent(claims -> {
                System.out.println("ROLE = " + claims.role());
                System.out.println("PROFESSION TYPE = " + claims.professionType());
                AuthenticatedUser user = new AuthenticatedUser(
                        claims.userId(),
                        claims.username(),
                        claims.role(),
                        claims.professionType()
                );
                String authority = "ROLE_" + claims.role();
                UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                        user,
                        null,
                        List.of(new SimpleGrantedAuthority(authority))
                );
                SecurityContextHolder.getContext().setAuthentication(authentication);
                System.out.println("AUTHENTICATION SET");
            });
        } else if (token == null) {
            System.out.println("CLAIMS PRESENT = false");
        } else {
            System.out.println("AUTHENTICATION ALREADY PRESENT");
        }

        filterChain.doFilter(request, response);
    }

    private String extractBearerToken(String authorizationHeader) {
        if (authorizationHeader == null || authorizationHeader.isBlank()) {
            return null;
        }

        String trimmedHeader = authorizationHeader.trim();
        if (!trimmedHeader.regionMatches(true, 0, "Bearer ", 0, 7)) {
            return null;
        }

        String token = trimmedHeader.substring(7).trim();
        return token.isEmpty() ? null : token;
    }
}
