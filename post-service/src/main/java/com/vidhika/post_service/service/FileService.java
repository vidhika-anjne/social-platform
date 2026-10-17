package com.vidhika.post_service.service;

import com.vidhika.post_service.dto.FileUploadResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class FileService
{
    private final S3Service s3Service;
    public FileUploadResponse uploadMediaFiles(List<MultipartFile> files)
    {
        try {
            List<String> uploadedUrls = s3Service.uploadFiles(files);
            log.info("UPLOAD_SUCCESS | Successfully uploaded all the files to S3");
            return FileUploadResponse.builder()
                    .mediaUrls(uploadedUrls)
                    .count(uploadedUrls.size())
                    .message("All Files uploaded successfully!!")
                    .build();
        } catch (IOException e) {
            log.error("UPLOAD_FAILED | Failed to upload file to S3");
            return FileUploadResponse.builder()
                    .mediaUrls(new ArrayList<>())
                    .count(0)
                    .message("Unable to upload file")
                    .build();
        }
    }
}
