package com.taxrecordsportal.tax_records_portal_backend.file_domain.file;

import com.taxrecordsportal.tax_records_portal_backend.common.R2StorageService;
import com.taxrecordsportal.tax_records_portal_backend.common.util.ClientAccessHelper;
import com.taxrecordsportal.tax_records_portal_backend.user_domain.user.User;
import com.taxrecordsportal.tax_records_portal_backend.client_domain.client.Client;
import com.taxrecordsportal.tax_records_portal_backend.client_domain.client.ClientRepository;
import com.taxrecordsportal.tax_records_portal_backend.file_domain.file.dto.FileUploadResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import com.taxrecordsportal.tax_records_portal_backend.file_domain.file.dto.ImageUploadResponse;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;
import java.util.UUID;

import static com.taxrecordsportal.tax_records_portal_backend.common.util.SecurityUtil.getCurrentUser;

@Service
@RequiredArgsConstructor
public class FileService {

    private static final Set<String> ALLOWED_EXTENSIONS = Set.of(
            "pdf", "doc", "docx", "xls", "xlsx", "csv", "jpg", "jpeg", "png", "gif", "webp", "dat"
    );
    private static final long MAX_DOCUMENT_SIZE = 25 * 1024 * 1024; // 25MB

    private static final Set<String> ALLOWED_IMAGE_EXTENSIONS = Set.of("jpg", "jpeg", "png", "gif", "webp");
    private static final long MAX_IMAGE_SIZE = 10 * 1024 * 1024; // 10MB

    private final ClientRepository clientRepository;
    private final FileRepository fileRepository;
    private final R2StorageService r2StorageService;
    private final ClientAccessHelper clientAccessHelper;

    @Transactional
    public FileUploadResponse upload(UUID clientId, MultipartFile file) {
        Client client = getAccessibleClient(clientId);
        User currentUser = getCurrentUser();

        if (file.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "File is empty");
        }

        String originalFilename = file.getOriginalFilename();
        if (originalFilename != null) {
            originalFilename = Path.of(originalFilename).getFileName().toString();
        }
        String extension = getExtension(originalFilename);

