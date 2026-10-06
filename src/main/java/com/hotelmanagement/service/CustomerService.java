package com.hotelmanagement.service;

import com.hotelmanagement.dao.CustomerDAO;
import com.hotelmanagement.exception.ValidationException;
import com.hotelmanagement.model.Customer;
import com.hotelmanagement.util.ValidationUtil;

import java.util.List;

/**
 * Service managing customer business logic, input validation, and CRUD operations.
 */
public class CustomerService {
    private final CustomerDAO customerDAO;

    public CustomerService() {
        this.customerDAO = new CustomerDAO();
    }

    public CustomerService(CustomerDAO customerDAO) {
        this.customerDAO = customerDAO;
    }

    public boolean addCustomer(Customer customer) {
        validateCustomer(customer);
        return customerDAO.create(customer);
    }

    public boolean updateCustomer(Customer customer) {
        if (customer.getCustomerId() <= 0) {
            throw new ValidationException("Customer ID is required for update.");
        }
        validateCustomer(customer);
        return customerDAO.update(customer);
    }

    public boolean deleteCustomer(int customerId) {
        if (customerId <= 0) {
            throw new ValidationException("Invalid customer ID.");
        }
        if (customerDAO.hasBookings(customerId)) {
            throw new ValidationException("Cannot delete customer: Guest has associated booking records in the system.");
        }
        return customerDAO.delete(customerId);
    }

    public Customer getCustomerById(int customerId) {
        return customerDAO.findById(customerId);
    }

    public List<Customer> getAllCustomers() {
        return customerDAO.findAll();
    }

    public List<Customer> searchCustomers(String keyword) {
        if (keyword == null || keyword.trim().isEmpty()) {
            return getAllCustomers();
        }
        return customerDAO.search(keyword.trim());
    }

    private void validateCustomer(Customer c) {
        ValidationUtil.requireNonEmpty(c.getFullName(), "Full Name");
        ValidationUtil.requireNonEmpty(c.getGender(), "Gender");
        ValidationUtil.validatePhone(c.getPhone());
        ValidationUtil.validateEmail(c.getEmail());
        ValidationUtil.requireNonEmpty(c.getIdType(), "ID Type");
        ValidationUtil.requireNonEmpty(c.getIdNumber(), "ID Number");
    }
}
