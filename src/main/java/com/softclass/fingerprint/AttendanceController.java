package com.softclass.fingerprint;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.stage.Stage;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.List;

public class AttendanceController {

    @FXML
    private ListView<Employee> employeeList;
    @FXML
    private Label statusLabel;
    @FXML
    private Button toggleContinuousButton;
    @FXML
    private CheckBox showInactiveCheck;

    private final EmployeeController employeeController = new EmployeeController();
    private FingerprintService fingerprintService;

    private boolean continuousModeActive = false;

    @FXML
    public void initialize() {
        try {
            fingerprintService = new FingerprintService();
            refreshEmployees();
            setupContinuousButton();
        } catch (Exception e) {
            statusLabel.setText("Error init: " + e.getMessage());
        }
    }

    private void setupContinuousButton() {
        if (toggleContinuousButton != null) {
            toggleContinuousButton.setText("Activar modo continuo");
            toggleContinuousButton.setOnAction(e -> toggleContinuousMode());
        }
    }

    private void refreshEmployees() throws SQLException {
        List<Employee> employees;

        if (showInactiveCheck != null && showInactiveCheck.isSelected()) {
            employees = employeeController.getAll(); // activos + inactivos
        } else {
            employees = employeeController.getAllActive(); // solo activos
        }

        employeeList.getItems().setAll(employees);

        employeeList.setCellFactory(list -> new ListCell<>() {
            @Override
            protected void updateItem(Employee e, boolean empty) {
                super.updateItem(e, empty);
                if (empty || e == null) {
                    setText(null);
                } else {
                    String status = (e.fingerprintBase64 != null && !e.fingerprintBase64.isBlank())
                            ? "✅ Enrolado"
                            : "No Enrolado";

                    String activeMark = e.active ? "" : " 🔴 INACTIVO";
                    setText(e.name + " (" + e.document + ") - " + status + activeMark);
                }
            }
        });
    }

    @FXML
    public void onAddEmployee() {
        TextInputDialog dialog = new TextInputDialog();
        dialog.setHeaderText("Ingresa el nombre del empleado:");
        dialog.showAndWait().ifPresent(name -> {
            try {
                Employee e = new Employee();
                e.name = name;
                e.document = "N/A";
                employeeController.save(e);
                refreshEmployees();
                statusLabel.setText("Agregado: " + name);
            } catch (SQLException ex) {
                statusLabel.setText("Error de BD: " + ex.getMessage());
            }
        });
    }

    @FXML
    public void onEnroll() {
        Employee sel = employeeList.getSelectionModel().getSelectedItem();
        if (sel == null) {
            statusLabel.setText("Selecciona un empleado primero");
            return;
        }
        // detener modo continuo antes de enrolar
        stopContinuousModeIfActive();

        Stage owner = (Stage) statusLabel.getScene().getWindow();

        // Ejecutar en hilo separado para no bloquear UI
        new Thread(() -> {
            try {
                Platform.runLater(() -> {
                    statusLabel.setText("⌛ Iniciando enrolamiento...");
                    Toast.show(owner, "🚀 Iniciando: Pon el dedo 4 veces");
                });

                String tmpl = fingerprintService.enrollFingerprint(needed -> {
                    Platform.runLater(() -> {
                        statusLabel.setText("☝ Muestras restantes: " + needed);
                        Toast.show(owner, "☝ Pon el dedo: Faltan " + needed);
                    });
                });

                Platform.runLater(() -> {
                    try {
                        employeeController.updateFingerprint(sel.id, tmpl);
                        refreshEmployees();
                        statusLabel.setText("Huella enrolada para " + sel.name);
                        Toast.show(owner, "✅ Enrolamiento completado para " + sel.name);
                        new Alert(Alert.AlertType.INFORMATION, "Huella enrolada correctamente", ButtonType.OK).show();
                    } catch (Exception ex) {
                        statusLabel.setText("Error actualizando BD: " + ex.getMessage());
                    }
                });
            } catch (Exception e) {
                Platform.runLater(() -> {
                    statusLabel.setText("Error de enrolamiento: " + e.getMessage());
                    Toast.show(owner, "❌ Error: " + e.getMessage());
                });
            }
        }).start();
    }

    @FXML
    public void onCheckIn() {
        registerAttendance("IN");
    }

    @FXML
    public void onCheckOut() {
        registerAttendance("OUT");
    }

    private void registerAttendance(String type) {
        try {
            // Para verificación usamos el método que solo pide 1 muestra
            String live = fingerprintService.captureTemplateBase64();
            List<Employee> employees = employeeController.getAllActive();
            for (Employee e : employees) {
                if (e.fingerprintBase64 == null)
                    continue;
                if (fingerprintService.match(e.fingerprintBase64, live)) {
                    try (var ps = Database.get().prepareStatement(
                            "INSERT INTO attendance(employee_id, timestamp, type) VALUES(?,?,?)")) {
                        ps.setInt(1, e.id);
                        ps.setString(2, LocalDateTime.now().toString());
                        ps.setString(3, type);
                        ps.executeUpdate();
                    }
                    statusLabel.setText(type + " registrado para " + e.name);
                    Toast.show((Stage) statusLabel.getScene().getWindow(), "✅ " + type + ": " + e.name);
                    return;
                }
            }
            statusLabel.setText("No se encontró coincidencia");
        } catch (Exception ex) {
            statusLabel.setText("Error: " + ex.getMessage());
        }
    }

