package com.appshala.storageservice.payload;

public record PresignedUrlResponse(
        String storageId,
        String presignedUrl
) {
}
