package net.hirana.push;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import net.hirana.services.ConnectionsService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;

public enum FCMInitializer {
    INSTANCE;

    private static final Logger log = LoggerFactory.getLogger(ConnectionsService.class);

    public void init(String fileCredential) {
        if (FirebaseApp.getApps().isEmpty()) {
            try {
                InputStream is = getClass().getResourceAsStream(fileCredential);
                FirebaseApp.initializeApp(
                        FirebaseOptions.builder()
                                .setCredentials(
                                        GoogleCredentials.fromStream(
                                                is
                                        )
                                )
                                .build()
                );
                log.info("Firebase application has been initialized");
            } catch (FileNotFoundException e) {
                log.error("Cant initialize firebase, Credentials not found", e);
            } catch (IOException e) {
                log.error("Failed to process input stream", e);
            }
        }
    }
}
