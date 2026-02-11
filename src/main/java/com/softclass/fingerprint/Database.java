package com.softclass.fingerprint;

import java.sql.*;

public class Database {
    private static Connection conn;

    public static void init() throws SQLException {
        conn = DriverManager.getConnection("jdbc:sqlite:fingerprint.db");
        try (Statement st = conn.createStatement()) {
            st.executeUpdate("""
                CREATE TABLE IF NOT EXISTS employee (
                  id INTEGER PRIMARY KEY AUTOINCREMENT,
                  name TEXT,
                  document TEXT,
                  fingerprint TEXT,
                  active INTEGER DEFAULT 1
                )
            """);
            st.executeUpdate("""
                CREATE TABLE IF NOT EXISTS attendance (
                  id INTEGER PRIMARY KEY AUTOINCREMENT,
                  employee_id INTEGER,
                  timestamp TEXT,
                  type TEXT,
                  FOREIGN KEY(employee_id) REFERENCES employee(id)
                )
            """);
        }
    }

    public static Connection get() { return conn; }
}
