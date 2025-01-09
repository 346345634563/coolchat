package com.inf5190.chat;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.concurrent.ExecutionException;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;

import com.inf5190.chat.auth.AuthController;
import com.inf5190.chat.auth.model.LoginRequest;
import com.inf5190.chat.auth.model.LoginResponse;
import com.inf5190.chat.auth.repository.FirestoreUserAccount;
import com.inf5190.chat.auth.repository.UserAccountRepository;
import com.inf5190.chat.auth.session.SessionData;
import com.inf5190.chat.auth.session.SessionManager;

public class TestAuthController {

    private final String username = "username";
    private final String password = "password";
    private final String hashedPassword = "hash";

    private final FirestoreUserAccount userAccount =
            new FirestoreUserAccount(this.username, this.hashedPassword);

    private final LoginRequest loginRequest = new LoginRequest(this.username, this.password);

    @Mock
    private SessionManager mockSessionManager;

    @Mock
    private UserAccountRepository mockAccountRepository;

    @Mock
    private PasswordEncoder mockPasswordEncoder;

    private AuthController authController;

    @BeforeEach
    public void setup() {
        MockitoAnnotations.openMocks(this);
        this.authController = new AuthController(mockSessionManager, mockAccountRepository, mockPasswordEncoder);
    }

    @Test
    public void firstLogin() throws InterruptedException, ExecutionException {

        final SessionData expectedSessionData = new SessionData(this.username);
        final String expectedUsername = this.username;
        final String expectedHashedPassword = this.hashedPassword;
        final String expectedSessionToken = "sessionToken";

        // pas de compte avec ce nom
        when(this.mockAccountRepository.getUserAccount(loginRequest.username())).thenReturn(null);
        when(this.mockPasswordEncoder.encode(loginRequest.password())).thenReturn(expectedHashedPassword);

        // Simule la session
        when(this.mockSessionManager.addSession(expectedSessionData)).thenReturn(expectedSessionToken);

        // login
        ResponseEntity<LoginResponse> response = this.authController.login(loginRequest);

        // verifie
        assertThat(response.getStatusCode().value()).isEqualTo(200);

        verify(this.mockAccountRepository, times(1)).getUserAccount(this.username);
        verify(this.mockPasswordEncoder, times(1)).encode(this.password);
        verify(this.mockSessionManager, times(1)).addSession(expectedSessionData);
    }


    @Test
    public void loginExistingUserAccountWithCorrectPassword()
            throws InterruptedException, ExecutionException {
        final SessionData expectedSessionData = new SessionData(this.username);
        final String expectedUsername = this.username;
        final String expectedSessionToken = "sessionToken";

        when(this.mockAccountRepository.getUserAccount(loginRequest.username()))
                .thenReturn(userAccount);
        when(this.mockPasswordEncoder.matches(loginRequest.password(), this.hashedPassword))
                .thenReturn(true);
        when(this.mockSessionManager.addSession(expectedSessionData)).thenReturn(expectedSessionToken);

        ResponseEntity<LoginResponse> response = this.authController.login(loginRequest);

        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(response.getBody().username()).isEqualTo(expectedUsername);

        verify(this.mockAccountRepository, times(1)).getUserAccount(this.username);
        verify(this.mockPasswordEncoder, times(1)).matches(this.password, this.hashedPassword);
        verify(this.mockSessionManager, times(1)).addSession(expectedSessionData);
    }


    @Test
    public void loginExistingUserAccountWithWrongPassword() throws InterruptedException, ExecutionException {
        final String expectedUsername = this.username;

        // Le compte existe
        when(this.mockAccountRepository.getUserAccount(loginRequest.username())).thenReturn(userAccount);

        when(this.mockPasswordEncoder.matches(loginRequest.password(), this.hashedPassword)).thenReturn(false);

        ResponseEntity<LoginResponse> response = this.authController.login(loginRequest);

        assertThat(response.getStatusCode().value()).isEqualTo(403);

        verify(this.mockAccountRepository, times(1)).getUserAccount(this.username);
        verify(this.mockPasswordEncoder, times(1)).matches(this.password, this.hashedPassword);
        // Pas de session cree
        verify(this.mockSessionManager, times(0)).addSession(any(SessionData.class));
    }

    
    @Test
    public void exceptionWhileInteractingWithDataStore() throws InterruptedException, ExecutionException {
        when(this.mockAccountRepository.getUserAccount(loginRequest.username())).thenThrow(new RuntimeException("Data store exception"));

        ResponseStatusException exception = org.junit.jupiter.api.Assertions.assertThrows(
                ResponseStatusException.class,
                () -> this.authController.login(loginRequest)
        );

        assertThat(exception.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(exception.getReason()).isEqualTo("Unexpected error during login.");

        verify(this.mockAccountRepository, times(1)).getUserAccount(this.username);

        verify(this.mockPasswordEncoder, times(0)).matches(anyString(), anyString());
        verify(this.mockSessionManager, times(0)).addSession(any(SessionData.class));
    }
}
