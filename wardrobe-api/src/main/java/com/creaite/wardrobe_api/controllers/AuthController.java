package com.creaite.wardrobe_api.controllers;

import com.creaite.wardrobe_api.domain.user.User;
import com.creaite.wardrobe_api.dto.LoginRequestDTO;
import com.creaite.wardrobe_api.dto.RegisterRequestDTO;
import com.creaite.wardrobe_api.dto.ResponseDTO;
import com.creaite.wardrobe_api.infra.security.TokenService;
import com.creaite.wardrobe_api.repositories.UserRepository;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.Optional;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {
    // DEPENDÊNCIAS (podia ser autowired tb)
    private final UserRepository repository;
    private final PasswordEncoder passwordEncoder;
    private final TokenService tokenService;

    @PostMapping("/login")
    public ResponseEntity login(@RequestBody LoginRequestDTO body){
        User user = this.repository.findByEmail(body.email()).orElseThrow(() -> new RuntimeException("User not found"));
        if(passwordEncoder.matches(body.password(), user.getPassword())) {
            String token = this.tokenService.generateAccessToken(user);
            user.setLastLogin(LocalDateTime.now());
            this.repository.save(user);
            return ResponseEntity.ok(new ResponseDTO(user.getName(), token)); //retorna as infos que o front precisa (nesse caso, token e name)
        }
        return ResponseEntity.badRequest().build();
        //criar classe controller advice e retornar resposta bonitinha pra tratar exceções
    }

    @PostMapping("/register")
    public ResponseEntity register(@RequestBody @Valid RegisterRequestDTO body){
        Optional<User> user = this.repository.findByEmail(body.email());

        if(user.isEmpty()) {
            User newUser = new User();
            newUser.setPassword(passwordEncoder.encode(body.password()));
            newUser.setEmail(body.email());
            newUser.setUsername(body.username());
            newUser.setName(body.name());
            newUser.setBirthDate(body.birthDate());
            newUser.setLanguage(body.language());
            this.repository.save(newUser);

            String token = this.tokenService.generateAccessToken(newUser);
            return ResponseEntity.ok(new ResponseDTO(newUser.getName(), token));
        }
        return ResponseEntity.badRequest().build();
    }
}


