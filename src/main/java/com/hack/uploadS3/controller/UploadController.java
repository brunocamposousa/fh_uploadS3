package com.hack.uploadS3.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import com.hack.uploadS3.application.S3Service;

@RestController
@RequestMapping("/uploadS3")
public class UploadController {

    private final S3Service s3Service;

    public UploadController(S3Service s3Service) {
        this.s3Service = s3Service;
    }

    @PostMapping("/upload")
    public ResponseEntity<String> uploadVideo(
            @RequestParam("file") MultipartFile file,
            @RequestParam("id") String id,
            @RequestParam("email") String email) {
        try {
            String fileUrl = s3Service.uploadFile(file, id, email);
            return ResponseEntity.ok("Upload realizado com sucesso! URL: " + fileUrl);
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Erro ao enviar o arquivo: " + e.getMessage());
        }
    }    
}
