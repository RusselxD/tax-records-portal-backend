package com.taxrecordsportal.tax_records_portal_backend.user_domain.seeder;

import com.taxrecordsportal.tax_records_portal_backend.user_domain.permission.Permission;
import com.taxrecordsportal.tax_records_portal_backend.user_domain.permission.PermissionRepository;
import com.taxrecordsportal.tax_records_portal_backend.user_domain.role.Role;
import com.taxrecordsportal.tax_records_portal_backend.user_domain.role.RoleKey;
import com.taxrecordsportal.tax_records_portal_backend.user_domain.role.RoleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Component
@RequiredArgsConstructor
@Order(1)
public class RolePermissionSeeder implements CommandLineRunner {

    private final RoleRepository roleRepository;
    private final PermissionRepository permissionRepository;

    private final Map<String, Permission> permissionCache = new HashMap<>();
    private final Map<RoleKey, Role> roleCache = new HashMap<>();

    @Override
    @Transactional
    public void run(String... args) {
        // Load existing permissions and roles into cache
        permissionRepository.findAll().forEach(p -> permissionCache.put(p.getName(), p));
        roleRepository.findAll().forEach(r -> roleCache.put(r.getKey(), r));

        // Permissions
        ensurePermission("user.create");
        ensurePermission("user.view.all");
        ensurePermission("client.create");
        ensurePermission("client.view.own");
        ensurePermission("client.view.all");
        ensurePermission("client.assign");
        ensurePermission("client.reassign");
        ensurePermission("client_info.create");
        ensurePermission("client_info.edit");
        ensurePermission("client_info.review");
        ensurePermission("client_info.view.own");
        ensurePermission("client_info.view.all");
        ensurePermission("tax_records.view.own");
        ensurePermission("tax_records.view.all");
        ensurePermission("task.create");
        ensurePermission("task.view.own");
        ensurePermission("task.view.all");
        ensurePermission("task.execute");
        ensurePermission("task.review");
        ensurePermission("billing.manage");
        ensurePermission("billing.view.own");
        ensurePermission("document.upload");
        ensurePermission("reminder.create");
        ensurePermission("notification.receive");
        ensurePermission("analytics.system.view");
        ensurePermission("client.manage");
        ensurePermission("consultation.create");
        ensurePermission("consultation.view.own");
        ensurePermission("consultation.view.all");
        ensurePermission("consultation.review");
        ensurePermission("consultation.config.manage");
        ensurePermission("consultation.view.own.client");

        // Role-Permission assignments
        // Manager
        assignPermission(RoleKey.MANAGER, "user.create");
        assignPermission(RoleKey.MANAGER, "user.view.all");
        assignPermission(RoleKey.MANAGER, "client.create");
        assignPermission(RoleKey.MANAGER, "client.view.all");
        assignPermission(RoleKey.MANAGER, "client.assign");
        assignPermission(RoleKey.MANAGER, "client.reassign");
        assignPermission(RoleKey.MANAGER, "client_info.create");
        assignPermission(RoleKey.MANAGER, "client_info.edit");
        assignPermission(RoleKey.MANAGER, "client_info.review");
        assignPermission(RoleKey.MANAGER, "client_info.view.all");
        assignPermission(RoleKey.MANAGER, "tax_records.view.all");
        assignPermission(RoleKey.MANAGER, "task.create");
        assignPermission(RoleKey.MANAGER, "task.view.all");
        assignPermission(RoleKey.MANAGER, "task.review");
        assignPermission(RoleKey.MANAGER, "billing.manage");
        assignPermission(RoleKey.MANAGER, "document.upload");
        assignPermission(RoleKey.MANAGER, "reminder.create");
        assignPermission(RoleKey.MANAGER, "notification.receive");
        assignPermission(RoleKey.MANAGER, "analytics.system.view");
        assignPermission(RoleKey.MANAGER, "client.manage");
        assignPermission(RoleKey.MANAGER, "consultation.create");
        assignPermission(RoleKey.MANAGER, "consultation.view.all");
        assignPermission(RoleKey.MANAGER, "consultation.review");
        assignPermission(RoleKey.MANAGER, "consultation.config.manage");

        // Onboarding, Offboarding & Support
        assignPermission(RoleKey.OOS, "client.create");
        assignPermission(RoleKey.OOS, "client.view.own");
        assignPermission(RoleKey.OOS, "client_info.create");
        assignPermission(RoleKey.OOS, "client_info.edit");
        assignPermission(RoleKey.OOS, "client_info.view.own");
        assignPermission(RoleKey.OOS, "task.view.own");
        assignPermission(RoleKey.OOS, "task.execute");
        assignPermission(RoleKey.OOS, "document.upload");
        assignPermission(RoleKey.OOS, "notification.receive");
        assignPermission(RoleKey.OOS, "client.assign");
        assignPermission(RoleKey.OOS, "tax_records.view.own");
        assignPermission(RoleKey.OOS, "consultation.create");
        assignPermission(RoleKey.OOS, "consultation.view.own");
        assignPermission(RoleKey.OOS, "consultation.config.manage");

        // Quality, Training & Development
        assignPermission(RoleKey.QTD, "client.view.own");
        assignPermission(RoleKey.QTD, "client_info.review");
        assignPermission(RoleKey.QTD, "client_info.view.own");
        assignPermission(RoleKey.QTD, "tax_records.view.own");
        assignPermission(RoleKey.QTD, "task.create");
        assignPermission(RoleKey.QTD, "task.view.own");
        assignPermission(RoleKey.QTD, "task.review");
        assignPermission(RoleKey.QTD, "notification.receive");
        assignPermission(RoleKey.QTD, "consultation.view.own");
        assignPermission(RoleKey.QTD, "consultation.review");

        // Client Service Delivery
        assignPermission(RoleKey.CSD, "client.view.own");
        assignPermission(RoleKey.CSD, "client_info.view.own");
        assignPermission(RoleKey.CSD, "client_info.edit");
        assignPermission(RoleKey.CSD, "tax_records.view.own");
        assignPermission(RoleKey.CSD, "task.view.own");
        assignPermission(RoleKey.CSD, "task.execute");
        assignPermission(RoleKey.CSD, "document.upload");
        assignPermission(RoleKey.CSD, "reminder.create");
        assignPermission(RoleKey.CSD, "notification.receive");
        assignPermission(RoleKey.CSD, "consultation.create");
        assignPermission(RoleKey.CSD, "consultation.view.own");
        assignPermission(RoleKey.CSD, "consultation.config.manage");

        // Internal Accounting / Billing
        assignPermission(RoleKey.BILLING, "client_info.view.all");
        assignPermission(RoleKey.BILLING, "billing.manage");
        assignPermission(RoleKey.BILLING, "document.upload");
        assignPermission(RoleKey.BILLING, "notification.receive");

        // Client
        assignPermission(RoleKey.CLIENT, "client_info.view.own");
        assignPermission(RoleKey.CLIENT, "billing.view.own");
        assignPermission(RoleKey.CLIENT, "notification.receive");
        assignPermission(RoleKey.CLIENT, "consultation.view.own.client");
    }

    private void ensurePermission(String name) {
        if (permissionCache.containsKey(name)) {
            return;
        }
        Permission permission = new Permission();
        permission.setName(name);
        permissionCache.put(name, permissionRepository.save(permission));
    }

    private void assignPermission(RoleKey roleKey, String permissionName) {
        Role role = roleCache.computeIfAbsent(roleKey, key -> {
            Role newRole = new Role();
            newRole.setKey(key);
            newRole.setName(key.getDisplayName());
            newRole.setPermissions(new HashSet<>());
            return roleRepository.save(newRole);
        });

        Permission permission = permissionCache.get(permissionName);
        if (role.getPermissions().contains(permission)) {
            return;
        }

        role.getPermissions().add(permission);
        roleRepository.save(role);
    }
}
