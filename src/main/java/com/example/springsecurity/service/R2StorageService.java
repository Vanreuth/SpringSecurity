package com.example.springsecurity.service;

import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

public interface R2StorageService {

    String uploadFile(MultipartFile file , String folder) throws IOException;

    void deleteFile(String publicUrl);

    String replaceFile(String oldPublicUrl, MultipartFile newFile, String folder) throws IOException;

    String extractKeyFromUrl(String publicUrl);

}
