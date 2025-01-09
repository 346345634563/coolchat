package com.inf5190.chat.messages;

import java.util.ArrayList;
import java.util.List;
import java.util.HashMap;
import java.util.Map;

import com.inf5190.chat.messages.model.NewMessageRequest;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import com.inf5190.chat.messages.model.Message;
import com.inf5190.chat.messages.repository.MessageRepository;
import com.inf5190.chat.websocket.WebSocketManager;
import com.inf5190.chat.auth.session.SessionManager;

/**
 * Contrôleur qui gère l'API de messages.
 */
@RestController
public class MessageController {

    public static final String MESSAGES_PATH = "/messages";

    private MessageRepository messageRepository;
    private WebSocketManager webSocketManager;
    private final SessionManager sessionManager;

    public MessageController(MessageRepository messageRepository,
            WebSocketManager webSocketManager, SessionManager sessionManager) {
        this.messageRepository = messageRepository;
        this.webSocketManager = webSocketManager;
        this.sessionManager = sessionManager;
    }

    @GetMapping("/messages")
    public ResponseEntity<List<Message>> receiveMessages(@RequestParam(value = "fromId", defaultValue = "") String fromId) {
        try {
            List<Message> messageReturned = messageRepository.getMessages(fromId);
            return ResponseEntity.ok(messageReturned);
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Invalid fromId", e);
        }
    }

    // route post /messages
    // On doit maintenant comparer le nom d'utilisateur du message et le
    @PostMapping("/messages")
    public ResponseEntity<Map<String, String>> sendMessages(@CookieValue("sid") String jwtToken,
            @RequestBody NewMessageRequest m) {


        String decryptedUsername = sessionManager.getSession(jwtToken).username();

        Map<String, String> response = new HashMap<>();

        if (decryptedUsername.equals(m.username())) {

            messageRepository.createMessage(m);
            webSocketManager.notifySessions();

            response.put("message", "Message Saved.");

            return ResponseEntity.ok(response);
        } else {
            response.put("message", "Claimed id and token id are different.");
            return ResponseEntity.status(403).body(response);
        }
    }

}
