package com.inf5190.chat;

import java.io.IOException;
import java.io.InputStream;
import java.io.FileNotFoundException;
import java.util.Arrays;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.DependsOn;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.PropertySource;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.cloud.firestore.Firestore;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.cloud.FirestoreClient;
import com.google.firebase.cloud.StorageClient;
import com.inf5190.chat.auth.AuthController;
import com.inf5190.chat.auth.filter.AuthFilter;
import com.inf5190.chat.auth.session.SessionManager;
import com.inf5190.chat.messages.MessageController;

@SpringBootApplication
@PropertySource("classpath:cors.properties")
@PropertySource("classpath:firebase.properties")
public class ChatApplication {

    private static final Logger LOGGER = LoggerFactory.getLogger(ChatApplication.class);

    @Value("${cors.allowedOrigins}")
    private String allowedOriginsConfig;

    @Value("${firebase.bucket.name}")
    private String firebaseBucketName;

    @Bean
    public FirebaseApp firebaseApp() {
        try {
            if (FirebaseApp.getApps().isEmpty()) {
                InputStream serviceAccount = getClass().getClassLoader()
                        .getResourceAsStream("firebase-key.json");
                if (serviceAccount == null) {
                    throw new FileNotFoundException("Service account key file not found in classpath");
                }
                FirebaseOptions options = FirebaseOptions.builder()
                        .setCredentials(GoogleCredentials.fromStream(serviceAccount))
                        .setStorageBucket(getStorageBucketName())
                        .build();
                LOGGER.info("Initializing Firebase application.");
                return FirebaseApp.initializeApp(options);
            } else {
                LOGGER.info("Firebase application already initialized.");
                return FirebaseApp.getInstance();
            }
        } catch (IOException e) {
            LOGGER.error("Could not initialize Firebase", e);
            throw new IllegalStateException("Firebase initialization failed", e);
        }
    }

    @Bean
    @DependsOn("firebaseApp")
    public Firestore getFirestore() {
        return FirestoreClient.getFirestore();
    }

    @Bean
    @DependsOn("firebaseApp")
    public StorageClient getCloudStorage() {
        return StorageClient.getInstance();
    }

    @Bean("allowedOrigins")
    public String[] getAllowedOrigins() {
        return Optional.ofNullable(System.getenv("ALLOWED_ORIGINS"))
                .orElse(allowedOriginsConfig)
                .split(",");
    }

    @Bean("firebaseBucketName")
    public String getStorageBucketName() {
        return Optional.ofNullable(System.getenv("STORAGE_BUCKET_NAME"))
                .orElse(firebaseBucketName);
    }

    public static void main(String[] args) {
        SpringApplication.run(ChatApplication.class, args);
    }

    /**
     * Function that registers the authorization filter.
     */
    @Bean
    public FilterRegistrationBean<AuthFilter> authenticationFilter(SessionManager sessionManager) {
        FilterRegistrationBean<AuthFilter> registrationBean = new FilterRegistrationBean<>();

        registrationBean.setFilter(new AuthFilter(sessionManager, Arrays.asList(getAllowedOrigins())));
        registrationBean.addUrlPatterns(MessageController.MESSAGES_PATH, AuthController.AUTH_LOGOUT_PATH);

        return registrationBean;
    }

    @Bean
    public PasswordEncoder getPasswordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
