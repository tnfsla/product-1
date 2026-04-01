package com.example.product1.controller;

import com.example.product1.model.DrawingResult;
import com.example.product1.service.DrawingService;
import com.example.product1.service.OcrService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@CrossOrigin(origins = "*")
public class OcrController {

    @Autowired
    private OcrService ocrService;

    @Autowired
    private DrawingService drawingService;

    @PostMapping("/api/parse")
    public ResponseEntity<DrawingResult> parse(@RequestParam("file") MultipartFile file) throws Exception {
        DrawingResult result = ocrService.processFile(file);
        return ResponseEntity.ok(result);
    }

    @PostMapping("/api/save")
    public ResponseEntity<DrawingResult> save(@RequestBody DrawingResult result) throws Exception {
        DrawingResult saved = drawingService.save(result);
        return ResponseEntity.ok(saved);
    }

    @GetMapping("/api/results")
    public ResponseEntity<List<DrawingResult>> results() throws Exception {
        return ResponseEntity.ok(drawingService.findAll());
    }
}
