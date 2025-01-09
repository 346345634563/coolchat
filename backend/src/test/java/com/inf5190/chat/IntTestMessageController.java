package com.inf5190.chat;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.HttpCookie;
import java.util.concurrent.ExecutionException;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.client.TestRestTemplate.HttpClientOption;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.PropertySource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;

import com.google.cloud.Timestamp;
import com.google.cloud.firestore.Firestore;
import com.inf5190.chat.auth.model.LoginRequest;
import com.inf5190.chat.auth.model.LoginResponse;
import com.inf5190.chat.messages.model.Message;
import com.inf5190.chat.messages.model.NewMessageRequest;
import com.inf5190.chat.messages.repository.FirestoreMessage;

@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
@PropertySource("classpath:firebase.properties")
public class IntTestMessageController {
    private final FirestoreMessage message1 = new FirestoreMessage("u1", Timestamp.now(), "t1", null);
    private final FirestoreMessage message2 = new FirestoreMessage("u2", Timestamp.now(), "t2", null);

    @Value("${firebase.project.id}")
    private String firebaseProjectId;

    @Value("${firebase.emulator.port}")
    private String emulatorPort;

    @LocalServerPort
    private int port;

    private TestRestTemplate restTemplate;

    @Autowired
    private Firestore firestore;

    private String messagesEndpointUrl;
    private String loginEndpointUrl;

    @BeforeAll
    public static void checkRunAgainstEmulator() {
        checkEmulators();
    }

    @BeforeEach
    public void setup() throws InterruptedException, ExecutionException {
        this.restTemplate = new TestRestTemplate(HttpClientOption.ENABLE_COOKIES);
        this.messagesEndpointUrl = "http://localhost:" + port + "/messages"; // pour les deux requetes (get et post)
        this.loginEndpointUrl = "http://localhost:" + port + "/auth/login";

        // Pour ajouter deux message dans firestore au début de chaque test.
        this.firestore.collection("messages").document("1").create(this.message1).get();
        this.firestore.collection("messages").document("2").create(this.message2).get();
    }

    @AfterEach
    public void testDown() {
        // Pour effacer le contenu de l'émulateur entre chaque test.
        this.restTemplate.delete("http://localhost:" + this.emulatorPort + "/emulator/v1/projects/"
                + this.firebaseProjectId + "/databases/(default)/documents");
    }

    /**
     * Se connecte et retourne le cookie de session.
     * 
     * @return le cookie de session.
     */
    private String login() {
        ResponseEntity<LoginResponse> response = this.restTemplate.postForEntity(this.loginEndpointUrl,
                new LoginRequest("username", "password"), LoginResponse.class);

        String setCookieHeader = response.getHeaders().getFirst(HttpHeaders.SET_COOKIE);
        HttpCookie sessionCookie = HttpCookie.parse(setCookieHeader).get(0);
        return sessionCookie.getName() + "=" + sessionCookie.getValue();
    }

    private HttpEntity<NewMessageRequest> createRequestEntityWithSessionCookie(
            NewMessageRequest messageRequest, String cookieValue) {
        HttpHeaders header = this.createHeadersWithSessionCookie(cookieValue);
        return new HttpEntity<NewMessageRequest>(messageRequest, header);
    }

    private HttpHeaders createHeadersWithSessionCookie(String cookieValue) {
        HttpHeaders header = new HttpHeaders();
        header.add(HttpHeaders.COOKIE, cookieValue);
        return header;
    }

    private static void checkEmulators() {
        final String firebaseEmulator = System.getenv().get("FIRESTORE_EMULATOR_HOST");
        if (firebaseEmulator == null || firebaseEmulator.length() == 0) {
            System.err.println(
                    "**********************************************************************************************************");
            System.err.println(
                    "******** You need to set FIRESTORE_EMULATOR_HOST=localhost:8181 in your system properties. ********");
            System.err.println(
                    "**********************************************************************************************************");
        }
        assertThat(firebaseEmulator).as(
                "You need to set FIRESTORE_EMULATOR_HOST=localhost:8181 in your system properties.")
                .isNotEmpty();
    }

    // Obtenir les messages sans login
    @Test
    public void getMessageNotLoggedIn() {
        ResponseEntity<String> response = this.restTemplate.getForEntity(this.messagesEndpointUrl, String.class);

        assertThat(response.getStatusCodeValue()).isEqualTo(403);
    }

