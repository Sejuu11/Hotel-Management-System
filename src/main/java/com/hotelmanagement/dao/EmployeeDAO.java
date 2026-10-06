package com.hotelmanagement.dao;

import com.hotelmanagement.database.DatabaseConnection;
import com.hotelmanagement.exception.DatabaseException;
import com.hotelmanagement.model.Employee;
import com.hotelmanagement.util.DateUtil;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Data Access Object for employees table.
 */
public class EmployeeDAO {

    public boolean create(Employee employee) {
        String sql = "INSERT INTO employees (full_name, gender, phone, email, address, designation, salary, joining_date, status) " +
                     "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, employee.getFullName());
            ps.setString(2, employee.getGender());
            ps.setString(3, employee.getPhone());
            ps.setString(4, employee.getEmail());
            ps.setString(5, employee.getAddress());
            ps.setString(6, employee.getDesignation());
            ps.setBigDecimal(7, employee.getSalary());
            ps.setDate(8, DateUtil.toSqlDate(employee.getJoiningDate()));
            ps.setString(9, employee.getStatus());

            int affected = ps.executeUpdate();
            if (affected > 0) {
                try (ResultSet rs = ps.getGeneratedKeys()) {
                    if (rs.next()) {
                        employee.setEmployeeId(rs.getInt(1));
                    }
                }
                return true;
            }
        } catch (SQLException e) {
            throw new DatabaseException("Failed to insert employee: " + e.getMessage(), e);
        }
        return false;
    }

    public boolean update(Employee employee) {
        String sql = "UPDATE employees SET full_name = ?, gender = ?, phone = ?, email = ?, " +
                     "address = ?, designation = ?, salary = ?, joining_date = ?, status = ? WHERE employee_id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, employee.getFullName());
            ps.setString(2, employee.getGender());
            ps.setString(3, employee.getPhone());
            ps.setString(4, employee.getEmail());
            ps.setString(5, employee.getAddress());
            ps.setString(6, employee.getDesignation());
            ps.setBigDecimal(7, employee.getSalary());
            ps.setDate(8, DateUtil.toSqlDate(employee.getJoiningDate()));
            ps.setString(9, employee.getStatus());
            ps.setInt(10, employee.getEmployeeId());

            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new DatabaseException("Failed to update employee: " + e.getMessage(), e);
        }
    }

    public boolean delete(int employeeId) {
        String sql = "DELETE FROM employees WHERE employee_id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, employeeId);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new DatabaseException("Failed to delete employee: " + e.getMessage(), e);
        }
    }

    public Employee findById(int employeeId) {
        String sql = "SELECT * FROM employees WHERE employee_id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, employeeId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapRow(rs);
                }
            }
        } catch (SQLException e) {
            throw new DatabaseException("Failed to find employee by ID: " + employeeId, e);
        }
        return null;
    }

    public List<Employee> findAll() {
        List<Employee> list = new ArrayList<>();
        String sql = "SELECT * FROM employees ORDER BY employee_id DESC";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                list.add(mapRow(rs));
            }
        } catch (SQLException e) {
            throw new DatabaseException("Failed to retrieve employees", e);
        }
        return list;
    }

    public List<Employee> search(String keyword) {
        List<Employee> list = new ArrayList<>();
        String sql = "SELECT * FROM employees WHERE full_name LIKE ? OR designation LIKE ? OR phone LIKE ? " +
                     "ORDER BY employee_id DESC";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            String pattern = "%" + (keyword == null ? "" : keyword.trim()) + "%";
            ps.setString(1, pattern);
            ps.setString(2, pattern);
            ps.setString(3, pattern);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapRow(rs));
                }
            }
        } catch (SQLException e) {
            throw new DatabaseException("Employee search query failed", e);
        }
        return list;
    }

    private Employee mapRow(ResultSet rs) throws SQLException {
        Date jDate = rs.getDate("joining_date");
        return new Employee(
            rs.getInt("employee_id"),
            rs.getString("full_name"),
            rs.getString("gender"),
            rs.getString("phone"),
            rs.getString("email"),
            rs.getString("address"),
            rs.getString("designation"),
            rs.getBigDecimal("salary"),
            jDate != null ? jDate.toLocalDate() : null,
            rs.getString("status")
        );
    }
}
