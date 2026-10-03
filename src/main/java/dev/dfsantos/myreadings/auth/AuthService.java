package dev.dfsantos.myreadings.auth;

import dev.dfsantos.myreadings.auth.dto.LoginRequest;
import dev.dfsantos.myreadings.auth.dto.RegisterRequest;
import dev.dfsantos.myreadings.auth.dto.RegisterResponse;
import dev.dfsantos.myreadings.auth.dto.TokenResponse;
import dev.dfsantos.myreadings.user.User;
import dev.dfsantos.myreadings.user.UserRepository;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.UUID;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;

    public AuthService(UserRepository userRepository, PasswordEncoder passwordEncoder,
                        JwtTokenProvider jwtTokenProvider) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtTokenProvider = jwtTokenProvider;
    }

    public RegisterResponse register(RegisterRequest request) {
        // Checagem explícita de unicidade (caminho comum). A constraint UNIQUE da
        // coluna email no banco continua sendo o backstop contra condição de corrida
        // entre esta checagem e o INSERT; se ela for violada, DataIntegrityViolationException
        // é traduzida para 409 pelo GlobalExceptionHandler da mesma forma.
        userRepository.findByEmail(request.email()).ifPresent(existing -> {
            throw new EmailAlreadyInUseException(request.email());
        });

        User user = new User(
                UUID.randomUUID(),
                request.email(),
                passwordEncoder.encode(request.password()),
                Instant.now());

        User saved = userRepository.save(user);

        return new RegisterResponse(saved.getId(), saved.getEmail());
    }

    public TokenResponse login(LoginRequest request) {
        // Mesma exceção e mensagem para e-mail inexistente e senha incorreta, para não
        // revelar a um atacante qual das duas informações está errada.
        User user = userRepository.findByEmail(request.email())
                .orElseThrow(() -> new BadCredentialsException("E-mail ou senha inválidos"));

        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw new BadCredentialsException("E-mail ou senha inválidos");
        }

        String accessToken = jwtTokenProvider.generateToken(user.getId());
        return new TokenResponse(accessToken, jwtTokenProvider.getExpirationSeconds());
    }
}
