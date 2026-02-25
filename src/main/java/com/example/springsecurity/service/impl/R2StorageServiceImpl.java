package com.example.springsecurity.service.impl;

import com.example.springsecurity.config.R2Properties;
import com.example.springsecurity.exception.FileStorageException;
import com.example.springsecurity.service.R2StorageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Exception;

import java.io.IOException;
import java.util.Set;
import java.util.UUID;



@Service
@RequiredArgsConstructor
@Slf4j

public class R2StorageServiceImpl implements R2StorageService {

    private final S3Client s3Client;
    private final R2Properties r2Properties;

    // ─── Allowed types ────────────────────────────────────────────────────────

    private static final Set<String> ALLOWED_TYPES = Set.of(
            "image/jpeg", "image/png", "image/webp", "image/gif"
    );

    private static final long        MAX_FILE_SIZE = 5 * 1024 * 1024L; // 5MB


    @Override
    public String uploadFile(MultipartFile file, String folder) throws IOException {
        validateFile(file);

        String fileKey = buildKey(folder, file.getOriginalFilename());

        try {
            PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                    .bucket(r2Properties.getBucketName())
                    .key(fileKey)
                    .contentType(file.getContentType())
                    .contentLength(file.getSize())
                    .build();

            s3Client.putObject(putObjectRequest,
                    RequestBody.fromBytes(file.getBytes()));

            String publicUrl = buildPublicUrl(fileKey);

            log.info("File uploaded successfully: {}", fileKey);
            return publicUrl;

        } catch (S3Exception e) {
            log.error("Failed to upload file to R2: {}", e.getMessage());
            throw new FileStorageException("Failed to upload file: " + e.getMessage());
        }

    }


    @Override
    public void deleteFile(String publicUrl) {
        String fileKey = extractKeyFromUrl(publicUrl);
        try {
            DeleteObjectRequest deleteObjectRequest = DeleteObjectRequest.builder()
                    .bucket(r2Properties.getBucketName())
                    .key(fileKey)
                    .build();

            s3Client.deleteObject(deleteObjectRequest);
            log.info("File deleted successfully: {}", fileKey);

        } catch (S3Exception e) {
            log.error("Failed to delete file from R2: {}", e.getMessage());
            throw new FileStorageException("Failed to delete file: " + e.getMessage());
        }
    }

    @Override
    public String replaceFile(String oldPublicUrl, MultipartFile newFile, String folder) throws IOException {
        deleteFile(oldPublicUrl);             // null-safe — skips if blank
        return uploadFile(newFile, folder);
    }

    // ─── Extract key from URL ─────────────────────────────────────────────────

    @Override
    public String extractKeyFromUrl(String publicUrl) {
        String base = r2Properties.getPublicUrl();
        if (StringUtils.hasText(publicUrl) && publicUrl.startsWith(base)) {
            return publicUrl.substring(base.length() + 1); // +1 strips leading "/"
        }
        return publicUrl;
    }

    // ─── Private helpers ──────────────────────────────────────────────────────

    private String buildPublicUrl(String key) {
        return r2Properties.getPublicUrl() + "/" + key;
    }

    private String buildKey(String folder, String originalFilename) {
        String extension = "";
        if (originalFilename != null && originalFilename.contains(".")) {
            extension = originalFilename.substring(originalFilename.lastIndexOf("."));
        }
        return folder + "/" + UUID.randomUUID() + extension;
    }

    private  void validateFile(MultipartFile file){

        if(file.isEmpty()){
            throw  new FileStorageException("File is empty");
        }

        if(file.getSize() > MAX_FILE_SIZE){
            throw new FileStorageException("File is invalid");
        }

        String  originalFilename = file.getOriginalFilename();

        if(originalFilename==null){
            throw new FileStorageException("File name is invalid");
        }

        if (!ALLOWED_TYPES.contains(file.getContentType())) {
            throw new FileStorageException(
                    "Invalid file type. Allowed: jpeg, png, webp, gif");
        }

    }
}
