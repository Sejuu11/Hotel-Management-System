package com.hotelmanagement.ui;

import com.hotelmanagement.model.Customer;
import com.hotelmanagement.service.CustomerService;
import com.hotelmanagement.util.DateUtil;
import com.hotelmanagement.util.UIUtil;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.time.LocalDate;
import java.util.List;

/**
 * Customer / Guest management panel with CRUD operations and search filtering.
 */
public class CustomerPanel extends JPanel {
    private final CustomerService customerService;

    private JTable tblCustomers;
    private DefaultTableModel tableModel;
    private JTextField txtSearch;

    // Form inputs
    private int selectedCustomerId = 0;
    private JTextField txtFullName;
    private JComboBox<String> cmbGender;
    private JTextField txtDob;
    private JTextField txtPhone;
    private JTextField txtEmail;
    private JTextField txtAddress;
    private JComboBox<String> cmbIdType;
    private JTextField txtIdNumber;

    private JButton btnAdd;
    private JButton btnUpdate;
    private JButton btnDelete;
    private JButton btnClear;

    public CustomerPanel() {
        this.customerService = new CustomerService();
        setLayout(new BorderLayout(16, 16));
        setBackground(UIUtil.COLOR_BACKGROUND);
        setBorder(new EmptyBorder(20, 24, 20, 24));

        initComponents();
        loadCustomers();
    }

