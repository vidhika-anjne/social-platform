package com.vidhika.post_service.service;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.GetUrlRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class FileService
{
    private final S3Client s3Client;
    @Value("${aws.bucketName}")
    private String bucketName;
    private static final Set<String> FILE_TYPES = Set.of("image/","video/","audio/");
    public List<String> uploadMediaFiles(MultipartFile[] files) throws IOException
    {
        List<String> uploadedUrls = new ArrayList<>();
        for(MultipartFile file : files)
        {
            if(file.isEmpty()) continue;
            String contentType = file.getContentType();
            if(!FILE_TYPES.contains(contentType))
            {
               String fileName = UUID.randomUUID().toString() + "_" + file.getOriginalFilename();
                PutObjectRequest putObjectRequest = PutObjectRequest
                        .builder()
                        .bucket(bucketName)
                        .key(fileName)
                        .contentType(contentType)
                        .build();
                s3Client.putObject(putObjectRequest,
                        RequestBody.fromInputStream(file.getInputStream(),file.getSize()));
                String fileUrl = s3Client.utilities()
                        .getUrl(GetUrlRequest
                                .builder()
                                .bucket(bucketName)
                                .key(fileName)
                                .build())
                        .toExternalForm();
                uploadedUrls.add(fileUrl);
            }
        }
        return uploadedUrls;
    }
    public void deleteFileFromS3(String fileUrl) throws URISyntaxException {
        if (fileUrl == null || fileUrl.isBlank()) return;
        URI uri = new URI(fileUrl);
        String path = uri.getPath();
        String s3Key = path.startsWith("/") ? path.substring(1) : path;
        if (s3Key.startsWith(bucketName + "/")) {
            s3Key = s3Key.substring(bucketName.length() + 1);
        }
        DeleteObjectRequest deleteObjectRequest = DeleteObjectRequest.builder()
                .bucket(bucketName)
                .key(s3Key)
                .build();

        s3Client.deleteObject(deleteObjectRequest);
    }
}