    // On veut obtenir les deux messages u1 et u2 avec un cookie valide sans FROMIS
    @Test
    public void getMessagesWithoutFromId() {
        final String sessionCookie = this.login();

        final HttpHeaders header = this.createHeadersWithSessionCookie(sessionCookie);
        final HttpEntity<Object> headers = new HttpEntity<Object>(header);
        final ResponseEntity<Message[]> response = this.restTemplate
                .exchange(this.messagesEndpointUrl, HttpMethod.GET, headers, Message[].class);

        Message[] messages = response.getBody();

        assertThat(messages.length == 2);
        assertThat(messages[0].username().equals(message1.getUsername()));
        assertThat(messages[1].username().equals(message2.getUsername()));
    }

    // On doit d'abord envoyer les deux messages, puis recevoir les deux messages
    // et donner le fromId du premier
    @Test
    public void getMessagesWithFromId() {
        final String sessionCookie = this.login();

        final HttpHeaders header = this.createHeadersWithSessionCookie(sessionCookie);
        final HttpEntity<Object> headers = new HttpEntity<>(header);

        final ResponseEntity<Message[]> initialResponse = this.restTemplate
                .exchange(this.messagesEndpointUrl, HttpMethod.GET, headers, Message[].class);

        Message[] messages = initialResponse.getBody();
        assertThat(messages.length).isGreaterThanOrEqualTo(2);

        String fromId = messages[0].id();

        String urlWithFromId = this.messagesEndpointUrl + "?fromId=" + fromId;
        final HttpEntity<Object> filteredHeaders = new HttpEntity<>(header);
        ResponseEntity<Message[]> filteredResponse = this.restTemplate.exchange(urlWithFromId,
                             HttpMethod.GET, filteredHeaders, Message[].class);


        Message[] filteredMessages = filteredResponse.getBody();
        assertThat(filteredMessages.length).isEqualTo(1);
        assertThat(filteredMessages[0].username()).isEqualTo("u2");
    }

    // Obtenir avec plus de 20 messages
    @Test
    public void getMoreThen20() {
        FirestoreMessage fm = new FirestoreMessage();
        for (int i = 0; i < 20; i++) {
            fm = new FirestoreMessage("u" + (i % 2 == 0 ? 1 : 2), Timestamp.now(), "message" + i + 2, null);
            try {
                this.firestore.collection("messages" + i).document("2").create(fm).get();
            } catch (InterruptedException | ExecutionException e) {
                e.printStackTrace();
            }
        }

        final String sessionCookie = this.login();

        final HttpHeaders header = this.createHeadersWithSessionCookie(sessionCookie);
        final HttpEntity<Object> headers = new HttpEntity<Object>(header);
        final ResponseEntity<Message[]> response = this.restTemplate
                .exchange(this.messagesEndpointUrl, HttpMethod.GET, headers, Message[].class);

        Message[] messages = response.getBody();

        // On verifie qu'on ramasse uniquement les 20 derniers messages
        assertThat(messages.length == 20);
        assertThat(messages[messages.length - 1].text().equals(fm.getText()));
    }

    @Test
    public void getMessagesWithInvalidFromId() {
        final String sessionCookie = this.login();

        final HttpHeaders header = this.createHeadersWithSessionCookie(sessionCookie);
        final HttpEntity<Object> headers = new HttpEntity<>(header);

        String fromId = "AAAAAAAAAAAAAAAAAA";

        String urlWithInvalidFromId = this.messagesEndpointUrl + "?fromId=" + fromId;

        ResponseEntity<String> response = this.restTemplate.exchange(urlWithInvalidFromId,HttpMethod.GET,headers,String.class);

        assertThat(response.getStatusCodeValue()).isEqualTo(404);

    }

    @Test
    public void postMessageWithoutToken() {
        NewMessageRequest newMessage = new NewMessageRequest("Message without token", "anonymous", null);

        HttpEntity<NewMessageRequest> request = new HttpEntity<>(newMessage);

        ResponseEntity<String> response = this.restTemplate.exchange(this.messagesEndpointUrl,
                HttpMethod.POST, request, String.class);

        assertThat(response.getStatusCodeValue()).isEqualTo(403);
    }


    @Test
    public void postMessageWithValidToken() {
        final String sessionCookie = this.login();

        NewMessageRequest newMessage = new NewMessageRequest("Hello world!", "jeanpapa1", null);
        HttpEntity<NewMessageRequest> request = this.createRequestEntityWithSessionCookie(newMessage, sessionCookie);

        ResponseEntity<Message> response = this.restTemplate.exchange(this.messagesEndpointUrl,
                HttpMethod.POST, request, Message.class);

        assertThat(response.getStatusCodeValue()).isEqualTo(201);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().text()).isEqualTo("Hello world!");
        assertThat(response.getBody().username()).isEqualTo("jeanpapa1");
    }


}