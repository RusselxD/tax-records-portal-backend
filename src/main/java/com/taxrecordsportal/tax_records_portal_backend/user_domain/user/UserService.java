package com.taxrecordsportal.tax_records_portal_backend.user_domain.user;

import com.taxrecordsportal.tax_records_portal_backend.client_domain.client.Client;
import com.taxrecordsportal.tax_records_portal_backend.client_domain.client.ClientRepository;
import com.taxrecordsportal.tax_records_portal_backend.common_domain.email.EmailService;
import com.taxrecordsportal.tax_records_portal_backend.user_domain.employee_position.EmployeePosition;
import com.taxrecordsportal.tax_records_portal_backend.user_domain.employee_position.EmployeePositionRepository;
import com.taxrecordsportal.tax_records_portal_backend.user_domain.role.Role;
import com.taxrecordsportal.tax_records_portal_backend.user_domain.role.RoleKey;
import com.taxrecordsportal.tax_records_portal_backend.user_domain.role.RoleRepository;
import com.taxrecordsportal.tax_records_portal_backend.user_domain.user.dto.request.ChangePasswordRequest;
import com.taxrecordsportal.tax_records_portal_backend.user_domain.user.dto.request.MePatchRequest;
import com.taxrecordsportal.tax_records_portal_backend.user_domain.user.dto.request.ResendActivationRequest;
import com.taxrecordsportal.tax_records_portal_backend.user_domain.user.dto.request.UserCreateRequest;
import com.taxrecordsportal.tax_records_portal_backend.user_domain.user.dto.response.AccountantListItemResponse;
import com.taxrecordsportal.tax_records_portal_backend.user_domain.user.dto.response.AvatarResponse;
import com.taxrecordsportal.tax_records_portal_backend.user_domain.user.dto.response.ClientAccountResponse;
import com.taxrecordsportal.tax_records_portal_backend.user_domain.user.dto.response.MePatchResponse;
import com.taxrecordsportal.tax_records_portal_backend.user_domain.user.dto.response.MeResponse;
import com.taxrecordsportal.tax_records_portal_backend.user_domain.user.dto.response.UserListItemResponse;
import com.taxrecordsportal.tax_records_portal_backend.common.util.UserDisplayUtil;
import com.taxrecordsportal.tax_records_portal_backend.user_domain.user.mapper.UserMapper;
import com.taxrecordsportal.tax_records_portal_backend.user_domain.user_tokens.TokenType;
import com.taxrecordsportal.tax_records_portal_backend.user_domain.user_tokens.UserToken;
import com.taxrecordsportal.tax_records_portal_backend.user_domain.user_tokens.UserTokenRepository;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

import static com.taxrecordsportal.tax_records_portal_backend.common.util.SecurityUtil.getCurrentUser;

@Service
@RequiredArgsConstructor
public class UserService {

    private static final Set<String> ALLOWED_IMAGE_EXTENSIONS = Set.of("png", "jpg", "jpeg");

    private final UserRepository userRepository;
    private final ClientRepository clientRepository;
    private final RoleRepository roleRepository;
    private final EmployeePositionRepository employeePositionRepository;
    private final UserMapper userMapper;
    private final UserTokenRepository userTokenRepository;
    private final EmailService emailService;
    private final PasswordEncoder passwordEncoder;

    @Value("${application.security.jwt.activation-token-expiration}")
    private long activationTokenExpiration;

    @Value("${application.file.upload-dir}")
    private String uploadDir;

    @PostConstruct
    public void init() {
        try {
            Files.createDirectories(Paths.get(uploadDir, "avatars"));
        } catch (IOException e) {
            throw new RuntimeException("Could not create avatars directory", e);
        }
    }

    public List<UserListItemResponse> getAllEmployees() {
        Role clientRole = roleRepository.findByKey(RoleKey.CLIENT)
                .orElseThrow(() -> new RuntimeException("Client role not found"));

        return userRepository.findAllByRoleNot(clientRole)
                .stream()
                .map(userMapper::mapUserToListItem)
                .toList();
    }

