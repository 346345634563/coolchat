package com.inf5190.chat.auth;

import com.inf5190.chat.auth.repository.UserAccountRepository;
import com.inf5190.chat.auth.repository.FirestoreUserAccount;

import java.sql.Time;
import java.util.concurrent.ExecutionException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Lazy;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import com.inf5190.chat.auth.model.LoginRequest;
import com.inf5190.chat.auth.model.LoginResponse;
import com.inf5190.chat.auth.session.SessionData;
import com.inf5190.chat.auth.session.SessionManager;
import jakarta.servlet.http.Cookie;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;

/**
 * Contrôleur qui gère l'API de login et logout.
 */
@RestController()
public class AuthController {

    public static final String AUTH_LOGIN_PATH = "/auth/login";
    public static final String AUTH_LOGOUT_PATH = "/auth/logout";
    public static final String SESSION_ID_COOKIE_NAME = "sid";

    private static final Logger LOGGER = LoggerFactory.getLogger(AuthController.class);

    private final UserAccountRepository userAccountRepository; 
    private final SessionManager sessionManager;
    private final PasswordEncoder passwordEncoder;

    @Lazy
    public AuthController(SessionManager sessionManager, UserAccountRepository userAccountRepository, PasswordEncoder passwordEncoder ) {
        this.sessionManager = sessionManager;
        this.userAccountRepository = userAccountRepository;
        this.passwordEncoder = passwordEncoder;
    }


    // retourner un cookie d'identification
    @PostMapping(AUTH_LOGIN_PATH)
    public ResponseEntity<LoginResponse> login(@RequestBody LoginRequest loginRequest) throws InterruptedException, ExecutionException {
        try {
            LoginResponse loginResponse;

            // on va faire un appel a la base de donnes pour verifier que les informations concorde (MOT DE PASSE NON ENCRYPTER)
            FirestoreUserAccount account = userAccountRepository.getUserAccount(loginRequest.username());
        
            if(account == null){            
                String encodedPass = passwordEncoder.encode(loginRequest.password());
                userAccountRepository.createUserAccount(new FirestoreUserAccount(loginRequest.username(), encodedPass));
            } else {
                // On compare les mots de passe et prends en compte la possibilite du mauvais mot de passe
                if(!passwordEncoder.matches(loginRequest.password(), account.getEncodedPassword())){
                    
                    // TEMPORAIRE A CHANGER AU TP4 
                    loginResponse = new LoginResponse("Invalid password.");
                    return ResponseEntity.status(403).body(loginResponse);
                }
            }

            SessionData currentSession = new SessionData(loginRequest.username());
        
            // Obligatoire pour avoir un session id
            String content = sessionManager.addSession(currentSession);

            // Creation de cookie JWT 
            ResponseCookie sessionCookie = ResponseCookie.from(SESSION_ID_COOKIE_NAME, content)
                .httpOnly(true)
                .secure(true) 
                .sameSite("None")            
                .path("/")
                .maxAge(24 * 3600) 
                .build();
            
            // On recois le resultat de la requete (LoginRequest)
            loginResponse = new LoginResponse(loginRequest.username());
            
            // Retourne HTTP 200 OK avec le corps (loginResponse) et le cookie (sessionCookie)
            return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, sessionCookie.toString())  
                .body(loginResponse);

        } catch (ResponseStatusException e) {
            throw e;
        } catch (Exception e) {
            LOGGER.warn("Erreur inattendue.", e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Unexpected error during login.");
        }
    }
    

    // On doit enlever les cookies presents
    @PostMapping(AUTH_LOGOUT_PATH)
    public ResponseEntity<Void> logout(@CookieValue(SESSION_ID_COOKIE_NAME) Cookie sessionCookie) {
        try {
            String jwtToken = sessionCookie.getValue();

            // On recupere le sid et on le retire du session manager
            // sessionManager.removeSession(sessionId);

            ResponseCookie sessionCookieRmv = ResponseCookie.from(SESSION_ID_COOKIE_NAME, jwtToken)
                .httpOnly(true)
                .secure(true) 
                .sameSite("None")            
                .path("/")
                .maxAge(0) 
                .build();

            return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, sessionCookieRmv.toString())
                .build();
            
        } catch (ResponseStatusException e) {
            throw e;
        } catch (Exception e) {
            LOGGER.warn("Erreur inattendue.", e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Unexpected error during logout.");
        }
    }

}
