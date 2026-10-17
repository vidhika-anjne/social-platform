package com.vidhika.post_service.controller;

import com.vidhika.post_service.dto.FileUploadResponse;
import com.vidhika.post_service.service.FileService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1")
public class FileUploadController
{
    private final FileService fileService;
    @PostMapping("/upload")
    public ResponseEntity<FileUploadResponse> uploadFile(@RequestParam("files")List<MultipartFile> files){
        return ResponseEntity.ok(fileService.uploadMediaFiles(files));
    }
}
