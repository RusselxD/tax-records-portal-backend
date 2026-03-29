package com.taxrecordsportal.tax_records_portal_backend.task_domain.tax_record_task.bulk;

import com.taxrecordsportal.tax_records_portal_backend.client_domain.client.ClientRepository;
import com.taxrecordsportal.tax_records_portal_backend.client_domain.client.ClientStatus;
import com.taxrecordsportal.tax_records_portal_backend.client_domain.client_info.dto.ClientInformation;
import com.taxrecordsportal.tax_records_portal_backend.common.util.UserDisplayUtil;
import com.taxrecordsportal.tax_records_portal_backend.task_domain.tax_record_task.Period;
import com.taxrecordsportal.tax_records_portal_backend.task_domain.tax_task_name.TaxTaskName;
import com.taxrecordsportal.tax_records_portal_backend.task_domain.tax_task_name.TaxTaskNameRepository;
import com.taxrecordsportal.tax_records_portal_backend.user_domain.role.RoleKey;
import com.taxrecordsportal.tax_records_portal_backend.user_domain.user.User;
import lombok.RequiredArgsConstructor;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellRangeAddressList;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

import static com.taxrecordsportal.tax_records_portal_backend.common.util.SecurityUtil.getCurrentUser;

@Service
@RequiredArgsConstructor
public class BulkTaskTemplateService {

    private static final int TEMPLATE_ROWS = 500;

    private final ClientRepository clientRepository;
    private final TaxTaskNameRepository taxTaskNameRepository;

    @Transactional(readOnly = true)
    public byte[] generateTemplate() throws IOException {
        try (XSSFWorkbook workbook = new XSSFWorkbook()) {
            List<TaxTaskName> taskNames = taxTaskNameRepository.findAllWithSubCategory().stream()
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

            List<ClientAccountantGroup> clientGroups = loadClientAccountantGroups();

            createTemplateSheet(workbook);
            createReferencesSheet(workbook, taskNames);
            createClientsSheet(workbook, clientGroups);
            Sheet dataSheet = createDataSheet(workbook, taskNames, clientGroups);
            createNamedRanges(workbook, dataSheet, taskNames, clientGroups);
            addDropdowns(workbook.getSheet("Template"));

            // Hide data sheet and ensure Template is first
            workbook.setSheetVisibility(workbook.getSheetIndex(dataSheet), SheetVisibility.VERY_HIDDEN);
            workbook.setSheetOrder("Template", 0);
            workbook.setActiveSheet(0);

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            workbook.write(out);
            return out.toByteArray();
        }
    }

    // --- Template sheet with dropdowns ---

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

    private void addDropdowns(Sheet sheet) {
        DataValidationHelper dvHelper = sheet.getDataValidationHelper();
        CellRangeAddressList fullRange = new CellRangeAddressList(1, TEMPLATE_ROWS, 0, 0);

        // Col A: Client Name
        addValidation(sheet, dvHelper, "ClientNames", 0, false);

        // Col B: Category
        addValidation(sheet, dvHelper, "CategoryNames", 1, false);

        // Col C: Sub Category — cascades from Category (B2 is relative, shifts per row)
        addFormulaValidation(sheet, dvHelper, 2,
                "INDIRECT(\"cat_\"&SUBSTITUTE(SUBSTITUTE(SUBSTITUTE(SUBSTITUTE(SUBSTITUTE(B2,\" \",\"_\"),\"&\",\"_\"),\",\",\"_\"),\"(\",\"_\"),\")\",\"_\"))");

        // Col D: Task Name — cascades from Sub Category
        addFormulaValidation(sheet, dvHelper, 3,
                "INDIRECT(\"sub_\"&SUBSTITUTE(SUBSTITUTE(SUBSTITUTE(SUBSTITUTE(SUBSTITUTE(C2,\" \",\"_\"),\"&\",\"_\"),\",\",\"_\"),\"(\",\"_\"),\")\",\"_\"))");

        // Col F: Period — strict
        addValidation(sheet, dvHelper, "PeriodNames", 5, true);

        // Col I: Assigned To — cascades from Client Name
        addFormulaValidation(sheet, dvHelper, 8,
                "INDIRECT(\"cli_\"&SUBSTITUTE(SUBSTITUTE(SUBSTITUTE(SUBSTITUTE(SUBSTITUTE(A2,\" \",\"_\"),\"&\",\"_\"),\",\",\"_\"),\"(\",\"_\"),\")\",\"_\"))");
    }