        if (!ALLOWED_EXTENSIONS.contains(extension.toLowerCase())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "File type not allowed. Allowed: pdf, doc, docx, xls, xlsx, csv, jpg, jpeg, png, gif, webp, dat");
        }

        if (file.getSize() > MAX_DOCUMENT_SIZE) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "File size exceeds 25MB limit");
        }

        String key = "clients/" + clientId + "/" + UUID.randomUUID() + "-" + originalFilename;
        String contentType = resolveMediaType(originalFilename);

        try {
            r2StorageService.upload(key, file.getBytes(), contentType);
        } catch (IOException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to store file");
        }

        FileEntity fileEntity = new FileEntity();
        fileEntity.setName(originalFilename);
        fileEntity.setUrl(key);
        fileEntity.setClient(client);
        fileEntity.setUploadedBy(currentUser);
        fileEntity = fileRepository.save(fileEntity);

        return new FileUploadResponse(fileEntity.getId(), fileEntity.getName());
    }

    @Transactional
    public ImageUploadResponse uploadImage(MultipartFile file) {
        User currentUser = getCurrentUser();

        if (file.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "File is empty");
        }

        String originalFilename = file.getOriginalFilename();
        if (originalFilename != null) {
            originalFilename = Path.of(originalFilename).getFileName().toString();
        }
        String extension = getExtension(originalFilename);

        if (!ALLOWED_IMAGE_EXTENSIONS.contains(extension.toLowerCase())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Only image files are allowed: jpg, jpeg, png, gif, webp");
        }

        if (file.getSize() > MAX_IMAGE_SIZE) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Image size exceeds 10MB limit");
        }

        String key = "images/" + UUID.randomUUID() + "-" + originalFilename;
        String contentType = resolveMediaType(originalFilename);

        try {
            r2StorageService.upload(key, file.getBytes(), contentType);
        } catch (IOException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to store image");
        }

        FileEntity fileEntity = new FileEntity();
        fileEntity.setName(originalFilename);
        fileEntity.setUrl(key);
        fileEntity.setUploadedBy(currentUser);
        fileEntity = fileRepository.save(fileEntity);

        String previewUrl = "/api/v1/files/images/" + fileEntity.getId();
        return new ImageUploadResponse(fileEntity.getId(), previewUrl);
    }

    @Transactional
    public void deleteImage(UUID fileId) {
        FileEntity fileEntity = fileRepository.findById(fileId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Image not found"));

        if (fileEntity.getClient() != null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "This file is not an uploaded image");
        }

        User currentUser = getCurrentUser();
        if (fileEntity.getUploadedBy() == null || !fileEntity.getUploadedBy().getId().equals(currentUser.getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You can only delete your own images");
        }

        r2StorageService.delete(fileEntity.getUrl());
        fileRepository.delete(fileEntity);
    }

    @Transactional(readOnly = true)
    public Resource previewImage(UUID fileId) {
        FileEntity fileEntity = fileRepository.findById(fileId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Image not found"));

        if (fileEntity.getClient() != null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Image not found");
        }

        return new InputStreamResource(r2StorageService.download(fileEntity.getUrl()));
    }

    public record FilePreviewResult(String name, String contentType, Resource resource) {}

    @Transactional(readOnly = true)
    public FilePreviewResult getFilePreview(UUID fileId) {
        FileEntity fileEntity = fileRepository.findWithClientById(fileId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "File not found"));

        if (fileEntity.getClient() != null) {
            enforceClientAccess(fileEntity.getClient());
        }

        String contentType = resolveMediaType(fileEntity.getName());
        Resource resource = new InputStreamResource(r2StorageService.download(fileEntity.getUrl()));
        return new FilePreviewResult(fileEntity.getName(), contentType, resource);
    }

    @Transactional(readOnly = true)
    public FilePreviewResult getImagePreview(UUID fileId) {
        FileEntity fileEntity = fileRepository.findById(fileId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Image not found"));

        if (fileEntity.getClient() != null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Image not found");
        }

        String contentType = resolveMediaType(fileEntity.getName());
        Resource resource = new InputStreamResource(r2StorageService.download(fileEntity.getUrl()));
        return new FilePreviewResult(fileEntity.getName(), contentType, resource);
    }

    @Transactional
    public void deleteAllByClient(UUID clientId) {
        r2StorageService.deleteByPrefix("clients/" + clientId + "/");
        fileRepository.deleteAllByClientId(clientId);
    }

    @Transactional
    public void delete(UUID fileId) {
        FileEntity fileEntity = fileRepository.findWithClientById(fileId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "File not found"));

        r2StorageService.delete(fileEntity.getUrl());
        fileRepository.delete(fileEntity);
    }

    @Transactional(readOnly = true)
    public Resource previewById(UUID fileId) {
        FileEntity fileEntity = fileRepository.findWithClientById(fileId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "File not found"));

        if (fileEntity.getClient() != null) {
            enforceClientAccess(fileEntity.getClient());
        }

        return new InputStreamResource(r2StorageService.download(fileEntity.getUrl()));
    }

    public String resolveMediaType(String filename) {
        try {
            String contentType = Files.probeContentType(Path.of(filename));
            return contentType != null ? contentType : "application/octet-stream";
        } catch (IOException e) {
            return "application/octet-stream";
        }
    }

    @Transactional(readOnly = true)
    public FileEntity getFileEntity(UUID fileId) {
        return fileRepository.findById(fileId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "File not found"));
    }

    private Client getAccessibleClient(UUID clientId) {
        Client client = clientRepository.findWithCreatorAccountantsAndUsersById(clientId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Client not found"));
        enforceClientAccess(client);
        return client;
    }

    private void enforceClientAccess(Client client) {
        clientAccessHelper.enforceAccessWithProtection(
                client,
                "You do not have access to this client's files",
                "client_info.view.all", "tax_records.view.all"
        );
    }

    private String getExtension(String filename) {
        if (filename == null || !filename.contains(".")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "File must have an extension");
        }
        return filename.substring(filename.lastIndexOf('.') + 1);
    }
}
