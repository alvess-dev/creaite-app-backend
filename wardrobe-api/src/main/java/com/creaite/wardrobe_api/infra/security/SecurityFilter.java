package com.creaite.wardrobe_api.infra.security;

import com.creaite.wardrobe_api.domain.user.User;
import com.creaite.wardrobe_api.repositories.UserRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;

@Component
public class SecurityFilter extends OncePerRequestFilter {

    @Autowired
    TokenService tokenService;

    @Autowired
    UserRepository userRepository;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        String requestPath = request.getRequestURI();
        String method = request.getMethod();

        System.out.println("=== SecurityFilter ===");
        System.out.println("Path: " + requestPath);
        System.out.println("Method: " + method);

        // Permitir OPTIONS para CORS
        if ("OPTIONS".equalsIgnoreCase(method)) {
            System.out.println("OPTIONS request - bypassing filter");
            filterChain.doFilter(request, response);
            return;
        }

        // Verificar se é um endpoint de autenticação (público)
        if (requestPath.startsWith("/auth/")) {
            System.out.println("Auth endpoint - bypassing authentication");
            filterChain.doFilter(request, response);
            return;
        }

        // Verificar se é OAuth2 (público)
        if (requestPath.startsWith("/oauth2/") || requestPath.startsWith("/login/oauth2/")) {
            System.out.println("OAuth2 endpoint - bypassing authentication");
            filterChain.doFilter(request, response);
            return;
        }

        // Para endpoints protegidos, validar token
        var token = this.recoverToken(request);
        System.out.println("Token present: " + (token != null));

        if (token != null) {
            var login = tokenService.validateAccessToken(token);
            System.out.println("Token validated for user: " + login);

            if(login != null) {
                User user = userRepository.findByEmail(login)
                        .orElseThrow(() -> new RuntimeException("User Not Found"));
                var authorities = Collections.singletonList(new SimpleGrantedAuthority("ROLE_USER"));
                var authentication = new UsernamePasswordAuthenticationToken(user, null, authorities);
                SecurityContextHolder.getContext().setAuthentication(authentication);
                System.out.println("User authenticated: " + user.getEmail());
            } else {
                System.out.println("Invalid token");
            }
        } else {
            System.out.println("No token provided for protected endpoint");
        }

        filterChain.doFilter(request, response);
    }

    private String recoverToken(HttpServletRequest request){
        var authHeader = request.getHeader("Authorization");
        if(authHeader == null) return null;
        return authHeader.replace("Bearer ", "");
    }
}