    private void addValidation(Sheet sheet, DataValidationHelper dvHelper,
                               String namedRange, int col, boolean strict) {
        DataValidationConstraint constraint = dvHelper.createFormulaListConstraint(namedRange);
        CellRangeAddressList range = new CellRangeAddressList(1, TEMPLATE_ROWS, col, col);
        DataValidation validation = dvHelper.createValidation(constraint, range);
        validation.setShowErrorBox(strict);
        sheet.addValidationData(validation);
    }

    private void addFormulaValidation(Sheet sheet, DataValidationHelper dvHelper,
                                      int col, String formula) {
        DataValidationConstraint constraint = dvHelper.createFormulaListConstraint(formula);
        CellRangeAddressList range = new CellRangeAddressList(1, TEMPLATE_ROWS, col, col);
        DataValidation validation = dvHelper.createValidation(constraint, range);
        validation.setShowErrorBox(false);
        sheet.addValidationData(validation);
    }

    // --- Hidden data sheet for named ranges ---

    private Sheet createDataSheet(Workbook workbook, List<TaxTaskName> taskNames,
                                  List<ClientAccountantGroup> clientGroups) {
        Sheet sheet = workbook.createSheet("_Data");

        // Build category -> subcategories -> task names maps
        LinkedHashMap<String, LinkedHashMap<String, List<String>>> hierarchy = buildHierarchy(taskNames);

        int col = 0;

        // Col 0: Category names
        int row = 0;
        for (String category : hierarchy.keySet()) {
            Row r = getOrCreateRow(sheet, row++);
            r.createCell(col).setCellValue(category);
        }
        col++;

        // Subsequent columns: one per category (its subcategories), one per subcategory (its task names)
        for (var catEntry : hierarchy.entrySet()) {
            row = 0;
            for (String subCat : catEntry.getValue().keySet()) {
                Row r = getOrCreateRow(sheet, row++);
                r.createCell(col).setCellValue(subCat);
            }
            col++;

            for (var subEntry : catEntry.getValue().entrySet()) {
                row = 0;
                for (String tn : subEntry.getValue()) {
                    Row r = getOrCreateRow(sheet, row++);
                    r.createCell(col).setCellValue(tn);
                }
                col++;
            }
        }

        // Periods column
        row = 0;
        for (Period p : Period.values()) {
            Row r = getOrCreateRow(sheet, row++);
            r.createCell(col).setCellValue(p.name());
        }
        col++;

        // Client names column
        int clientNamesCol = col;
        row = 0;
        for (ClientAccountantGroup group : clientGroups) {
            Row r = getOrCreateRow(sheet, row++);
            r.createCell(col).setCellValue(group.clientName());
        }
        col++;

        // One column per client (its accountants)
        for (ClientAccountantGroup group : clientGroups) {
            row = 0;
            for (String acct : group.accountantNames()) {
                Row r = getOrCreateRow(sheet, row++);
                r.createCell(col).setCellValue(acct);
            }
            col++;
        }

        return sheet;
    }