    // -------------------------------------------------------------------------
    // 🟢 MODO CONTINUO DE ESCUCHA
    // -------------------------------------------------------------------------

    private void toggleContinuousMode() {
        if (!continuousModeActive) {
            startContinuousMode();
        } else {
            stopContinuousModeIfActive();
        }
    }

    private void startContinuousMode() {
        continuousModeActive = true;
        toggleContinuousButton.setText("Detener modo continuo");
        statusLabel.setText("Modo continuo activo. Escuchando huellas...");

        fingerprintService.startContinuousMode(templateBase64 -> {
            Platform.runLater(() -> {
                try {
                    List<Employee> employees = employeeController.getAllActive();
                    for (Employee e : employees) {
                        if (e.fingerprintBase64 == null)
                            continue;
                        if (fingerprintService.match(e.fingerprintBase64, templateBase64)) {
                            String nextType = getNextAttendanceType(e.id);
                            saveAttendance(e.id, nextType);
                            SoundUtil.playSuccess();
                            statusLabel.setText(nextType + " registrada para " + e.name);
                            return;
                        }
                    }
                    SoundUtil.playError();
                    statusLabel.setText("Huella no registrada");
                } catch (Exception ex) {
                    SoundUtil.playError();
                    statusLabel.setText("Error: " + ex.getMessage());
                }
            });
        });
    }

    private void stopContinuousModeIfActive() {
        if (continuousModeActive) {
            continuousModeActive = false;
            fingerprintService.stopContinuousMode();
            if (toggleContinuousButton != null) {
                toggleContinuousButton.setText("Activar modo continuo");
            }
            statusLabel.setText("Modo continuo detenido");
        }
    }

    private String getNextAttendanceType(int employeeId) throws SQLException {
        try (var ps = Database.get().prepareStatement(
                "SELECT type FROM attendance WHERE employee_id = ? ORDER BY timestamp DESC LIMIT 1")) {
            ps.setInt(1, employeeId);
            try (var rs = ps.executeQuery()) {
                if (rs.next()) {
                    String lastType = rs.getString("type");
                    return "IN".equals(lastType) ? "OUT" : "IN";
                }
            }
        }
        return "IN";
    }

    private void saveAttendance(int employeeId, String type) throws SQLException {
        try (var ps = Database.get().prepareStatement(
                "INSERT INTO attendance(employee_id, timestamp, type) VALUES(?,?,?)")) {
            ps.setInt(1, employeeId);
            ps.setString(2, LocalDateTime.now().toString());
            ps.setString(3, type);
            ps.executeUpdate();
        }
    }

    // -------------------------------------------------------------------------

    @FXML
    public void onEditEmployee() {
        Employee sel = employeeList.getSelectionModel().getSelectedItem();
        if (sel == null) {
            statusLabel.setText("Selecciona un empleado para editar");
            return;
        }

        Dialog<Employee> dialog = new Dialog<>();
        dialog.setTitle("Editar Empleado");
        dialog.setHeaderText("Modificar información del empleado:");

        ButtonType saveButtonType = new ButtonType("Guardar", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(saveButtonType, ButtonType.CANCEL);

        TextField nameField = new TextField(sel.name);
        TextField docField = new TextField(sel.document);

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.add(new Label("Nombre:"), 0, 0);
        grid.add(nameField, 1, 0);
        grid.add(new Label("Documento:"), 0, 1);
        grid.add(docField, 1, 1);

        dialog.getDialogPane().setContent(grid);

        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == saveButtonType) {
                sel.name = nameField.getText();
                sel.document = docField.getText();
                return sel;
            }
            return null;
        });

        dialog.showAndWait().ifPresent(updated -> {
            try {
                employeeController.update(updated);
                refreshEmployees();
                statusLabel.setText("Actualizado: " + updated.name);
            } catch (SQLException ex) {
                statusLabel.setText("DB error: " + ex.getMessage());
            }
        });
    }

    @FXML
    public void onShowReports() {
        try {
            new ReportWindow().show();
        } catch (Exception e) {
            statusLabel.setText("Error abriendo reportes: " + e.getMessage());
        }
    }

    @FXML
    public void onToggleEmployeeActive() {
        Employee sel = employeeList.getSelectionModel().getSelectedItem();

        if (sel == null) {
            statusLabel.setText("Select employee first");
            return;
        }

        try {
            boolean newState = !sel.active;
            employeeController.setActive(sel.id, newState);

            refreshEmployees();

            statusLabel.setText(
                    "Empleado " + sel.name + (newState ? " activado" : " desactivado"));
        } catch (SQLException e) {
            statusLabel.setText("Error changing status: " + e.getMessage());
        }
    }

    @FXML
    private void onToggleShowInactive() {
        try {
            refreshEmployees();
        } catch (SQLException e) {
            statusLabel.setText("Error refreshing list: " + e.getMessage());
        }
    }

}
