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
        ensurePermission("user.create", "Create internal user accounts");
        ensurePermission("user.view.all", "View all internal user accounts");
        ensurePermission("user.deactivate", "Deactivate internal user accounts");
        ensurePermission("client.create", "Create a client account and initiate onboarding");
        ensurePermission("client.view.own", "View only clients you onboarded or are assigned to");
        ensurePermission("client.view.all", "View all clients across the system");
        ensurePermission("client.view.archived", "View archived clients");
        ensurePermission("client.assign", "Formally assign an approved client to a CSD accountant");
        ensurePermission("client.reassign", "Modify which CSD accountants are assigned to a client post-handoff");
        ensurePermission("client_info.create", "Encode and submit a client's onboarding profile");
        ensurePermission("client_info.edit", "Edit a client profile (corrections after QTD rejection)");
        ensurePermission("client_info.review", "Approve or reject a submitted client profile");
        ensurePermission("client_info.view.own", "View client info for your own onboarded/assigned clients only");
        ensurePermission("client_info.view.all", "View all client info across the system");
        ensurePermission("client_info_template.manage", "Define and edit the global client onboarding field structure");
        ensurePermission("tax_records.view.own", "View tax records for assigned clients only");
        ensurePermission("tax_records.view.all", "View all tax records across the system");
        ensurePermission("task.create", "Create tasks and assign them to CSD accountants");
        ensurePermission("task.view.own", "View only tasks assigned to you");
        ensurePermission("task.view.all", "View all tasks across the system");
        ensurePermission("task.execute", "Upload draft and mark a task as complete");
        ensurePermission("task.review", "Approve or reject a submitted task");
        ensurePermission("tax_task_category.manage", "Manage tax task categories, sub-categories, and task names");
        ensurePermission("client_notice.manage", "Create, edit, delete notices on a client's dashboard");
        ensurePermission("billing.manage", "Send and update amount due, due dates, and invoice files to a client");
        ensurePermission("document.upload", "Upload documents to the system");
        ensurePermission("reminder.create", "Set manual reminders for filing deadlines and unpaid balances");
        ensurePermission("notification.receive", "Receive in-app notifications");
        ensurePermission("analytics.view.own", "View analytics for your own assigned/onboarded clients");
        ensurePermission("analytics.view.all", "View all analytics across the system");
        ensurePermission("analytics.system.view", "View system-wide analytics dashboard");
        ensurePermission("client.manage", "Change client status (Onboarding, Active Client, Offboarding, Inactive Client)");

        // Role-Permission assignments
        // Manager
        assignPermission(RoleKey.MANAGER, "user.create");
        assignPermission(RoleKey.MANAGER, "user.view.all");
        assignPermission(RoleKey.MANAGER, "user.deactivate");
        assignPermission(RoleKey.MANAGER, "client.create");
        assignPermission(RoleKey.MANAGER, "client.view.all");
        assignPermission(RoleKey.MANAGER, "client.view.archived");
        assignPermission(RoleKey.MANAGER, "client.assign");
        assignPermission(RoleKey.MANAGER, "client.reassign");
        assignPermission(RoleKey.MANAGER, "client_info.create");
        assignPermission(RoleKey.MANAGER, "client_info.edit");
        assignPermission(RoleKey.MANAGER, "client_info.review");
        assignPermission(RoleKey.MANAGER, "client_info.view.all");
        assignPermission(RoleKey.MANAGER, "client_info_template.manage");
        assignPermission(RoleKey.MANAGER, "tax_records.view.all");
        assignPermission(RoleKey.MANAGER, "task.create");
        assignPermission(RoleKey.MANAGER, "task.view.all");
        assignPermission(RoleKey.MANAGER, "task.review");
        assignPermission(RoleKey.MANAGER, "tax_task_category.manage");
        assignPermission(RoleKey.MANAGER, "client_notice.manage");
        assignPermission(RoleKey.MANAGER, "billing.manage");
        assignPermission(RoleKey.MANAGER, "document.upload");
        assignPermission(RoleKey.MANAGER, "reminder.create");
        assignPermission(RoleKey.MANAGER, "notification.receive");
        assignPermission(RoleKey.MANAGER, "analytics.view.all");
        assignPermission(RoleKey.MANAGER, "analytics.system.view");
        assignPermission(RoleKey.MANAGER, "client.manage");

        // Onboarding, Offboarding & Support
        assignPermission(RoleKey.OOS, "client.create");
        assignPermission(RoleKey.OOS, "client.view.own");
        assignPermission(RoleKey.OOS, "client_info.create");
        assignPermission(RoleKey.OOS, "client_info.edit");
        assignPermission(RoleKey.OOS, "client_info.view.own");
        assignPermission(RoleKey.OOS, "task.view.own");
        assignPermission(RoleKey.OOS, "task.execute");
        assignPermission(RoleKey.OOS, "client_notice.manage");
        assignPermission(RoleKey.OOS, "document.upload");
        assignPermission(RoleKey.OOS, "notification.receive");
        assignPermission(RoleKey.OOS, "client.assign");
        assignPermission(RoleKey.OOS, "analytics.view.own");

        // Quality, Training & Development
        assignPermission(RoleKey.QTD, "client.view.own");
        assignPermission(RoleKey.QTD, "client_info.review");
        assignPermission(RoleKey.QTD, "client_info.view.own");
        assignPermission(RoleKey.QTD, "tax_records.view.own");
        assignPermission(RoleKey.QTD, "task.create");
        assignPermission(RoleKey.QTD, "task.view.own");
        assignPermission(RoleKey.QTD, "task.review");
        assignPermission(RoleKey.QTD, "tax_task_category.manage");
        assignPermission(RoleKey.QTD, "notification.receive");
        assignPermission(RoleKey.QTD, "analytics.view.own");

        // Client Service Delivery
        assignPermission(RoleKey.CSD, "client.view.own");
        assignPermission(RoleKey.CSD, "client_info.view.own");
        assignPermission(RoleKey.CSD, "client_info.edit");
        assignPermission(RoleKey.CSD, "tax_records.view.own");
        assignPermission(RoleKey.CSD, "task.view.own");
        assignPermission(RoleKey.CSD, "task.execute");
        assignPermission(RoleKey.CSD, "client_notice.manage");
        assignPermission(RoleKey.CSD, "document.upload");
        assignPermission(RoleKey.CSD, "reminder.create");
        assignPermission(RoleKey.CSD, "notification.receive");
        assignPermission(RoleKey.CSD, "analytics.view.own");

        // Internal Accounting / Billing
        assignPermission(RoleKey.BILLING, "client_info.view.all");
        assignPermission(RoleKey.BILLING, "billing.manage");
        assignPermission(RoleKey.BILLING, "notification.receive");

        // Client
        assignPermission(RoleKey.CLIENT, "client_info.view.own");
        assignPermission(RoleKey.CLIENT, "notification.receive");
    }

    private void ensurePermission(String name, String description) {
        if (permissionCache.containsKey(name)) {
            return;
        }
        Permission permission = new Permission();
        permission.setName(name);
        permission.setDescription(description);
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