    private void createNamedRanges(Workbook workbook, Sheet dataSheet,
                                   List<TaxTaskName> taskNames,
                                   List<ClientAccountantGroup> clientGroups) {
        LinkedHashMap<String, LinkedHashMap<String, List<String>>> hierarchy = buildHierarchy(taskNames);
        String sheetName = "_Data";

        int col = 0;

        // CategoryNames — flat list of all categories
        int catCount = hierarchy.size();
        if (catCount > 0) {
            createNamedRange(workbook, "CategoryNames", sheetName, col, 0, catCount - 1);
        }
        col++;

        // Per-category: cat_<sanitized> → its subcategories
        // Per-subcategory: sub_<sanitized> → its task names
        for (var catEntry : hierarchy.entrySet()) {
            int subCount = catEntry.getValue().size();
            if (subCount > 0) {
                createNamedRange(workbook, "cat_" + sanitize(catEntry.getKey()), sheetName, col, 0, subCount - 1);
            }
            col++;

            for (var subEntry : catEntry.getValue().entrySet()) {
                int tnCount = subEntry.getValue().size();
                if (tnCount > 0) {
                    createNamedRange(workbook, "sub_" + sanitize(subEntry.getKey()), sheetName, col, 0, tnCount - 1);
                }
                col++;
            }
        }

        // PeriodNames
        int periodCount = Period.values().length;
        if (periodCount > 0) {
            createNamedRange(workbook, "PeriodNames", sheetName, col, 0, periodCount - 1);
        }
        col++;

        // ClientNames — flat list
        int clientCount = clientGroups.size();
        if (clientCount > 0) {
            createNamedRange(workbook, "ClientNames", sheetName, col, 0, clientCount - 1);
        }
        col++;

        // Per-client: cli_<sanitized> → its accountants
        for (ClientAccountantGroup group : clientGroups) {
            int acctCount = group.accountantNames().size();
            if (acctCount > 0) {
                createNamedRange(workbook, "cli_" + sanitize(group.clientName()), sheetName, col, 0, acctCount - 1);
            }
            col++;
        }
    }

    private void createNamedRange(Workbook workbook, String name, String sheetName,
                                  int col, int firstRow, int lastRow) {
        String colLetter = columnLetter(col);
        String ref = "'" + sheetName + "'!$" + colLetter + "$" + (firstRow + 1) +
                ":$" + colLetter + "$" + (lastRow + 1);
        Name namedRange = workbook.createName();
        namedRange.setNameName(name);
        namedRange.setRefersToFormula(ref);
    }

    // --- References sheet (read-only reference for humans) ---

    private void createReferencesSheet(Workbook workbook, List<TaxTaskName> taskNames) {
        Sheet sheet = workbook.createSheet("References");
        CellStyle headerStyle = createHeaderStyle(workbook);

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

        int maxRows = Math.max(taskNames.size(), Period.values().length);
        String prevCategory = null;
        String prevSubCategory = null;

        for (int i = 0; i < maxRows; i++) {
            Row row = sheet.createRow(i + 1);

            if (i < taskNames.size()) {
                TaxTaskName tn = taskNames.get(i);
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

    // --- Clients sheet (read-only reference) ---

    private void createClientsSheet(Workbook workbook, List<ClientAccountantGroup> clientGroups) {
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

        int rowIdx = 1;
        for (ClientAccountantGroup group : clientGroups) {
            for (int j = 0; j < group.accountantNames().size(); j++) {
                Row row = sheet.createRow(rowIdx++);
                if (j == 0) {
                    row.createCell(0).setCellValue(group.clientName());
                }
                row.createCell(1).setCellValue(group.accountantNames().get(j));
            }
        }
    }

    // --- Helpers ---

    private LinkedHashMap<String, LinkedHashMap<String, List<String>>> buildHierarchy(List<TaxTaskName> taskNames) {
        LinkedHashMap<String, LinkedHashMap<String, List<String>>> hierarchy = new LinkedHashMap<>();
        for (TaxTaskName tn : taskNames) {
            String cat = tn.getSubCategory().getCategory().getName();
            String sub = tn.getSubCategory().getName();
            hierarchy.computeIfAbsent(cat, k -> new LinkedHashMap<>())
                    .computeIfAbsent(sub, k -> new ArrayList<>())
                    .add(tn.getName());
        }
        return hierarchy;
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
                .filter(Objects::nonNull)
                .sorted(Comparator.comparing(ClientAccountantGroup::clientName))
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

    private static String sanitize(String name) {
        return name.replaceAll("[^a-zA-Z0-9]", "_");
    }

    private static String columnLetter(int col) {
        StringBuilder sb = new StringBuilder();
        while (col >= 0) {
            sb.insert(0, (char) ('A' + col % 26));
            col = col / 26 - 1;
        }
        return sb.toString();
    }

    private static Row getOrCreateRow(Sheet sheet, int rowIdx) {
        Row row = sheet.getRow(rowIdx);
        return row != null ? row : sheet.createRow(rowIdx);
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
