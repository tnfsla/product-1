package com.example.product1.service;

import com.example.product1.model.User;
import com.google.api.core.ApiFuture;
import com.google.cloud.firestore.DocumentReference;
import com.google.cloud.firestore.DocumentSnapshot;
import com.google.cloud.firestore.Firestore;
import com.google.cloud.firestore.WriteResult;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.concurrent.ExecutionException;

@Service
public class UserService {

    private static final String COLLECTION_NAME = "users";

    @Autowired
    private Firestore firestore;

    @Autowired
    private PasswordEncoder passwordEncoder;

    public User saveUser(User user) throws ExecutionException, InterruptedException {
        // Hash the password before saving
        user.setPassword(passwordEncoder.encode(user.getPassword()));
        ApiFuture<WriteResult> collectionsApiFuture = firestore.collection(COLLECTION_NAME).document(user.getUsername()).set(user);
        collectionsApiFuture.get(); // Wait for the operation to complete
        return user;
    }

    public User getUserByUsername(String username) throws ExecutionException, InterruptedException {
        DocumentReference documentReference = firestore.collection(COLLECTION_NAME).document(username);
        ApiFuture<DocumentSnapshot> future = documentReference.get();
        DocumentSnapshot document = future.get();
        if (document.exists()) {
            return document.toObject(User.class);
        }
        return null;
    }

    public boolean verifyPassword(String rawPassword, String encodedPassword) {
        return passwordEncoder.matches(rawPassword, encodedPassword);
    }

    // Temporary main method to test BCrypt hash compatibility - REMOVE AFTER USE
    public static void main(String[] args) {
        BCryptPasswordEncoder tempPasswordEncoder = new BCryptPasswordEncoder();
        String rawPassword = "1234";
        String providedHash = "$2a$10$9421OM.DtpucZSse//FWaOubd2Ps66JHAWRKeR/Rj4100FLuacdQG"; // Hash from online generator
        boolean matches = tempPasswordEncoder.matches(rawPassword, providedHash);
        System.out.println("------------------------------------------");
        System.out.println("Does '1234' match the provided hash? " + matches);
        System.out.println("------------------------------------------");
        System.exit(0); // Exit after testing
    }
}
