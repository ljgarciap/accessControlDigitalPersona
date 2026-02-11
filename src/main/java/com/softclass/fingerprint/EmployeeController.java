package com.softclass.fingerprint;

import java.sql.*;
import java.util.*;

public class EmployeeController {

    public List<Employee> getAllActive() throws SQLException {
        List<Employee> list = new ArrayList<>();
        try (PreparedStatement ps = Database.get().prepareStatement(
                "SELECT * FROM employee WHERE active = 1")) {
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                Employee e = map(rs);
                list.add(e);
            }
        }
        return list;
    }

    public List<Employee> getAll() throws SQLException {
        List<Employee> list = new ArrayList<>();
        try (Statement st = Database.get().createStatement();
             ResultSet rs = st.executeQuery("SELECT * FROM employee")) {
            while (rs.next()) {
                list.add(map(rs));
            }
        }
        return list;
    }

    public void save(Employee e) throws SQLException {
        try (PreparedStatement ps = Database.get().prepareStatement(
                "INSERT INTO employee(name, document, fingerprint) VALUES(?,?,?)")) {
            ps.setString(1, e.name);
            ps.setString(2, e.document);
            ps.setString(3, e.fingerprintBase64);
            ps.executeUpdate();
        }
    }

    public void updateFingerprint(int id, String tmpl) throws SQLException {
        try (PreparedStatement ps = Database.get().prepareStatement(
                "UPDATE employee SET fingerprint=? WHERE id=?")) {
            ps.setString(1, tmpl);
            ps.setInt(2, id);
            ps.executeUpdate();
        }
    }

    public void update(Employee e) throws SQLException {
        try (PreparedStatement ps = Database.get().prepareStatement(
                "UPDATE employee SET name=?, document=? WHERE id=?")) {
            ps.setString(1, e.name);
            ps.setString(2, e.document);
            ps.setInt(3, e.id);
            ps.executeUpdate();
        }
    }

    private Employee map(ResultSet rs) throws SQLException {
        Employee e = new Employee();
        e.id = rs.getInt("id");
        e.name = rs.getString("name");
        e.document = rs.getString("document");
        e.fingerprintBase64 = rs.getString("fingerprint");
        e.active = rs.getInt("active") == 1;
        return e;
    }

    public void setActive(int employeeId, boolean active) throws SQLException {
        try (PreparedStatement ps = Database.get().prepareStatement(
                "UPDATE employee SET active = ? WHERE id = ?")) {
            ps.setInt(1, active ? 1 : 0);
            ps.setInt(2, employeeId);
            ps.executeUpdate();
        }
    }

}
