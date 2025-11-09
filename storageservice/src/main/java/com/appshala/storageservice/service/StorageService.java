package com.appshala.storageservice.service;

public interface StorageService {

    public  String generateUploadPresignedUrl(String storageId);

    public String generateDownloadpresignedUrl(String storageId);

    void deleteObject(String storageId);
}
