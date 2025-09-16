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
public class SecurityFilter extends OncePerRequestFilter { // filtro que executa a cada request
    @Autowired
    TokenService tokenService;
    @Autowired
    UserRepository userRepository;

    // verifica se o token é válido
    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        var token = this.recoverToken(request);
        var login = tokenService.validateAccessToken(token);

        if(login != null){ // se o token passa
            User user = userRepository.findByEmail(login).orElseThrow(() -> new RuntimeException("User Not Found")); // trata se user n tver email
            var authorities = Collections.singletonList(new SimpleGrantedAuthority("ROLE_USER"));
            var authentication = new UsernamePasswordAuthenticationToken(user, null, authorities);
            SecurityContextHolder.getContext().setAuthentication(authentication);
        }
        filterChain.doFilter(request, response);
    }

    // recebe request e ve o header authorization
    private String recoverToken(HttpServletRequest request){
        var authHeader = request.getHeader("Authorization"); // PEGA O HEADER DO FRONT
        if(authHeader == null) return null;
        return authHeader.replace("Bearer ", ""); // FICA SÓ O VALOR DO TOKEN
    }
}