    @Transactional
    public UserListItemResponse createEmployee(UserCreateRequest request) {
        if (userRepository.findByEmail(request.email()).isPresent()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Email already exists.");
        }

        Role role = roleRepository.findById(request.roleId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Role not found."));

        EmployeePosition position = null;
        if (request.positionId() != null) {
            position = employeePositionRepository.findById(request.positionId())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Position not found."));
        }

        User user = new User();
        user.setFirstName(request.firstName());
        user.setLastName(request.lastName());
        user.setEmail(request.email());
        user.setRole(role);
        user.setPosition(position);
        user.setTitles(request.titles());
        user.setStatus(UserStatus.PENDING);

        User savedUser = userRepository.save(user);

        UserToken activationToken = new UserToken();
        activationToken.setUser(savedUser);
        activationToken.setToken(UUID.randomUUID().toString());
        activationToken.setType(TokenType.ACCOUNT_ACTIVATION);
        activationToken.setExpiresAt(Instant.now().plusMillis(activationTokenExpiration));
        userTokenRepository.save(activationToken);

        emailService.sendActivationEmail(
                savedUser.getEmail(),
                savedUser.getFirstName(),
                activationToken.getToken()
        );

        return userMapper.mapUserToListItem(savedUser);
    }

    @Transactional(readOnly = true)
    public ClientAccountResponse getClientAccount(UUID clientId) {
        User currentUser = getCurrentUser();
        boolean hasViewAll = currentUser.getAuthorities().stream()
                .anyMatch(a -> Objects.equals(a.getAuthority(), "client.view.all"));

        Client client = clientRepository.findWithCreatorAccountantsAndUserById(clientId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Client not found"));

        if (!hasViewAll
                && !client.getCreatedBy().getId().equals(currentUser.getId())
                && client.getAccountants().stream().noneMatch(a -> a.getId().equals(currentUser.getId()))) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You do not have access to this client");
        }

        User user = client.getUser();
        if (user == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Client does not have an account");
        }

        return userMapper.toClientAccountResponse(user);
    }

    @Transactional
    public void resendActivationEmail(UUID userId, ResendActivationRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found."));

        if (user.getStatus() != UserStatus.PENDING) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "User has already been activated.");
        }

        if (request.firstName() != null) user.setFirstName(request.firstName());
        if (request.lastName() != null) user.setLastName(request.lastName());
        if (request.email() != null) user.setEmail(request.email());

        userTokenRepository.deleteByUserAndType(user, TokenType.ACCOUNT_ACTIVATION);

        UserToken activationToken = new UserToken();
        activationToken.setUser(user);
        activationToken.setToken(UUID.randomUUID().toString());
        activationToken.setType(TokenType.ACCOUNT_ACTIVATION);
        activationToken.setExpiresAt(Instant.now().plusMillis(activationTokenExpiration));
        userTokenRepository.save(activationToken);

