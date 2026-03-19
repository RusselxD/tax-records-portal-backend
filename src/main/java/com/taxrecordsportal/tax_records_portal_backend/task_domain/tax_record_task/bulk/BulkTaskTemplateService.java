package com.taxrecordsportal.tax_records_portal_backend.task_domain.tax_record_task.bulk;

import com.taxrecordsportal.tax_records_portal_backend.client_domain.client.Client;
import com.taxrecordsportal.tax_records_portal_backend.task_domain.tax_record_task.Period;
import com.taxrecordsportal.tax_records_portal_backend.client_domain.client.ClientRepository;
import com.taxrecordsportal.tax_records_portal_backend.client_domain.client.ClientStatus;
import com.taxrecordsportal.tax_records_portal_backend.client_domain.client_info.dto.ClientInformation;
import com.taxrecordsportal.tax_records_portal_backend.common.util.UserDisplayUtil;
import com.taxrecordsportal.tax_records_portal_backend.user_domain.role.RoleKey;
import com.taxrecordsportal.tax_records_portal_backend.task_domain.tax_task_name.TaxTaskName;
import com.taxrecordsportal.tax_records_portal_backend.task_domain.tax_task_name.TaxTaskNameRepository;
import lombok.RequiredArgsConstructor;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.taxrecordsportal.tax_records_portal_backend.user_domain.user.User;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

import static com.taxrecordsportal.tax_records_portal_backend.common.util.SecurityUtil.getCurrentUser;

@Service
@RequiredArgsConstructor
public class BulkTaskTemplateService {

    private final ClientRepository clientRepository;
    private final TaxTaskNameRepository taxTaskNameRepository;

    /**
     * Generates an Excel workbook with two sheets:
     * - "Template": empty rows with headers for bulk task entry
     * - "References": lookup data (clients, accountants, categories, etc.)
     */
    @Transactional(readOnly = true)
    public byte[] generateTemplate() throws IOException {
        try (XSSFWorkbook workbook = new XSSFWorkbook()) {
            createTemplateSheet(workbook);
            createReferencesSheet(workbook);
            createClientsSheet(workbook);

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            workbook.write(out);
            return out.toByteArray();
        }
    }

    private void createTemplateSheet(Workbook workbook) {
        Sheet sheet = workbook.createSheet("Template");
        CellStyle headerStyle = createHeaderStyle(workbook);

        String[] headers = {
                "Client Name", "Category", "Sub Category", "Task Name",
                "Year", "Period", "Deadline", "Description", "Assigned To"
        };

        Row headerRow = sheet.createRow(0);
        for (int i = 0; i < headers.length; i++) {
            Cell cell = headerRow.createCell(i);
            cell.setCellValue(headers[i]);
            cell.setCellStyle(headerStyle);
            sheet.setColumnWidth(i, 5000);
        }
    }

