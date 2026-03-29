package com.ecads.pulse.config;

import com.ecads.pulse.common.exception.FirebaseInitializationFailedException;
import com.google.auth.oauth2.GoogleCredentials;
import com.google.cloud.firestore.Firestore;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.cloud.FirestoreClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.annotation.PostConstruct;
import java.io.InputStream;

@Slf4j
@Configuration
public class FirebaseConfig {

    @PostConstruct
    public void firebaseInit() {
        try (InputStream serviceAccount = getClass().getResourceAsStream("/firebase/pulse-1d704-firebase-adminsdk-fbsvc-c7415dd7f4.json")) {

            if (serviceAccount == null) {
                throw new FirebaseInitializationFailedException("Firebase Initialization JSON not found");
            }

            FirebaseOptions options = FirebaseOptions.builder()
                    .setCredentials(GoogleCredentials.fromStream(serviceAccount))
                    .build();

            if (FirebaseApp.getApps().isEmpty()) {
                FirebaseApp.initializeApp(options);
            }

        } catch (Exception e) {
            log.error("Firebase Initialization Failed: {}", e.getMessage());
            throw new FirebaseInitializationFailedException("Firebase Initialization Failed: " + e.getMessage());
        }
    }

    @Bean
    public Firestore firestore() {
        return FirestoreClient.getFirestore();
    }

}
