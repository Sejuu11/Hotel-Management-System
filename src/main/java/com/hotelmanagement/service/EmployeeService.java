package com.hotelmanagement.service;

import com.hotelmanagement.dao.EmployeeDAO;
import com.hotelmanagement.exception.ValidationException;
import com.hotelmanagement.model.Employee;
import com.hotelmanagement.util.ValidationUtil;

import java.util.List;

/**
 * Service managing employee payroll, department assignments, and staff records.
 */
public class EmployeeService {
    private final EmployeeDAO employeeDAO;

    public EmployeeService() {
        this.employeeDAO = new EmployeeDAO();
    }

    public EmployeeService(EmployeeDAO employeeDAO) {
        this.employeeDAO = employeeDAO;
    }

    public boolean addEmployee(Employee employee) {
        validateEmployee(employee);
        return employeeDAO.create(employee);
    }

    public boolean updateEmployee(Employee employee) {
        if (employee.getEmployeeId() <= 0) {
            throw new ValidationException("Employee ID is required for update.");
        }
        validateEmployee(employee);
        return employeeDAO.update(employee);
    }

    public boolean deleteEmployee(int employeeId) {
        if (employeeId <= 0) {
            throw new ValidationException("Invalid employee ID.");
        }
        return employeeDAO.delete(employeeId);
    }

    public Employee getEmployeeById(int employeeId) {
        return employeeDAO.findById(employeeId);
    }

    public List<Employee> getAllEmployees() {
        return employeeDAO.findAll();
    }

    public List<Employee> searchEmployees(String keyword) {
        if (keyword == null || keyword.trim().isEmpty()) {
            return getAllEmployees();
        }
        return employeeDAO.search(keyword.trim());
    }

    private void validateEmployee(Employee e) {
        ValidationUtil.requireNonEmpty(e.getFullName(), "Full Name");
        ValidationUtil.requireNonEmpty(e.getGender(), "Gender");
        ValidationUtil.validatePhone(e.getPhone());
        ValidationUtil.validateEmail(e.getEmail());
        ValidationUtil.requireNonEmpty(e.getDesignation(), "Designation");
        ValidationUtil.validatePositiveNumber(e.getSalary(), "Salary");
        if (e.getJoiningDate() == null) {
            throw new ValidationException("Joining Date is required.");
        }
    }
}
