package com.hotelmanagement.ui;

import com.hotelmanagement.model.Employee;
import com.hotelmanagement.service.EmployeeService;
import com.hotelmanagement.util.DateUtil;
import com.hotelmanagement.util.UIUtil;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Employee and staff management panel with payroll details (Restricted to ADMIN).
 */
public class EmployeePanel extends JPanel {
    private final EmployeeService employeeService;

    private JTable tblEmployees;
    private DefaultTableModel tableModel;
    private JTextField txtSearch;

    private int selectedEmployeeId = 0;
    private JTextField txtFullName;
    private JComboBox<String> cmbGender;
    private JTextField txtPhone;
    private JTextField txtEmail;
    private JTextField txtAddress;
    private JComboBox<String> cmbDesignation;
    private JTextField txtSalary;
    private JTextField txtJoiningDate;
    private JComboBox<String> cmbStatus;

    private JButton btnAdd;
    private JButton btnUpdate;
    private JButton btnDelete;
    private JButton btnClear;

    public EmployeePanel() {
        this.employeeService = new EmployeeService();
        setLayout(new BorderLayout(16, 16));
        setBackground(UIUtil.COLOR_BACKGROUND);
        setBorder(new EmptyBorder(20, 24, 20, 24));

        initComponents();
        loadEmployees();
    }

