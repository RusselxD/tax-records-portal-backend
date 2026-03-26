package com.taxrecordsportal.tax_records_portal_backend.user_domain.user;

import com.taxrecordsportal.tax_records_portal_backend.user_domain.role.RoleKey;
import com.taxrecordsportal.tax_records_portal_backend.user_domain.user.dto.request.ChangePasswordRequest;
import com.taxrecordsportal.tax_records_portal_backend.user_domain.user.dto.request.MePatchRequest;
import com.taxrecordsportal.tax_records_portal_backend.user_domain.user.dto.request.ResendActivationRequest;
import com.taxrecordsportal.tax_records_portal_backend.user_domain.user.dto.request.UserCreateRequest;
import com.taxrecordsportal.tax_records_portal_backend.user_domain.user.dto.request.UserUpdateRequest;
import com.taxrecordsportal.tax_records_portal_backend.user_domain.user.dto.request.UserStatusUpdateRequest;
import com.taxrecordsportal.tax_records_portal_backend.user_domain.user.dto.response.AccountantListItemResponse;
import com.taxrecordsportal.tax_records_portal_backend.user_domain.user.dto.response.AvatarResponse;
import com.taxrecordsportal.tax_records_portal_backend.user_domain.user.dto.response.ClientAccountResponse;
import com.taxrecordsportal.tax_records_portal_backend.user_domain.user.dto.response.MePatchResponse;
import com.taxrecordsportal.tax_records_portal_backend.user_domain.user.dto.response.MeResponse;
import com.taxrecordsportal.tax_records_portal_backend.user_domain.user.dto.response.UserListItemResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @GetMapping()
    @PreAuthorize("hasAuthority('user.view.all')")
    public ResponseEntity<List<UserListItemResponse>> getAllEmployees(){
        return ResponseEntity.ok(userService.getAllEmployees());
    }

    @PostMapping()
    @PreAuthorize("hasAuthority('user.create')")
    public ResponseEntity<UserListItemResponse> createEmployee(
            @Valid @RequestBody UserCreateRequest request
            ){
        return ResponseEntity.ok(userService.createEmployee(request));
    }

    @PatchMapping("/{userId}")
    @PreAuthorize("hasAuthority('user.create')")
    public ResponseEntity<UserListItemResponse> updateUser(
            @PathVariable UUID userId,
            @Valid @RequestBody UserUpdateRequest request) {
        return ResponseEntity.ok(userService.updateUser(userId, request));
    }

    @PatchMapping("/{userId}/status")
    @PreAuthorize("hasAuthority('user.create')")
    public ResponseEntity<Void> changeUserStatus(
            @PathVariable UUID userId,
            @Valid @RequestBody UserStatusUpdateRequest request) {
        userService.changeUserStatus(userId, request);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/client/{clientId}")
    @PreAuthorize("hasAuthority('client.view.own') or hasAuthority('client.view.all')")
    public ResponseEntity<List<ClientAccountResponse>> getClientAccounts(@PathVariable UUID clientId) {
        return ResponseEntity.ok(userService.getClientAccounts(clientId));
    }

    @PostMapping("/{id}/resend-activation")
    @PreAuthorize("hasAuthority('user.create') or hasAuthority('client.view.own')")
    public ResponseEntity<Void> resendActivationEmail(
            @PathVariable UUID id,
            @Valid @RequestBody ResendActivationRequest request) {
        userService.resendActivationEmail(id, request);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/accountants")
    @PreAuthorize("hasAuthority('client.assign') or hasAuthority('task.create')")
    public ResponseEntity<List<AccountantListItemResponse>> getAccountants(
            @RequestParam List<RoleKey> roleKey
    ) {
        return ResponseEntity.ok(userService.getAccountantsByRoleKeys(roleKey));
    }

    @GetMapping("/me")
    public ResponseEntity<MeResponse> getMe() {
        return ResponseEntity.ok(userService.getMe());
    }

    @PatchMapping("/me")
    public ResponseEntity<MePatchResponse> updateMe(@Valid @RequestBody MePatchRequest request) {
        return ResponseEntity.ok(userService.updateMe(request));
    }

    @PostMapping("/me/avatar")
    public ResponseEntity<AvatarResponse> uploadAvatar(@RequestParam("file") MultipartFile file) {
        return ResponseEntity.ok(userService.uploadAvatar(file));
    }

    @DeleteMapping("/me/avatar")
    public ResponseEntity<Void> deleteAvatar() {
        userService.deleteAvatar();
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/me/change-password")
    public ResponseEntity<Void> changePassword(@Valid @RequestBody ChangePasswordRequest request) {
        userService.changePassword(request);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{userId}/avatar")
    public ResponseEntity<Resource> getAvatar(@PathVariable UUID userId) {
        UserService.AvatarResult avatar = userService.getAvatarWithMediaType(userId);
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(avatar.mediaType()))
                .body(avatar.resource());
    }
}
