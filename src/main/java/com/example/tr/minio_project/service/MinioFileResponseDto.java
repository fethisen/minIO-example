package com.example.tr.minio_project.service;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
public class MinioFileResponseDto {
    private String fileName;
    private String fileUrl;
}
