package com.example.tr.minio_project.service;

import io.minio.*;
import io.minio.errors.*;
import io.minio.messages.Bucket;
import io.minio.messages.Item;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
@Slf4j
public class ProposalService {
    private final MinioClient minioClient;

    @Value("${minio.bucket.image}")
    private String proposalBucket;

    @PostConstruct
    public void ensureBucketExists() {
        try {
            boolean found = minioClient.bucketExists(BucketExistsArgs.builder()
                    .bucket(proposalBucket).build());
            if (!found) {
                minioClient.makeBucket(MakeBucketArgs.builder()
                        .bucket(proposalBucket).build());
                log.info("Bucket '{}' created successfully.", proposalBucket);
            } else {
                log.info("Bucket '{}' already exists.", proposalBucket);
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to initialize MinIO bucket: " + e.getMessage(), e);
        }
    }

    public void uploadFiles(MultipartFile file,String companyName) {
        if (file.isEmpty() || file.getOriginalFilename() == null) {
            throw new RuntimeException("File must have name");
        }
        String fileName = companyName + "/" + generateFileName(file);
        String contentType = file.getContentType();
        try {
            InputStream inputStream = file.getInputStream();
            saveObject(inputStream, fileName, contentType);
        } catch (Exception e) {
            log.error("Error occurred while uploading file: {}", e.getMessage());
            throw new RuntimeException("File upload failed", e);
        }

    }

    public MinioFileResponseDto generatePreSignUrl(String bucketName,String pathPrefix,String objectName, io.minio.http.Method method) throws ServerException, InsufficientDataException, ErrorResponseException, IOException, NoSuchAlgorithmException, InvalidKeyException, InvalidResponseException, XmlParserException, InternalException {
        String fullObjectPath = pathPrefix +"/"+ objectName;
        GetPresignedObjectUrlArgs args = GetPresignedObjectUrlArgs.builder()
                .method(method)
                .bucket(proposalBucket)
                .object(fullObjectPath)
                .expiry(10, TimeUnit.HOURS)
                .build();
        String url = minioClient.getPresignedObjectUrl(args);

        MinioFileResponseDto minioFileResponseDto = MinioFileResponseDto.builder()
                .fileName(fullObjectPath)
                .fileUrl(url)
                .build();
        return minioFileResponseDto;
    }

    public InputStream getObject(String bucketName, String pathPrefix, String objectName) {
        try {
            // Construct the full object path
            String fullObjectPath = pathPrefix +"/"+ objectName;

            // Retrieve the object as an InputStream
            return minioClient.getObject(
                    GetObjectArgs.builder()
                            .bucket(bucketName)
                            .object(fullObjectPath)
                            .build()
            );
        } catch (MinioException e) {
            System.out.println("Error occurred: " + e);
        } catch (Exception e) {
            System.out.println("General Error: " + e.getMessage());
        }
        return null;
    }


    public List<String> listFilesInPath(String bucketName, String pathPrefix) {
        List<String> fileList = new ArrayList<>();
        try {
            Iterable<Result<Item>> results = minioClient.listObjects(
                    ListObjectsArgs.builder()
                            .bucket(bucketName)
                            .prefix(pathPrefix)
                            .recursive(true)
                            .build()
            );

            for (Result<Item> result : results) {
                Item item = result.get();
                fileList.add(item.objectName());
            }
        } catch (MinioException e) {
            System.out.println("Error occurred: " + e);
        } catch (Exception e) {
            System.out.println("General Error: " + e.getMessage());
        }
        return fileList;
    }


    @SneakyThrows
    private void saveObject(final InputStream inputStream, final String fileName, String contentType) {

        minioClient.putObject(
                PutObjectArgs.builder()
                        .bucket(proposalBucket)
                        .object(fileName)
                        .stream(inputStream, inputStream.available(), -1)
                        .contentType(contentType != null ? contentType : "application/octet-stream")
                        .build()
        );
        log.info("File '{}' uploaded successfully to bucket '{}'", fileName, proposalBucket);
    }


    private String generateFileName(final MultipartFile file) {
        String extension = getExtension(file);
        return UUID.randomUUID() + "." + extension;
    }

    private String getExtension(final MultipartFile file) {
        return file.getOriginalFilename()
                .substring(file.getOriginalFilename()
                        .lastIndexOf(".") + 1);
    }
}
