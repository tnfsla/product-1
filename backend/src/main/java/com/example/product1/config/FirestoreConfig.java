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
        // Authenticate using Application Default Credentials
        // This will automatically pick up credentials from the environment
        // For example, if running on Google Cloud Platform, or if GOOGLE_APPLICATION_CREDENTIALS
        // environment variable is set to a service account key file path.
        GoogleCredentials credentials = GoogleCredentials.getApplicationDefault();
        FirestoreOptions firestoreOptions = FirestoreOptions.newBuilder()
                .setCredentials(credentials)
                .build();
        return firestoreOptions.getService();
    }
}
