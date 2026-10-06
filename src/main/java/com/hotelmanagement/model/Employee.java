package com.hotelmanagement.model;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Represents a hotel employee / staff entity.
 */
public class Employee {
    private int employeeId;
    private String fullName;
    private String gender;
    private String phone;
    private String email;
    private String address;
    private String designation;
    private BigDecimal salary;
    private LocalDate joiningDate;
    private String status; // ACTIVE, INACTIVE, ON_LEAVE

    public Employee() {}

    public Employee(int employeeId, String fullName, String gender, String phone,
                    String email, String address, String designation, BigDecimal salary,
                    LocalDate joiningDate, String status) {
        this.employeeId = employeeId;
        this.fullName = fullName;
        this.gender = gender;
        this.phone = phone;
        this.email = email;
        this.address = address;
        this.designation = designation;
        this.salary = salary;
        this.joiningDate = joiningDate;
        this.status = status;
    }

    public int getEmployeeId() {
        return employeeId;
    }

    public void setEmployeeId(int employeeId) {
        this.employeeId = employeeId;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public String getGender() {
        return gender;
    }

    public void setGender(String gender) {
        this.gender = gender;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getDesignation() {
        return designation;
    }

    public void setDesignation(String designation) {
        this.designation = designation;
    }

    public BigDecimal getSalary() {
        return salary;
    }

    public void setSalary(BigDecimal salary) {
        this.salary = salary;
    }

    public LocalDate getJoiningDate() {
        return joiningDate;
    }

    public void setJoiningDate(LocalDate joiningDate) {
        this.joiningDate = joiningDate;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    @Override
    public String toString() {
        return fullName + " (" + designation + ")";
    }
}