    private void createReferencesSheet(Workbook workbook) {
        Sheet sheet = workbook.createSheet("References");
        CellStyle headerStyle = createHeaderStyle(workbook);

        // Col A-C: Hierarchy (Category > Sub Category > Task Name)
        // Col D: Periods
        String[] headers = {"Category", "Sub Category", "Task Name", "Periods"};

        Row headerRow = sheet.createRow(0);
        for (int i = 0; i < headers.length; i++) {
            Cell cell = headerRow.createCell(i);
            cell.setCellValue(headers[i]);
            cell.setCellStyle(headerStyle);
        }
        sheet.setColumnWidth(0, 8000);
        sheet.setColumnWidth(1, 8000);
        sheet.setColumnWidth(2, 20000);
        sheet.setColumnWidth(3, 3000);

        List<TaxTaskName> sortedTaskNames = taxTaskNameRepository.findAllWithSubCategory().stream()
                .sorted((a, b) -> {
                    int cat = a.getSubCategory().getCategory().getName()
                            .compareToIgnoreCase(b.getSubCategory().getCategory().getName());
                    if (cat != 0) return cat;
                    int sub = a.getSubCategory().getName()
                            .compareToIgnoreCase(b.getSubCategory().getName());
                    if (sub != 0) return sub;
                    return a.getName().compareToIgnoreCase(b.getName());
                })
                .toList();

        int maxRows = Math.max(sortedTaskNames.size(), Period.values().length);
        String prevCategory = null;
        String prevSubCategory = null;

        for (int i = 0; i < maxRows; i++) {
            Row row = sheet.createRow(i + 1);

            if (i < sortedTaskNames.size()) {
                TaxTaskName tn = sortedTaskNames.get(i);
                String category = tn.getSubCategory().getCategory().getName();
                String subCategory = tn.getSubCategory().getName();

                if (!category.equals(prevCategory)) {
                    row.createCell(0).setCellValue(category);
                    prevCategory = category;
                    prevSubCategory = null;
                }
                if (!subCategory.equals(prevSubCategory)) {
                    row.createCell(1).setCellValue(subCategory);
                    prevSubCategory = subCategory;
                }
                row.createCell(2).setCellValue(tn.getName());
            }

            if (i < Period.values().length) {
                row.createCell(3).setCellValue(Period.values()[i].name());
            }
        }
    }

    private void createClientsSheet(Workbook workbook) {
        Sheet sheet = workbook.createSheet("Clients");
        CellStyle headerStyle = createHeaderStyle(workbook);

        String[] headers = {"Client", "Assigned Accountants"};
        Row headerRow = sheet.createRow(0);
        for (int i = 0; i < headers.length; i++) {
            Cell cell = headerRow.createCell(i);
            cell.setCellValue(headers[i]);
            cell.setCellStyle(headerStyle);
        }
        sheet.setColumnWidth(0, 8000);
        sheet.setColumnWidth(1, 8000);

        List<ClientAccountantGroup> groups = loadClientAccountantGroups();

        int rowIdx = 1;
        for (ClientAccountantGroup group : groups) {
            for (int j = 0; j < group.accountantNames.size(); j++) {
                Row row = sheet.createRow(rowIdx++);
                if (j == 0) {
                    row.createCell(0).setCellValue(group.clientName);
                }
                row.createCell(1).setCellValue(group.accountantNames.get(j));
            }
        }
    }

    private List<ClientAccountantGroup> loadClientAccountantGroups() {
        User currentUser = getCurrentUser();
        boolean hasViewAll = currentUser.getAuthorities().stream()
                .anyMatch(a -> Objects.equals(a.getAuthority(), "client.view.all"));

        return clientRepository.findByStatusAndAccountantsIsNotEmpty(ClientStatus.ACTIVE_CLIENT)
                .stream()
                .filter(client -> hasViewAll || client.getAccountants().stream()
                        .anyMatch(a -> a.getId().equals(currentUser.getId())))
                .map(client -> {
                    if (client.getClientInfo() == null) return null;
                    String clientName = computeClientName(client.getClientInfo().getClientInformation());
                    if (clientName == null) return null;

                    List<String> accountants = client.getAccountants().stream()
                            .filter(a -> a.getRole().getKey() == RoleKey.CSD || a.getRole().getKey() == RoleKey.OOS)
                            .map(UserDisplayUtil::formatDisplayName)
                            .sorted()
                            .toList();
                    return new ClientAccountantGroup(clientName, accountants);
                })
                .filter(group -> group != null)
                .sorted(Comparator.comparing(g -> g.clientName))
                .toList();
    }

    private record ClientAccountantGroup(String clientName, List<String> accountantNames) {}

    private String computeClientName(ClientInformation ci) {
        if (ci == null) return null;
        String registered = ci.registeredName();
        String trade = ci.tradeName();
        if (registered != null && trade != null) return registered + " (" + trade + ")";
        if (registered != null) return registered;
        return trade;
    }

    private CellStyle createHeaderStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        Font font = workbook.createFont();
        font.setBold(true);
        style.setFont(font);
        style.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        return style;
    }
}
