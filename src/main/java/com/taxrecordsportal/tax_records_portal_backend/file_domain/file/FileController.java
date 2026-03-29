package com.taxrecordsportal.tax_records_portal_backend.file_domain.file;

import com.taxrecordsportal.tax_records_portal_backend.file_domain.file.dto.FileUploadResponse;
import com.taxrecordsportal.tax_records_portal_backend.file_domain.file.dto.ImageUploadResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;

@RestController
@RequiredArgsConstructor
public class FileController {

    private final FileService fileService;

    @PostMapping("/api/v1/clients/{clientId}/files")
    @PreAuthorize("hasAuthority('document.upload')")
    public ResponseEntity<FileUploadResponse> upload(
            @PathVariable UUID clientId,
            @RequestParam("file") MultipartFile file
    ) {
        FileUploadResponse response = fileService.upload(clientId, file);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/api/v1/files/images")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ImageUploadResponse> uploadImage(@RequestParam("file") MultipartFile file) {
        return ResponseEntity.ok(fileService.uploadImage(file));
    }

    @DeleteMapping("/api/v1/files/images/{fileId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Void> deleteImage(@PathVariable UUID fileId) {
        fileService.deleteImage(fileId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/api/v1/files/images/{fileId}")
    public ResponseEntity<Resource> previewImage(@PathVariable UUID fileId) {
        FileService.FilePreviewResult preview = fileService.getImagePreview(fileId);
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(preview.contentType()))
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.builder("inline").filename(preview.name()).build().toString())
                .body(preview.resource());
    }

    @DeleteMapping("/api/v1/files/{fileId}")
    @PreAuthorize("hasAuthority('document.upload')")
    public ResponseEntity<Void> delete(@PathVariable UUID fileId) {
        fileService.delete(fileId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/api/v1/files/{fileId}/preview")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Resource> previewById(@PathVariable UUID fileId) {
        FileService.FilePreviewResult preview = fileService.getFilePreview(fileId);
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(preview.contentType()))
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.builder("inline").filename(preview.name()).build().toString())
                .body(preview.resource());
    }
}