    private void initComponents() {
        // 1. Top Bar
        JPanel topPanel = new JPanel(new BorderLayout(12, 12));
        topPanel.setOpaque(false);

        JLabel lblTitle = new JLabel("Employee & Staff Management");
        lblTitle.setFont(UIUtil.FONT_HEADER);
        lblTitle.setForeground(UIUtil.COLOR_TEXT_MAIN);
        topPanel.add(lblTitle, BorderLayout.WEST);

        JPanel searchPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        searchPanel.setOpaque(false);

        txtSearch = UIUtil.createTextField(16);
        txtSearch.setToolTipText("Search by Name, Designation, or Phone");

        JButton btnSearch = UIUtil.createSecondaryButton("Search");
        btnSearch.addActionListener(e -> performSearch());

        JButton btnReset = UIUtil.createSecondaryButton("Reset");
        btnReset.addActionListener(e -> {
            txtSearch.setText("");
            loadEmployees();
        });

        searchPanel.add(new JLabel("Search:"));
        searchPanel.add(txtSearch);
        searchPanel.add(btnSearch);
        searchPanel.add(btnReset);
        topPanel.add(searchPanel, BorderLayout.EAST);

        add(topPanel, BorderLayout.NORTH);

        // 2. Table
        String[] columns = {"ID", "Full Name", "Gender", "Phone", "Email", "Designation", "Salary", "Joining Date", "Status", "Address"};
        tableModel = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) { return false; }
        };
        tblEmployees = new JTable(tableModel);
        UIUtil.styleTable(tblEmployees);
        tblEmployees.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        tblCustomersSelection();

        JScrollPane scrollPane = new JScrollPane(tblEmployees);
        scrollPane.setBorder(BorderFactory.createLineBorder(UIUtil.COLOR_BORDER, 1));
        add(scrollPane, BorderLayout.CENTER);

        // 3. Form Card
        JPanel formCard = UIUtil.createCardPanel();
        formCard.setLayout(new BorderLayout(10, 10));

        JLabel lblFormTitle = new JLabel("Staff Member Details");
        lblFormTitle.setFont(UIUtil.FONT_SUBHEADER);
        formCard.add(lblFormTitle, BorderLayout.NORTH);

        JPanel fieldsGrid = new JPanel(new GridLayout(2, 5, 12, 10));
        fieldsGrid.setOpaque(false);

        txtFullName = UIUtil.createTextField(14);
        cmbGender = UIUtil.createComboBox(new String[]{"Male", "Female", "Other"});
        txtPhone = UIUtil.createTextField(12);
        txtEmail = UIUtil.createTextField(14);
        cmbDesignation = UIUtil.createComboBox(new String[]{"Manager", "Receptionist", "Housekeeping", "Security", "Driver", "Accountant", "Other"});
        txtSalary = UIUtil.createTextField(10);
        txtJoiningDate = UIUtil.createTextField(10);
        txtJoiningDate.setText(LocalDate.now().toString());
        cmbStatus = UIUtil.createComboBox(new String[]{"ACTIVE", "INACTIVE", "ON_LEAVE"});
        txtAddress = UIUtil.createTextField(16);

        fieldsGrid.add(createFieldWrapper("Full Name *", txtFullName));
        fieldsGrid.add(createFieldWrapper("Gender *", cmbGender));
        fieldsGrid.add(createFieldWrapper("Phone Number *", txtPhone));
        fieldsGrid.add(createFieldWrapper("Email Address", txtEmail));
        fieldsGrid.add(createFieldWrapper("Designation *", cmbDesignation));
        fieldsGrid.add(createFieldWrapper("Monthly Salary (₹) *", txtSalary));
        fieldsGrid.add(createFieldWrapper("Joining Date (YYYY-MM-DD) *", txtJoiningDate));
        fieldsGrid.add(createFieldWrapper("Employment Status *", cmbStatus));
        fieldsGrid.add(createFieldWrapper("Residential Address", txtAddress));

        formCard.add(fieldsGrid, BorderLayout.CENTER);

        // Buttons
        JPanel btnBar = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 8));
        btnBar.setOpaque(false);

        btnAdd = UIUtil.createPrimaryButton("Add Staff");
        btnAdd.addActionListener(e -> saveEmployee());

        btnUpdate = UIUtil.createSecondaryButton("Update Staff");
        btnUpdate.setEnabled(false);
        btnUpdate.addActionListener(e -> updateEmployee());

        btnDelete = UIUtil.createDangerButton("Delete Staff");
        btnDelete.setEnabled(false);
        btnDelete.addActionListener(e -> deleteEmployee());

        btnClear = UIUtil.createSecondaryButton("Clear");
        btnClear.addActionListener(e -> clearForm());

        btnBar.add(btnAdd);
        btnBar.add(btnUpdate);
        btnBar.add(btnDelete);
        btnBar.add(btnClear);

        formCard.add(btnBar, BorderLayout.SOUTH);
        add(formCard, BorderLayout.SOUTH);
    }

    private void tblCustomersSelection() {
        tblEmployees.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                int row = tblEmployees.getSelectedRow();
                if (row >= 0) {
                    selectedEmployeeId = (int) tableModel.getValueAt(row, 0);
                    txtFullName.setText(String.valueOf(tableModel.getValueAt(row, 1)));
                    cmbGender.setSelectedItem(tableModel.getValueAt(row, 2));
                    txtPhone.setText(String.valueOf(tableModel.getValueAt(row, 3)));
                    txtEmail.setText(String.valueOf(tableModel.getValueAt(row, 4)));
                    cmbDesignation.setSelectedItem(tableModel.getValueAt(row, 5));
                    txtSalary.setText(String.valueOf(tableModel.getValueAt(row, 6)));
                    txtJoiningDate.setText(String.valueOf(tableModel.getValueAt(row, 7)));
                    cmbStatus.setSelectedItem(tableModel.getValueAt(row, 8));
                    txtAddress.setText(String.valueOf(tableModel.getValueAt(row, 9)));

                    btnAdd.setEnabled(false);
                    btnUpdate.setEnabled(true);
                    btnDelete.setEnabled(true);
                }
            }
        });
    }

    private JPanel createFieldWrapper(String labelText, JComponent comp) {
        JPanel p = new JPanel(new BorderLayout(0, 4));
        p.setOpaque(false);
        JLabel lbl = new JLabel(labelText);
        lbl.setFont(UIUtil.FONT_SMALL);
        lbl.setForeground(UIUtil.COLOR_TEXT_MUTED);
        p.add(lbl, BorderLayout.NORTH);
        p.add(comp, BorderLayout.CENTER);
        return p;
    }

    public void loadEmployees() {
        tableModel.setRowCount(0);
        List<Employee> list = employeeService.getAllEmployees();
        for (Employee emp : list) {
            tableModel.addRow(new Object[]{
                emp.getEmployeeId(),
                emp.getFullName(),
                emp.getGender(),
                emp.getPhone(),
                emp.getEmail() != null ? emp.getEmail() : "",
                emp.getDesignation(),
                emp.getSalary(),
                emp.getJoiningDate() != null ? emp.getJoiningDate().toString() : "",
                emp.getStatus(),
                emp.getAddress() != null ? emp.getAddress() : ""
            });
        }
    }

    private void performSearch() {
        String kw = txtSearch.getText().trim();
        tableModel.setRowCount(0);
        List<Employee> list = employeeService.searchEmployees(kw);
        for (Employee emp : list) {
            tableModel.addRow(new Object[]{
                emp.getEmployeeId(),
                emp.getFullName(),
                emp.getGender(),
                emp.getPhone(),
                emp.getEmail() != null ? emp.getEmail() : "",
                emp.getDesignation(),
                emp.getSalary(),
                emp.getJoiningDate() != null ? emp.getJoiningDate().toString() : "",
                emp.getStatus(),
                emp.getAddress() != null ? emp.getAddress() : ""
            });
        }
    }

    private void saveEmployee() {
        try {
            Employee emp = buildEmployeeFromInputs();
            if (employeeService.addEmployee(emp)) {
                UIUtil.showSuccess(this, "Employee '" + emp.getFullName() + "' added successfully!");
                clearForm();
                loadEmployees();
            }
        } catch (Exception ex) {
            UIUtil.showError(this, ex.getMessage());
        }
    }

    private void updateEmployee() {
        if (selectedEmployeeId <= 0) return;
        try {
            Employee emp = buildEmployeeFromInputs();
            emp.setEmployeeId(selectedEmployeeId);
            if (employeeService.updateEmployee(emp)) {
                UIUtil.showSuccess(this, "Employee details updated successfully!");
                clearForm();
                loadEmployees();
            }
        } catch (Exception ex) {
            UIUtil.showError(this, ex.getMessage());
        }
    }

    private void deleteEmployee() {
        if (selectedEmployeeId <= 0) return;
        if (!UIUtil.confirmAction(this, "Confirm Deletion", "Are you sure you want to delete this staff record?")) {
            return;
        }
        try {
            if (employeeService.deleteEmployee(selectedEmployeeId)) {
                UIUtil.showSuccess(this, "Employee record deleted successfully.");
                clearForm();
                loadEmployees();
            }
        } catch (Exception ex) {
            UIUtil.showError(this, ex.getMessage());
        }
    }

    private void clearForm() {
        selectedEmployeeId = 0;
        txtFullName.setText("");
        cmbGender.setSelectedIndex(0);
        txtPhone.setText("");
        txtEmail.setText("");
        cmbDesignation.setSelectedIndex(0);
        txtSalary.setText("");
        txtJoiningDate.setText(LocalDate.now().toString());
        cmbStatus.setSelectedIndex(0);
        txtAddress.setText("");

        tblEmployees.clearSelection();
        btnAdd.setEnabled(true);
        btnUpdate.setEnabled(false);
        btnDelete.setEnabled(false);
    }

    private Employee buildEmployeeFromInputs() {
        Employee emp = new Employee();
        emp.setFullName(txtFullName.getText().trim());
        emp.setGender((String) cmbGender.getSelectedItem());
        emp.setPhone(txtPhone.getText().trim());
        emp.setEmail(txtEmail.getText().trim());
        emp.setDesignation((String) cmbDesignation.getSelectedItem());

        String salStr = txtSalary.getText().trim();
        if (!salStr.isEmpty()) {
            emp.setSalary(new BigDecimal(salStr));
        }

        String jdStr = txtJoiningDate.getText().trim();
        if (!jdStr.isEmpty()) {
            emp.setJoiningDate(DateUtil.parseDate(jdStr));
        }

        emp.setStatus((String) cmbStatus.getSelectedItem());
        emp.setAddress(txtAddress.getText().trim());
        return emp;
    }
}
