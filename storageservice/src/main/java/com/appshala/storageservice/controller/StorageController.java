package com.appshala.storageservice.controller;

import com.appshala.storageservice.payload.PresignedUrlResponse;
import com.appshala.storageservice.service.StorageService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/storage")
public class StorageController {
    private final StorageService service;

    public StorageController(StorageService service)
    {
        this.service=service;
    }

    @GetMapping("/generate-upload-url")
    public ResponseEntity<PresignedUrlResponse> getUploadUrl()
    {
        String storageId = UUID.randomUUID().toString();
        String presignedUrl = service.generateUploadPresignedUrl(storageId);
        return ResponseEntity.ok(new PresignedUrlResponse(storageId,presignedUrl));
    }

    @GetMapping("/download-url/{storageId}")
    public ResponseEntity<PresignedUrlResponse> getDownloadUrl(@PathVariable("stoargeId") String storageId)
    {
        String presignedUrl = service.generateDownloadpresignedUrl(storageId);
        return ResponseEntity.ok(new PresignedUrlResponse(storageId,presignedUrl));
    }

    @DeleteMapping("/delete/{storageId}")
    public ResponseEntity<Void> deleteFile(@PathVariable("storageId") String storageId)
    {
        service.deleteObject(storageId);
        return ResponseEntity.noContent().build();
    }
}
