package com.puredo.blog.Service.Storage;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.io.IOException;
import java.time.Year;
import java.util.Map;
import java.util.UUID;

@Service
public class S3StorageService implements StorageService {

    private static final long MAX_SIZE = 5 * 1024 * 1024;
    private static final Map<String, String> ALLOWED_TYPES = Map.of(
        "image/png", ".png",
        "image/jpeg", ".jpg",
        "image/webp", ".webp"
    );

    private final S3Client s3Client;

    @Value("${aws.bucket-name}")
    private String bucketName;

    @Value("${aws.region}")
    private String region;

    public S3StorageService(S3Client s3Client) {
        this.s3Client = s3Client;
    }

    @Override
    public String uploadAvatar(MultipartFile file) {
        String key = "avatars/" + UUID.randomUUID() + extension(file);
        return upload(file, key);
    }

    @Override
    public String uploadCover(MultipartFile file) {
        String key = "thumbnails/" + UUID.randomUUID() + extension(file);
        return upload(file, key);
    }

    @Override
    public String uploadPostImage(MultipartFile file) {
        String key = "posts/" + Year.now().getValue() + "/" + UUID.randomUUID() + extension(file);
        return upload(file, key);
    }

    @Override
    public void deleteFile(String url) {
        String prefix = "https://" + bucketName + ".s3." + region + ".amazonaws.com/";
        if (url == null || !url.startsWith(prefix)) {
            throw new IllegalArgumentException("URL inválida para este bucket");
        }
        String key = url.substring(prefix.length());
        s3Client.deleteObject(DeleteObjectRequest.builder()
            .bucket(bucketName)
            .key(key)
            .build());
    }

    private String upload(MultipartFile file, String key) {
        validate(file);
        try {
            PutObjectRequest request = PutObjectRequest.builder()
                .bucket(bucketName)
                .key(key)
                .contentType(file.getContentType())
                .build();
            s3Client.putObject(request, RequestBody.fromBytes(file.getBytes()));
            return publicUrl(key);
        } catch (IOException e) {
            throw new RuntimeException("Falha ao fazer upload da imagem", e);
        }
    }

    private void validate(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("Arquivo vazio");
        }
        if (file.getSize() > MAX_SIZE) {
            throw new IllegalArgumentException("Arquivo excede o tamanho máximo de 5MB");
        }
        if (!ALLOWED_TYPES.containsKey(file.getContentType())) {
            throw new IllegalArgumentException("Tipo de arquivo não permitido: " + file.getContentType());
        }
    }

    private String extension(MultipartFile file) {
        return ALLOWED_TYPES.getOrDefault(file.getContentType(), ".png");
    }

    private String publicUrl(String key) {
        return "https://" + bucketName + ".s3." + region + ".amazonaws.com/" + key;
    }
}
