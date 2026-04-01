package com.example.product1.service;

import com.example.product1.model.DrawingResult;
import com.google.api.core.ApiFuture;
import com.google.cloud.firestore.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

@Service
public class DrawingService {

    private static final String COLLECTION = "drawings";

    @Autowired
    private Firestore firestore;

    public DrawingResult save(DrawingResult result) throws ExecutionException, InterruptedException {
        result.setId(UUID.randomUUID().toString());
        result.setSavedAt(System.currentTimeMillis());
        firestore.collection(COLLECTION).document(result.getId()).set(result).get();
        return result;
    }

    public List<DrawingResult> findAll() throws ExecutionException, InterruptedException {
        ApiFuture<QuerySnapshot> future = firestore.collection(COLLECTION)
                .orderBy("savedAt", Query.Direction.DESCENDING)
                .limit(100)
                .get();
        QuerySnapshot snapshot = future.get();
        return snapshot.getDocuments().stream()
                .map(doc -> doc.toObject(DrawingResult.class))
                .collect(Collectors.toList());
    }
}
