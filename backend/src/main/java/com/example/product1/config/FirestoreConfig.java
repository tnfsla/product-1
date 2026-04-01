package com.example.product1.config;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.cloud.firestore.Firestore;
import com.google.cloud.firestore.FirestoreOptions;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.IOException;

@Configuration
public class FirestoreConfig {

    @Bean
    public Firestore firestore() throws IOException {
        String emulatorHost = System.getenv("FIRESTORE_EMULATOR_HOST");
        FirestoreOptions.Builder builder = FirestoreOptions.newBuilder()
                .setProjectId("firebase-485608")
                .setDatabaseId("productdb");
        if (emulatorHost == null || emulatorHost.isEmpty()) {
            builder.setCredentials(GoogleCredentials.getApplicationDefault());
        }
        return builder.build().getService();
    }
}
