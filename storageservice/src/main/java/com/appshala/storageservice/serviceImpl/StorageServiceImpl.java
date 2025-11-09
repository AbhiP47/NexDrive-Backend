package com.appshala.storageservice.serviceImpl;

import com.appshala.storageservice.config.S3config;
import com.appshala.storageservice.service.StorageService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedGetObjectRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedPutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest;

import java.time.Duration;

@Service
public class StorageServiceImpl implements StorageService {

    @Value("${aws.bucket.name}")
    private String bucketName;

    private final S3Client s3Client;
    private final S3Presigner s3Presigner;

    private static final Duration PRESIGNED_URL_DURATION = Duration.ofMinutes(5);

    public StorageServiceImpl(S3Client s3Client, S3Presigner s3Presigner)
    {
        this.s3Client = s3Client;
        this.s3Presigner = s3Presigner;
    }
    @Override
    public String generateUploadPresignedUrl(String storageId) {
        PutObjectPresignRequest presignRequest = PutObjectPresignRequest.builder()
                .signatureDuration(PRESIGNED_URL_DURATION)
                .putObjectRequest(req-> req.bucket(bucketName).key(storageId))
                .build();
        PresignedPutObjectRequest presignedPutObjectRequest = s3Presigner.presignPutObject(presignRequest);
        return presignedPutObjectRequest.url().toString();
    }

    @Override
    public String generateDownloadpresignedUrl(String storageId) {
        GetObjectPresignRequest presignRequest = GetObjectPresignRequest.builder()
                .signatureDuration(PRESIGNED_URL_DURATION)
                .getObjectRequest(req-> req.bucket(bucketName).key(storageId))
                .build();

        PresignedGetObjectRequest presignedGetObjectRequest = s3Presigner.presignGetObject(presignRequest);
        return presignedGetObjectRequest.url().toString();
    }

    @Override
    public void deleteObject(String storageId) {
        DeleteObjectRequest deleteObjectRequest = DeleteObjectRequest.builder()
                .bucket(bucketName)
                .key(storageId)
                .build();
        s3Client.deleteObject(deleteObjectRequest);
    }
}
