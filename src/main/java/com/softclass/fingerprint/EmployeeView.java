package com.softclass.fingerprint;

import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Stage;
import javafx.application.Platform;

import java.sql.SQLException;
import java.util.List;

public class EmployeeView {

    private final EmployeeController employeeController = new EmployeeController();
    private final TableView<Employee> table = new TableView<>();
    private final TextField txtName = new TextField();
    private final TextField txtDocument = new TextField();
    private final Button btnSave = new Button("Guardar");
    private final Button btnEnroll = new Button("Capturar Huella");
    private final Button btnUpdate = new Button("Actualizar");
    private final Button btnToggleActive = new Button("Activar / Desactivar");

    private FingerprintService fingerprintService;

    @SuppressWarnings("unchecked")
    public void showWindow() {
        Stage stage = new Stage();

        try {
            Database.init();
            fingerprintService = new FingerprintService();
        } catch (Exception e) {
            showError("Error inicializando lector o base: " + e.getMessage());
            return;
        }

        // --- Formulario ---
        GridPane form = new GridPane();
        form.setPadding(new Insets(10));
        form.setHgap(10);
        form.setVgap(10);
        form.add(new Label("Nombre:"), 0, 0);
        form.add(txtName, 1, 0);
        form.add(new Label("Documento:"), 0, 1);
        form.add(txtDocument, 1, 1);
        form.add(btnSave, 0, 2);
        form.add(btnEnroll, 1, 2);
        form.add(btnUpdate, 2, 2);
        form.add(btnToggleActive, 0, 3, 3, 1);

        // --- Tabla ---
        TableColumn<Employee, String> colName = new TableColumn<>("Nombre");
        colName.setCellValueFactory(c -> new javafx.beans.property.SimpleStringProperty(c.getValue().name));

        TableColumn<Employee, String> colDoc = new TableColumn<>("Documento");
        colDoc.setCellValueFactory(c -> new javafx.beans.property.SimpleStringProperty(c.getValue().document));

        TableColumn<Employee, String> colFP = new TableColumn<>("Huella Registrada");
        colFP.setCellValueFactory(c -> new javafx.beans.property.SimpleStringProperty(
                c.getValue().fingerprintBase64 != null ? "✅ Sí" : "No"));

        TableColumn<Employee, String> colStatus = new TableColumn<>("Estado");
        colStatus.setCellValueFactory(c -> new javafx.beans.property.SimpleStringProperty(
                c.getValue().active ? "🟢 Activo" : "🔴 Inactivo"));

        table.getColumns().addAll(colName, colDoc, colFP, colStatus);

        table.setRowFactory(tv -> new TableRow<>() {
            @Override
            protected void updateItem(Employee item, boolean empty) {
                super.updateItem(item, empty);

                if (item == null || empty) {
                    setStyle("");
                } else if (!item.active) {
                    setStyle("-fx-background-color: #eeeeee; -fx-text-fill: #888888;");
                } else {
                    setStyle("");
                }
            }
        });

        refreshTable();

        btnSave.setOnAction(e -> onSave());
        btnEnroll.setOnAction(e -> onEnroll());
        btnUpdate.setOnAction(e -> onUpdate());
        btnToggleActive.setOnAction(e -> onToggleActive());

        VBox root = new VBox(10, form, table);
        root.setPadding(new Insets(10));

        stage.setTitle("Gestión de Empleados");
        stage.setScene(new Scene(root, 600, 400));

        stage.setOnCloseRequest(e -> {
            if (fingerprintService != null)
                fingerprintService.close();
        });

        stage.show();
    }

    private void onSave() {
        try {
            Employee e = new Employee();
            e.name = txtName.getText();
            e.document = txtDocument.getText();
            employeeController.save(e);
            refreshTable();
            txtName.clear();
            txtDocument.clear();
            showAlert("Empleado guardado correctamente");
        } catch (SQLException ex) {
            showError("Error al guardar empleado: " + ex.getMessage());
        }
    }

    private void onUpdate() {
        Employee selected = table.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showError("Selecciona un empleado para actualizar");
            return;
        }
        try {
            selected.name = txtName.getText();
            selected.document = txtDocument.getText();
            employeeController.update(selected);
            refreshTable();
            showAlert("Empleado actualizado correctamente");
        } catch (SQLException ex) {
            showError("Error al actualizar empleado: " + ex.getMessage());
        }
    }

    private void onEnroll() {
        Employee selected = table.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showError("Selecciona un empleado para capturar su huella");
            return;
        }

        if (!selected.active) {
            showError("No se puede capturar huella de un empleado INACTIVO");
            return;
        }

        Stage owner = (Stage) btnEnroll.getScene().getWindow();
        new Thread(() -> {
            try {
                String tmpl = fingerprintService.enrollFingerprint(needed -> {
                    Platform.runLater(() -> {
                        System.out.println("☝ Muestras restantes: " + needed);
                        Toast.show(owner, "☝ Pon el dedo: Faltan " + needed);
                    });
                });

                Platform.runLater(() -> {
                    try {
                        employeeController.updateFingerprint(selected.id, tmpl);
                        refreshTable();
                        showAlert("Huella enrolada correctamente");
                        Toast.show(owner, "✅ Enrolamiento completado");
                    } catch (Exception ex) {
                        showError("Error al actualizar la base: " + ex.getMessage());
                    }
                });
            } catch (Exception ex) {
                Platform.runLater(() -> {
                    showError("Error al enrolar huella: " + ex.getMessage());
                    Toast.show(owner, "❌ Error: " + ex.getMessage());
                });
                ex.printStackTrace();
            }
        }).start();
    }

    private void refreshTable() {
        try {
            List<Employee> list = employeeController.getAll();
            table.getItems().setAll(list);
        } catch (SQLException ex) {
            showError("Error al cargar empleados: " + ex.getMessage());
        }
    }

    private void onToggleActive() {
        Employee selected = table.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showError("Selecciona un empleado");
            return;
        }

        try {
            boolean newState = !selected.active;
            employeeController.setActive(selected.id, newState);
            refreshTable();

            showAlert("Empleado " + (newState ? "ACTIVADO" : "DESACTIVADO") + " correctamente");
        } catch (SQLException ex) {
            showError("Error cambiando estado: " + ex.getMessage());
        }
    }

    private void showAlert(String msg) {
        new Alert(Alert.AlertType.INFORMATION, msg, ButtonType.OK).showAndWait();
    }

    private void showError(String msg) {
        new Alert(Alert.AlertType.ERROR, msg, ButtonType.OK).showAndWait();
    }

}
