package com.softclass.fingerprint;

import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import com.itextpdf.text.*;
import com.itextpdf.text.pdf.*;

import java.io.File;
import java.io.FileOutputStream;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class ReportWindow {

    private final TableView<AttendanceRecord> table = new TableView<>();
    private final ComboBox<Employee> employeeFilter = new ComboBox<>();
    private final DatePicker fromDate = new DatePicker();
    private final DatePicker toDate = new DatePicker();

    public void show() throws Exception {
        setupTable();
        setupFilters();

        VBox root = new VBox(10);
        root.setPadding(new Insets(10));
        root.getChildren().addAll(buildFilterBox(), table, buildExportButtons());

        Stage stage = new Stage();
        stage.setTitle("Reporte de Ingresos");
        stage.setScene(new Scene(root, 900, 600)); // ventana más ancha
        stage.show();

        loadEmployees();
        loadData(null, null, null);
    }

    private void setupTable() {
        TableColumn<AttendanceRecord, String> nameCol = new TableColumn<>("Usuario");
        nameCol.setCellValueFactory(new PropertyValueFactory<>("employeeName"));

        TableColumn<AttendanceRecord, String> timeCol = new TableColumn<>("Timestamp");
        timeCol.setCellValueFactory(new PropertyValueFactory<>("timestamp"));

        TableColumn<AttendanceRecord, String> typeCol = new TableColumn<>("Tipo");
        typeCol.setCellValueFactory(new PropertyValueFactory<>("type"));

        table.getColumns().addAll(nameCol, timeCol, typeCol);
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
    }

    private HBox buildFilterBox() {
        Button filterBtn = new Button("Filtrar");
        filterBtn.setOnAction(e -> {
            try {
                Employee emp = employeeFilter.getValue();
                LocalDate from = fromDate.getValue();
                LocalDate to = toDate.getValue();
                loadData(emp, from, to);
            } catch (Exception ex) {
                new Alert(Alert.AlertType.ERROR, "Error filtrando: " + ex.getMessage()).show();
            }
        });

        HBox filters = new HBox(10, new Label("Usuario:"), employeeFilter,
                new Label("Desde:"), fromDate, new Label("Hasta:"), toDate, filterBtn);
        filters.setPadding(new Insets(5));
        return filters;
    }

    private HBox buildExportButtons() {
        Button exportExcel = new Button("Exportar Excel");
        Button exportPDF = new Button("Exportar PDF");

        exportExcel.setOnAction(e -> exportToExcel());
        exportPDF.setOnAction(e -> exportToPDF());

        HBox buttons = new HBox(10, exportExcel, exportPDF);
        buttons.setPadding(new Insets(10, 0, 0, 0));
        return buttons;
    }

    private void setupFilters() {
        employeeFilter.setPromptText("All employees");
        fromDate.setPromptText("Start date");
        toDate.setPromptText("End date");
    }

    private void loadEmployees() throws SQLException {
        try (var ps = Database.get().prepareStatement("SELECT id, name, document FROM employee");
             ResultSet rs = ps.executeQuery()) {
            List<Employee> list = new ArrayList<>();
            while (rs.next()) {
                Employee e = new Employee();
                e.id = rs.getInt("id");
                e.name = rs.getString("name");
                e.document = rs.getString("document");
                list.add(e);
            }

            Employee all = new Employee();
            all.id = -1;
            all.name = "Todos los empleados";
            all.document = "";
            list.add(0, all);

            employeeFilter.getItems().setAll(list);
            employeeFilter.getSelectionModel().selectFirst();
        }
    }

    private void loadData(Employee emp, LocalDate from, LocalDate to) throws SQLException {
        StringBuilder sql = new StringBuilder("""
            SELECT e.name, e.active, a.timestamp, a.type
            FROM attendance a
            JOIN employee e ON a.employee_id = e.id
            WHERE 1=1
            """);

        List<Object> params = new ArrayList<>();

        if (emp != null && emp.id != -1) {
            sql.append(" AND e.id = ?");
            params.add(emp.id);
        }
        if (from != null) {
            sql.append(" AND datetime(a.timestamp) >= datetime(?)");
            params.add(from.atStartOfDay().toString());
        }
        if (to != null) {
            sql.append(" AND datetime(a.timestamp) <= datetime(?)");
            params.add(to.plusDays(1).atStartOfDay().toString());
        }

        sql.append(" ORDER BY a.timestamp DESC");

        try (var ps = Database.get().prepareStatement(sql.toString())) {
            for (int i = 0; i < params.size(); i++) {
                ps.setObject(i + 1, params.get(i));
            }

            try (ResultSet rs = ps.executeQuery()) {
                table.getItems().clear();
                while (rs.next()) {
                    table.getItems().add(new AttendanceRecord(
                            rs.getString("name") + (rs.getBoolean("active") ? "" : " (INACTIVO)"),
                            rs.getString("timestamp"),
                            rs.getString("type"),
                            rs.getBoolean("active")
                    ));
                }
            }
        }
    }

    // ==============================================
    // ============== EXPORTAR A EXCEL ==============
    // ==============================================
    private void exportToExcel() {

        if (table.getItems().isEmpty()) {
            new Alert(Alert.AlertType.WARNING,
                    "No hay datos para exportar").showAndWait();
            return;
        }

        try {
            FileChooser fileChooser = new FileChooser();
            fileChooser.setTitle("Generar Excel");
            fileChooser.getExtensionFilters()
                    .add(new FileChooser.ExtensionFilter("Excel (*.xlsx)", "*.xlsx"));
            fileChooser.setInitialFileName("reporte_ingresos.xlsx");

            File file = fileChooser.showSaveDialog(table.getScene().getWindow());
            if (file == null) return;

            Workbook workbook = new XSSFWorkbook();
            Sheet sheet = workbook.createSheet("Reporte");

            Font headerFont = workbook.createFont();
            headerFont.setBold(true);

            CellStyle headerStyle = workbook.createCellStyle();
            headerStyle.setFont(headerFont);

            String[] columns = {"Usuario", "Timestamp", "Tipo"};

            Row headerRow = sheet.createRow(0);
            for (int i = 0; i < columns.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(columns[i]);
                cell.setCellStyle(headerStyle);
            }

            int rowIdx = 1;
            for (AttendanceRecord rec : table.getItems()) {
                Row row = sheet.createRow(rowIdx++);
                row.createCell(0).setCellValue(rec.getEmployeeName());
                row.createCell(1).setCellValue(rec.getTimestamp());
                row.createCell(2).setCellValue(rec.getType());
            }

            for (int i = 0; i < columns.length; i++) {
                sheet.autoSizeColumn(i);
            }

            try (FileOutputStream fos = new FileOutputStream(file)) {
                workbook.write(fos);
            }

            workbook.close();

            new Alert(Alert.AlertType.INFORMATION,
                    "Excel exportado correctamente").showAndWait();

        } catch (Exception e) {
            e.printStackTrace();
            new Alert(Alert.AlertType.ERROR,
                    "Error exportando Excel:\n" + e.getMessage()).showAndWait();
        }
    }

    // =============================================
    // ============== EXPORTAR A PDF ===============
    // =============================================
    private void exportToPDF() {
        try {
            FileChooser fileChooser = new FileChooser();
            fileChooser.setTitle("Save PDF Report");
            fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("PDF Files", "*.pdf"));
            var file = fileChooser.showSaveDialog(null);
            if (file == null) return;

            com.itextpdf.text.Document document = new com.itextpdf.text.Document(PageSize.A4, 36, 36, 54, 36);
            PdfWriter.getInstance(document, new FileOutputStream(file));
            document.open();

            com.itextpdf.text.Font headerFont = com.itextpdf.text.FontFactory.getFont(
                    com.itextpdf.text.FontFactory.HELVETICA_BOLD, 14);
            com.itextpdf.text.Font cellFont = com.itextpdf.text.FontFactory.getFont(
                    com.itextpdf.text.FontFactory.HELVETICA, 10);

            document.add(new com.itextpdf.text.Paragraph("Reporte de Ingresos", headerFont));
            document.add(new com.itextpdf.text.Paragraph(" ")); // espacio

            PdfPTable pdfTable = new PdfPTable(3);
            pdfTable.setWidthPercentage(100);
            pdfTable.setWidths(new float[]{3f, 4f, 2f});

            String[] headers = {"Usuario", "Timestamp", "Tipo"};
            for (String header : headers) {
                PdfPCell cell = new PdfPCell(new com.itextpdf.text.Phrase(header, headerFont));
                cell.setBackgroundColor(BaseColor.LIGHT_GRAY);
                cell.setPadding(5);
                pdfTable.addCell(cell);
            }

            for (AttendanceRecord rec : table.getItems()) {
                pdfTable.addCell(new com.itextpdf.text.Phrase(rec.getEmployeeName(), cellFont));
                pdfTable.addCell(new com.itextpdf.text.Phrase(rec.getTimestamp(), cellFont));
                pdfTable.addCell(new com.itextpdf.text.Phrase(rec.getType(), cellFont));
            }

            document.add(pdfTable);
            document.close();

            new Alert(Alert.AlertType.INFORMATION, "PDF exportado correctamente!").show();
        } catch (Exception e) {
            new Alert(Alert.AlertType.ERROR, "Error exportando PDF: " + e.getMessage()).show();
        }
    }
}
