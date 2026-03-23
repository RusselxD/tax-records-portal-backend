package com.taxrecordsportal.tax_records_portal_backend.file_domain.file;

import com.taxrecordsportal.tax_records_portal_backend.common.R2StorageService;
import com.taxrecordsportal.tax_records_portal_backend.user_domain.user.User;
import com.taxrecordsportal.tax_records_portal_backend.client_domain.client.Client;
import com.taxrecordsportal.tax_records_portal_backend.client_domain.client.ClientRepository;
import com.taxrecordsportal.tax_records_portal_backend.file_domain.file.dto.FileUploadResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class FileService {

    private static final Set<String> ALLOWED_EXTENSIONS = Set.of(
            "pdf", "png", "jpg", "jpeg", "doc", "docx", "xls", "xlsx", "csv", "txt"
    );

    private final ClientRepository clientRepository;
    private final FileRepository fileRepository;
    private final R2StorageService r2StorageService;

    public FileUploadResponse upload(UUID clientId, MultipartFile file) {
        Client client = getAccessibleClient(clientId);
        User currentUser = (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();

        if (file.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "File is empty");
        }

        String originalFilename = file.getOriginalFilename();
        if (originalFilename != null) {
            originalFilename = Path.of(originalFilename).getFileName().toString();
        }
        String extension = getExtension(originalFilename);

        if (!ALLOWED_EXTENSIONS.contains(extension.toLowerCase())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "File type not allowed");
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

    public void deleteAllByClient(UUID clientId) {
        r2StorageService.deleteByPrefix("clients/" + clientId + "/");
        fileRepository.deleteAllByClientId(clientId);
    }

    public void delete(UUID fileId) {
        FileEntity fileEntity = fileRepository.findWithClientById(fileId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "File not found"));

        r2StorageService.delete(fileEntity.getUrl());
        fileRepository.delete(fileEntity);
    }

    public Resource previewById(UUID fileId) {
        FileEntity fileEntity = fileRepository.findWithClientById(fileId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "File not found"));

        enforceClientAccess(fileEntity.getClient());

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

    public FileEntity getFileEntity(UUID fileId) {
        return fileRepository.findById(fileId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "File not found"));
    }

    private Client getAccessibleClient(UUID clientId) {
        Client client = clientRepository.findWithCreatorAccountantsAndUserById(clientId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Client not found"));
        enforceClientAccess(client);
        return client;
    }

    private void enforceClientAccess(Client client) {
        User currentUser = (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();

        boolean hasViewAll = currentUser.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("client_info.view.all")
                        || a.getAuthority().equals("tax_records.view.all"));

        if (!hasViewAll) {
            boolean isCreator = client.getCreatedBy() != null
                    && client.getCreatedBy().getId().equals(currentUser.getId());
            boolean isAssignedAccountant = client.getAccountants() != null
                    && client.getAccountants().stream().anyMatch(a -> a.getId().equals(currentUser.getId()));
            boolean isClient = client.getUser() != null
                    && client.getUser().getId().equals(currentUser.getId());

            if (!isCreator && !isAssignedAccountant && !isClient) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You do not have access to this client's files");
            }
        }
    }

    private String getExtension(String filename) {
        if (filename == null || !filename.contains(".")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "File must have an extension");
        }
        return filename.substring(filename.lastIndexOf('.') + 1);
    }
}
