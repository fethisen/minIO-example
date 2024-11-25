package com.example.tr.minio_project.controller;

import com.example.tr.minio_project.service.MinioFileResponseDto;
import com.example.tr.minio_project.service.ProposalService;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/api/s3/storage")
@RequiredArgsConstructor
public class StorageController {
    private final ProposalService proposalService;

    @PostMapping
    public ResponseEntity<Void> upload(@RequestParam("companyName") String companyName,@RequestParam MultipartFile file) {
        proposalService.uploadFiles(file,companyName);
        return ResponseEntity.created(URI.create("/file")).build();
    }

    @SneakyThrows
    @GetMapping("/presigned-url/{bucketName}")
    public ResponseEntity<MinioFileResponseDto> generatePresignedUrl(@PathVariable String bucketName,
                                                                     @RequestParam String pathPrefix,// subfolder
                                                                     @RequestParam String objectName,
                                                                     @RequestParam String method) {

        io.minio.http.Method httpMethod = "GET".equalsIgnoreCase(method) ? io.minio.http.Method.GET : io.minio.http.Method.PUT;
        return ResponseEntity.ok(proposalService.generatePreSignUrl(bucketName, pathPrefix, objectName,httpMethod));
    }


    @GetMapping("/get-object/{bucketName}")
    public ResponseEntity<InputStreamResource> getObject(
            @PathVariable String bucketName,
            @RequestParam String pathPrefix,
            @RequestParam String objectName) {

        InputStream inputStream = proposalService.getObject(bucketName, pathPrefix, objectName);

        if (inputStream != null) {
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + objectName + "\"")
                    .body(new InputStreamResource(inputStream));
        } else {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
    }


    @GetMapping("/list-files/{bucketName}")
    public List<String> listFiles(@PathVariable String bucketName, @RequestParam String pathPrefix) {
        return proposalService.listFilesInPath(bucketName, pathPrefix);
    }
}
