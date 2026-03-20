package com.taxrecordsportal.tax_records_portal_backend.file_domain.file;

import com.taxrecordsportal.tax_records_portal_backend.user_domain.user.User;
import com.taxrecordsportal.tax_records_portal_backend.client_domain.client.Client;
import com.taxrecordsportal.tax_records_portal_backend.client_domain.client.ClientRepository;
import com.taxrecordsportal.tax_records_portal_backend.file_domain.file.dto.FileUploadResponse;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class FileService {

    private static final Set<String> ALLOWED_EXTENSIONS = Set.of(
            "pdf", "png", "jpg", "jpeg", "doc", "docx", "xls", "xlsx", "csv", "txt"
    );

    @Value("${application.file.upload-dir}")
    private String uploadDir;

    private final ClientRepository clientRepository;
    private final FileRepository fileRepository;

    @PostConstruct
    public void init() {
        try {
            Files.createDirectories(Paths.get(uploadDir));
        } catch (IOException e) {
            throw new RuntimeException("Could not create upload directory", e);
        }
    }

    public FileUploadResponse upload(UUID clientId, MultipartFile file) {
        Client client = getAccessibleClient(clientId);
        User currentUser = (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();

        if (file.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "File is empty");
        }

        String originalFilename = file.getOriginalFilename();
        String extension = getExtension(originalFilename);

        if (!ALLOWED_EXTENSIONS.contains(extension.toLowerCase())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "File type not allowed");
        }

        String storedFilename = UUID.randomUUID() + "-" + originalFilename;
        Path clientDir = Paths.get(uploadDir, clientId.toString());

        try {
            Files.createDirectories(clientDir);
            Path targetPath = clientDir.resolve(storedFilename);
            Files.copy(file.getInputStream(), targetPath, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to store file");
        }

        String url = "/api/v1/clients/" + clientId + "/files/" + storedFilename;

        FileEntity fileEntity = new FileEntity();
        fileEntity.setName(originalFilename);
        fileEntity.setUrl(url);
        fileEntity.setClient(client);
        fileEntity.setUploadedBy(currentUser);
        fileEntity = fileRepository.save(fileEntity);

        return new FileUploadResponse(fileEntity.getId(), fileEntity.getName());
    }

    public void deleteAllByClient(UUID clientId) {
        fileRepository.deleteAllByClientId(clientId);

        Path clientDir = Paths.get(uploadDir, clientId.toString()).normalize();
        if (java.nio.file.Files.exists(clientDir)) {
            try (var walker = java.nio.file.Files.walk(clientDir)) {
                walker.sorted(java.util.Comparator.reverseOrder())
                        .forEach(path -> {
                            try { java.nio.file.Files.delete(path); } catch (IOException ignored) {}
                        });
            } catch (IOException ignored) {}
        }
    }

    public void delete(UUID fileId) {
        FileEntity fileEntity = fileRepository.findWithClientById(fileId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "File not found"));

        UUID clientId = fileEntity.getClient().getId();
        String filename = fileEntity.getUrl().substring(fileEntity.getUrl().lastIndexOf('/') + 1);
        Path filePath = Paths.get(uploadDir, clientId.toString(), filename).normalize();

        try {
            Files.deleteIfExists(filePath);
        } catch (IOException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to delete file from storage");
        }

        fileRepository.delete(fileEntity);
    }

    public Resource previewById(UUID fileId) {
        FileEntity fileEntity = fileRepository.findWithClientById(fileId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "File not found"));

        UUID clientId = fileEntity.getClient().getId();
        enforceClientAccess(fileEntity.getClient());

        String filename = fileEntity.getUrl().substring(fileEntity.getUrl().lastIndexOf('/') + 1);
        Path filePath = Paths.get(uploadDir, clientId.toString(), filename).normalize();

        // prevent path traversal
        if (!filePath.startsWith(Paths.get(uploadDir, clientId.toString()).normalize())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Access denied");
        }

        try {
            Resource resource = new UrlResource(filePath.toUri());
            if (!resource.exists() || !resource.isReadable()) {
                throw new ResponseStatusException(HttpStatus.NOT_FOUND, "File not found");
            }
            return resource;
        } catch (MalformedURLException e) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "File not found");
        }
    }

    public String resolveMediaType(String filename) {
        try {
            Path path = Paths.get(filename);
            String contentType = Files.probeContentType(path);
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
