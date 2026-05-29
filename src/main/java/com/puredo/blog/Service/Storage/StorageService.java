package com.puredo.blog.Service.Storage;

import org.springframework.web.multipart.MultipartFile;

public interface StorageService {
    String uploadAvatar(MultipartFile file);
    String uploadCover(MultipartFile file);
    String uploadPostImage(MultipartFile file);
    void deleteFile(String url);
}