    private void initComponents() {
        // 1. Top Header & Search Bar
        JPanel topPanel = new JPanel(new BorderLayout(12, 12));
        topPanel.setOpaque(false);

        JLabel lblTitle = new JLabel("Customer / Guest Management");
        lblTitle.setFont(UIUtil.FONT_HEADER);
        lblTitle.setForeground(UIUtil.COLOR_TEXT_MAIN);
        topPanel.add(lblTitle, BorderLayout.WEST);

        JPanel searchPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        searchPanel.setOpaque(false);

        txtSearch = UIUtil.createTextField(16);
        txtSearch.setToolTipText("Search by Name, Phone, Email, or ID Number");

        JButton btnSearch = UIUtil.createSecondaryButton("Search");
        btnSearch.addActionListener(e -> performSearch());

        JButton btnReset = UIUtil.createSecondaryButton("Reset");
        btnReset.addActionListener(e -> {
            txtSearch.setText("");
            loadCustomers();
        });

        searchPanel.add(new JLabel("Search:"));
        searchPanel.add(txtSearch);
        searchPanel.add(btnSearch);
        searchPanel.add(btnReset);
        topPanel.add(searchPanel, BorderLayout.EAST);

        add(topPanel, BorderLayout.NORTH);

        // 2. Center Table
        String[] columns = {"ID", "Full Name", "Gender", "DOB", "Phone", "Email", "ID Type", "ID Number", "Address"};
        tableModel = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) { return false; }
        };
        tblCustomers = new JTable(tableModel);
        UIUtil.styleTable(tblCustomers);
        tblCustomers.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        tblCustomers.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                populateFormFromSelectedRow();
            }
        });

        JScrollPane scrollPane = new JScrollPane(tblCustomers);
        scrollPane.setBorder(BorderFactory.createLineBorder(UIUtil.COLOR_BORDER, 1));
        add(scrollPane, BorderLayout.CENTER);

        // 3. Bottom Form Panel
        JPanel formCard = UIUtil.createCardPanel();
        formCard.setLayout(new BorderLayout(10, 10));

        JLabel lblFormTitle = new JLabel("Guest Information Form");
        lblFormTitle.setFont(UIUtil.FONT_SUBHEADER);
        formCard.add(lblFormTitle, BorderLayout.NORTH);

        JPanel fieldsGrid = new JPanel(new GridLayout(2, 4, 12, 10));
        fieldsGrid.setOpaque(false);

        txtFullName = UIUtil.createTextField(14);
        cmbGender = UIUtil.createComboBox(new String[]{"Male", "Female", "Other"});
        txtDob = UIUtil.createTextField(10);
        txtDob.setToolTipText("YYYY-MM-DD (e.g. 1995-08-20)");
        txtPhone = UIUtil.createTextField(12);
        txtEmail = UIUtil.createTextField(14);
        cmbIdType = UIUtil.createComboBox(new String[]{"Aadhaar", "Passport", "Driving License", "Voter ID", "Other"});
        txtIdNumber = UIUtil.createTextField(12);
        txtAddress = UIUtil.createTextField(16);

        fieldsGrid.add(createFieldWrapper("Full Name *", txtFullName));
        fieldsGrid.add(createFieldWrapper("Gender *", cmbGender));
        fieldsGrid.add(createFieldWrapper("Date of Birth (YYYY-MM-DD)", txtDob));
        fieldsGrid.add(createFieldWrapper("Phone Number *", txtPhone));
        fieldsGrid.add(createFieldWrapper("Email Address", txtEmail));
        fieldsGrid.add(createFieldWrapper("ID Type *", cmbIdType));
        fieldsGrid.add(createFieldWrapper("ID Proof Number *", txtIdNumber));
        fieldsGrid.add(createFieldWrapper("Address", txtAddress));

        formCard.add(fieldsGrid, BorderLayout.CENTER);

        // Button Toolbar
        JPanel btnBar = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 8));
        btnBar.setOpaque(false);

        btnAdd = UIUtil.createPrimaryButton("Add Guest");
        btnAdd.addActionListener(e -> saveCustomer());

        btnUpdate = UIUtil.createSecondaryButton("Update Guest");
        btnUpdate.setEnabled(false);
        btnUpdate.addActionListener(e -> updateCustomer());

        btnDelete = UIUtil.createDangerButton("Delete Guest");
        btnDelete.setEnabled(false);
        btnDelete.addActionListener(e -> deleteCustomer());

        btnClear = UIUtil.createSecondaryButton("Clear Fields");
        btnClear.addActionListener(e -> clearForm());

        btnBar.add(btnAdd);
        btnBar.add(btnUpdate);
        btnBar.add(btnDelete);
        btnBar.add(btnClear);

        formCard.add(btnBar, BorderLayout.SOUTH);
        add(formCard, BorderLayout.SOUTH);
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

    public void loadCustomers() {
        tableModel.setRowCount(0);
        List<Customer> list = customerService.getAllCustomers();
        for (Customer c : list) {
            tableModel.addRow(new Object[]{
                c.getCustomerId(),
                c.getFullName(),
                c.getGender(),
                c.getDateOfBirth() != null ? c.getDateOfBirth().toString() : "",
                c.getPhone(),
                c.getEmail() != null ? c.getEmail() : "",
                c.getIdType(),
                c.getIdNumber(),
                c.getAddress() != null ? c.getAddress() : ""
            });
        }
    }

    private void performSearch() {
        String kw = txtSearch.getText().trim();
        tableModel.setRowCount(0);
        List<Customer> list = customerService.searchCustomers(kw);
        for (Customer c : list) {
            tableModel.addRow(new Object[]{
                c.getCustomerId(),
                c.getFullName(),
                c.getGender(),
                c.getDateOfBirth() != null ? c.getDateOfBirth().toString() : "",
                c.getPhone(),
                c.getEmail() != null ? c.getEmail() : "",
                c.getIdType(),
                c.getIdNumber(),
                c.getAddress() != null ? c.getAddress() : ""
            });
        }
    }

    private void populateFormFromSelectedRow() {
        int row = tblCustomers.getSelectedRow();
        if (row >= 0) {
            selectedCustomerId = (int) tableModel.getValueAt(row, 0);
            txtFullName.setText(String.valueOf(tableModel.getValueAt(row, 1)));
            cmbGender.setSelectedItem(tableModel.getValueAt(row, 2));
            txtDob.setText(String.valueOf(tableModel.getValueAt(row, 3)));
            txtPhone.setText(String.valueOf(tableModel.getValueAt(row, 4)));
            txtEmail.setText(String.valueOf(tableModel.getValueAt(row, 5)));
            cmbIdType.setSelectedItem(tableModel.getValueAt(row, 6));
            txtIdNumber.setText(String.valueOf(tableModel.getValueAt(row, 7)));
            txtAddress.setText(String.valueOf(tableModel.getValueAt(row, 8)));

            btnAdd.setEnabled(false);
            btnUpdate.setEnabled(true);
            btnDelete.setEnabled(true);
        }
    }

    private void saveCustomer() {
        try {
            Customer c = buildCustomerFromInputs();
            if (customerService.addCustomer(c)) {
                UIUtil.showSuccess(this, "Guest '" + c.getFullName() + "' added successfully!");
                clearForm();
                loadCustomers();
            }
        } catch (Exception ex) {
            UIUtil.showError(this, ex.getMessage());
        }
    }

    private void updateCustomer() {
        if (selectedCustomerId <= 0) return;
        try {
            Customer c = buildCustomerFromInputs();
            c.setCustomerId(selectedCustomerId);
            if (customerService.updateCustomer(c)) {
                UIUtil.showSuccess(this, "Guest details updated successfully!");
                clearForm();
                loadCustomers();
            }
        } catch (Exception ex) {
            UIUtil.showError(this, ex.getMessage());
        }
    }

    private void deleteCustomer() {
        if (selectedCustomerId <= 0) return;
        if (!UIUtil.confirmAction(this, "Confirm Deletion", "Are you sure you want to delete this guest?")) {
            return;
        }
        try {
            if (customerService.deleteCustomer(selectedCustomerId)) {
                UIUtil.showSuccess(this, "Guest record deleted successfully.");
                clearForm();
                loadCustomers();
            }
        } catch (Exception ex) {
            UIUtil.showError(this, ex.getMessage());
        }
    }

    private void clearForm() {
        selectedCustomerId = 0;
        txtFullName.setText("");
        cmbGender.setSelectedIndex(0);
        txtDob.setText("");
        txtPhone.setText("");
        txtEmail.setText("");
        txtAddress.setText("");
        cmbIdType.setSelectedIndex(0);
        txtIdNumber.setText("");

        tblCustomers.clearSelection();
        btnAdd.setEnabled(true);
        btnUpdate.setEnabled(false);
        btnDelete.setEnabled(false);
    }

    private Customer buildCustomerFromInputs() {
        Customer c = new Customer();
        c.setFullName(txtFullName.getText().trim());
        c.setGender((String) cmbGender.getSelectedItem());

        String dobStr = txtDob.getText().trim();
        if (!dobStr.isEmpty()) {
            LocalDate dob = DateUtil.parseDate(dobStr);
            c.setDateOfBirth(dob);
        }
        c.setPhone(txtPhone.getText().trim());
        c.setEmail(txtEmail.getText().trim());
        c.setIdType((String) cmbIdType.getSelectedItem());
        c.setIdNumber(txtIdNumber.getText().trim());
        c.setAddress(txtAddress.getText().trim());
        return c;
    }
}
