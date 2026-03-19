package com.taxrecordsportal.tax_records_portal_backend.file_domain.file;

import com.taxrecordsportal.tax_records_portal_backend.file_domain.file.dto.FileUploadResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
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

    @DeleteMapping("/api/v1/files/{fileId}")
    @PreAuthorize("hasAuthority('document.upload')")
    public ResponseEntity<Void> delete(@PathVariable UUID fileId) {
        fileService.delete(fileId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/api/v1/files/{fileId}/preview")
    @PreAuthorize("hasAuthority('client.view.own') or hasAuthority('client_info.view.own') or hasAuthority('client_info.view.all') or hasAuthority('tax_records.view.own') or hasAuthority('tax_records.view.all')")
    public ResponseEntity<Resource> previewById(@PathVariable UUID fileId) {
        FileEntity fileEntity = fileService.getFileEntity(fileId);
        Resource resource = fileService.previewById(fileId);
        String contentType = fileService.resolveMediaType(fileEntity.getName());
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(contentType))
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + fileEntity.getName() + "\"")
                .body(resource);
    }
}