        emailService.sendActivationEmail(
                user.getEmail(),
                user.getFirstName(),
                activationToken.getToken()
        );
    }

    @Transactional(readOnly = true)
    public MeResponse getMe() {
        User user = userRepository.findById(getCurrentUser().getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
        List<String> permissions = user.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .toList();
        return new MeResponse(
                user.getId(),
                user.getFirstName(),
                user.getLastName(),
                user.getEmail(),
                UserDisplayUtil.formatDisplayName(user),
                user.getRole().getName(),
                user.getRole().getKey(),
                user.getPosition() != null ? user.getPosition().getName() : null,
                user.getStatus(),
                user.getProfileUrl(),
                permissions,
                user.getTitles()
        );
    }

    @Transactional
    public MePatchResponse updateMe(MePatchRequest request) {
        User user = getCurrentUser();

        if (request.email() != null && !request.email().equals(user.getEmail())) {
            if (userRepository.findByEmail(request.email()).isPresent()) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "Email already in use.");
            }
            user.setEmail(request.email());
        }
        if (request.firstName() != null) user.setFirstName(request.firstName());
        if (request.lastName() != null) user.setLastName(request.lastName());
        if (request.titles() != null) user.setTitles(request.titles());

        userRepository.save(user);

        return new MePatchResponse(UserDisplayUtil.formatDisplayName(user), user.getEmail());
    }

    @Transactional(readOnly = true)
    public List<AccountantListItemResponse> getAccountantsByRoleKeys(List<RoleKey> roleKeys) {
        return userRepository.findByRole_KeyInAndStatus(roleKeys, UserStatus.ACTIVE)
                .stream()
                .map(userMapper::toAccountantListItemResponse)
                .toList();
    }

    @Transactional
    public AvatarResponse uploadAvatar(MultipartFile file) {
        if (file.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "File is empty");
        }

        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null || !originalFilename.contains(".")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "File must have an extension");
        }
        String extension = originalFilename.substring(originalFilename.lastIndexOf('.') + 1).toLowerCase();
        if (!ALLOWED_IMAGE_EXTENSIONS.contains(extension)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Only PNG, JPG, and JPEG images are allowed");
        }

        User currentUser = getCurrentUser();
        Path avatarDir = Paths.get(uploadDir, "avatars", currentUser.getId().toString());

        try {
            Files.createDirectories(avatarDir);

            // Delete old avatar file if present
            deleteAvatarFileFromDisk(currentUser, avatarDir);

            String storedFilename = UUID.randomUUID() + "-" + originalFilename;
            Files.copy(file.getInputStream(), avatarDir.resolve(storedFilename), StandardCopyOption.REPLACE_EXISTING);

            // TODO: S3 migration — replace with S3 upload and store the full S3 URL directly
            String profileUrl = "/api/v1/users/" + currentUser.getId() + "/avatar";
            currentUser.setProfileUrl(profileUrl);
            userRepository.save(currentUser);

            return new AvatarResponse(profileUrl);
        } catch (IOException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to store avatar");
        }
    }

    @Transactional
    public void deleteAvatar() {
        User currentUser = getCurrentUser();
        if (currentUser.getProfileUrl() == null) {
            return;
        }

        Path avatarDir = Paths.get(uploadDir, "avatars", currentUser.getId().toString());
        deleteAvatarFileFromDisk(currentUser, avatarDir);

        currentUser.setProfileUrl(null);
        userRepository.save(currentUser);
    }

    @Transactional
    public void changePassword(ChangePasswordRequest request) {
        User currentUser = getCurrentUser();

        if (!passwordEncoder.matches(request.currentPassword(), currentUser.getPasswordHash())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Current password is incorrect");
        }

        currentUser.setPasswordHash(passwordEncoder.encode(request.newPassword()));
        userRepository.save(currentUser);
    }

    public record AvatarResult(Resource resource, String mediaType) {}

    public AvatarResult getAvatarWithMediaType(UUID userId) {
        Path avatarDir = Paths.get(uploadDir, "avatars", userId.toString());
        try (var files = Files.list(avatarDir)) {
            Path avatarFile = files.findFirst()
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Avatar not found"));
            Resource resource = new UrlResource(avatarFile.toUri());
            if (!resource.exists() || !resource.isReadable()) {
                throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Avatar not found");
            }
            String mediaType;
            try { mediaType = Files.probeContentType(avatarFile); } catch (IOException e) { mediaType = null; }
            return new AvatarResult(resource, mediaType != null ? mediaType : "application/octet-stream");
        } catch (ResponseStatusException e) {
            throw e;
        } catch (IOException e) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Avatar not found");
        }
    }

    private void deleteAvatarFileFromDisk(User user, Path avatarDir) {
        if (user.getProfileUrl() == null) return;
        try (var files = Files.list(avatarDir)) {
            files.forEach(path -> {
                try { Files.deleteIfExists(path); } catch (IOException ignored) {}
            });
        } catch (IOException ignored) {}
    }
}
