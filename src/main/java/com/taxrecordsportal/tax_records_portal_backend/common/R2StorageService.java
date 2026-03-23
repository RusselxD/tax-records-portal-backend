package com.taxrecordsportal.tax_records_portal_backend.common;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;

import java.io.InputStream;
import java.net.URI;
import java.util.List;

@Slf4j
@Service
public class R2StorageService {

    private final S3Client s3Client;
    private final String bucketName;

    public R2StorageService(
            @Value("${application.storage.r2.endpoint}") String endpoint,
            @Value("${application.storage.r2.access-key-id}") String accessKeyId,
            @Value("${application.storage.r2.secret-access-key}") String secretAccessKey,
            @Value("${application.storage.r2.bucket-name}") String bucketName) {
        this.bucketName = bucketName;
        this.s3Client = S3Client.builder()
                .endpointOverride(URI.create(endpoint))
                .credentialsProvider(StaticCredentialsProvider.create(
                        AwsBasicCredentials.create(accessKeyId, secretAccessKey)))
                .region(Region.of("auto"))
                .build();
    }

    public void upload(String key, byte[] bytes, String contentType) {
        s3Client.putObject(
                PutObjectRequest.builder()
                        .bucket(bucketName)
                        .key(key)
                        .contentType(contentType)
                        .build(),
                RequestBody.fromBytes(bytes));
    }

    public InputStream download(String key) {
        return s3Client.getObject(
                GetObjectRequest.builder()
                        .bucket(bucketName)
                        .key(key)
                        .build());
    }

    public void delete(String key) {
        s3Client.deleteObject(
                DeleteObjectRequest.builder()
                        .bucket(bucketName)
                        .key(key)
                        .build());
    }

    public List<String> listKeys(String prefix) {
        return s3Client.listObjectsV2(
                ListObjectsV2Request.builder()
                        .bucket(bucketName)
                        .prefix(prefix)
                        .build())
                .contents().stream()
                .map(S3Object::key)
                .toList();
    }

    public void deleteByPrefix(String prefix) {
        List<String> keys = listKeys(prefix);
        if (keys.isEmpty()) return;
        s3Client.deleteObjects(
                DeleteObjectsRequest.builder()
                        .bucket(bucketName)
                        .delete(Delete.builder()
                                .objects(keys.stream()
                                        .map(k -> ObjectIdentifier.builder().key(k).build())
                                        .toList())
                                .build())
                        .build());
    }
}
