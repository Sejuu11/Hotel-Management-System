package com.hotelmanagement.ui;

import com.hotelmanagement.model.Room;
import com.hotelmanagement.service.RoomService;
import com.hotelmanagement.util.UIUtil;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.math.BigDecimal;
import java.util.List;

/**
 * Room inventory management panel with color-coded status badges and filters.
 */
public class RoomPanel extends JPanel {
    private final RoomService roomService;

    private JTable tblRooms;
    private DefaultTableModel tableModel;
    private JTextField txtSearch;
    private JComboBox<String> cmbFilterType;
    private JComboBox<String> cmbFilterStatus;

    private int selectedRoomId = 0;
    private JTextField txtRoomNumber;
    private JComboBox<String> cmbRoomType;
    private JTextField txtFloor;
    private JTextField txtPrice;
    private JComboBox<String> cmbStatus;
    private JTextField txtDescription;

    private JButton btnAdd;
    private JButton btnUpdate;
    private JButton btnDelete;
    private JButton btnClear;

    public RoomPanel() {
        this.roomService = new RoomService();
        setLayout(new BorderLayout(16, 16));
        setBackground(UIUtil.COLOR_BACKGROUND);
        setBorder(new EmptyBorder(20, 24, 20, 24));

        initComponents();
        loadRooms();
    }

    private void initComponents() {
        // 1. Top Bar & Filters
        JPanel topPanel = new JPanel(new BorderLayout(12, 12));
        topPanel.setOpaque(false);

        JLabel lblTitle = new JLabel("Room Inventory Management");
        lblTitle.setFont(UIUtil.FONT_HEADER);
        lblTitle.setForeground(UIUtil.COLOR_TEXT_MAIN);
        topPanel.add(lblTitle, BorderLayout.WEST);

        JPanel filterPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        filterPanel.setOpaque(false);

        txtSearch = UIUtil.createTextField(10);
        txtSearch.setToolTipText("Search Room # or Description");

        cmbFilterType = UIUtil.createComboBox(new String[]{"All", "Single", "Double", "Deluxe", "Suite"});
        cmbFilterStatus = UIUtil.createComboBox(new String[]{"All", "AVAILABLE", "RESERVED", "OCCUPIED", "MAINTENANCE"});

        JButton btnFilter = UIUtil.createSecondaryButton("Filter");
        btnFilter.addActionListener(e -> performFilter());

        JButton btnReset = UIUtil.createSecondaryButton("Reset");
        btnReset.addActionListener(e -> {
            txtSearch.setText("");
            cmbFilterType.setSelectedIndex(0);
            cmbFilterStatus.setSelectedIndex(0);
            loadRooms();
        });

        filterPanel.add(new JLabel("Search:"));
        filterPanel.add(txtSearch);
        filterPanel.add(new JLabel("Type:"));
        filterPanel.add(cmbFilterType);
        filterPanel.add(new JLabel("Status:"));
        filterPanel.add(cmbFilterStatus);
        filterPanel.add(btnFilter);
        filterPanel.add(btnReset);
        topPanel.add(filterPanel, BorderLayout.EAST);

        add(topPanel, BorderLayout.NORTH);

        // 2. Center Table with Custom Status Renderer
        String[] columns = {"ID", "Room #", "Type", "Floor", "Price / Night (₹)", "Status", "Description"};
        tableModel = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int col) { return false; }
        };
        tblRooms = new JTable(tableModel);
        UIUtil.styleTable(tblRooms);
        tblRooms.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        // Custom status badge renderer
        tblRooms.getColumnModel().getColumn(5).setCellRenderer(new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object val, boolean isSel, boolean hasFocus, int r, int c) {
                JLabel lbl = (JLabel) super.getTableCellRendererComponent(table, val, isSel, hasFocus, r, c);
                lbl.setHorizontalAlignment(SwingConstants.CENTER);
                lbl.setFont(UIUtil.FONT_BOLD);
                String status = String.valueOf(val);

                if (!isSel) {
                    if ("AVAILABLE".equalsIgnoreCase(status)) {
                        lbl.setForeground(new Color(16, 185, 129));
                    } else if ("RESERVED".equalsIgnoreCase(status)) {
                        lbl.setForeground(new Color(59, 130, 246));
                    } else if ("OCCUPIED".equalsIgnoreCase(status)) {
                        lbl.setForeground(new Color(245, 158, 11));
                    } else if ("MAINTENANCE".equalsIgnoreCase(status)) {
                        lbl.setForeground(new Color(239, 68, 68));
                    }
                }
                return lbl;
            }
        });

        tblRooms.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                populateFormFromSelectedRow();
            }
        });

        JScrollPane scrollPane = new JScrollPane(tblRooms);
        scrollPane.setBorder(BorderFactory.createLineBorder(UIUtil.COLOR_BORDER, 1));
        add(scrollPane, BorderLayout.CENTER);

        // 3. Form Card
        JPanel formCard = UIUtil.createCardPanel();
        formCard.setLayout(new BorderLayout(10, 10));

        JLabel lblFormTitle = new JLabel("Room Details");
        lblFormTitle.setFont(UIUtil.FONT_SUBHEADER);
        formCard.add(lblFormTitle, BorderLayout.NORTH);

        JPanel fieldsGrid = new JPanel(new GridLayout(2, 3, 16, 10));
        fieldsGrid.setOpaque(false);

        txtRoomNumber = UIUtil.createTextField(10);
        cmbRoomType = UIUtil.createComboBox(new String[]{"Single", "Double", "Deluxe", "Suite"});
        txtFloor = UIUtil.createTextField(6);
        txtPrice = UIUtil.createTextField(10);
        cmbStatus = UIUtil.createComboBox(new String[]{"AVAILABLE", "RESERVED", "OCCUPIED", "MAINTENANCE"});
        txtDescription = UIUtil.createTextField(20);

        fieldsGrid.add(createFieldWrapper("Room Number *", txtRoomNumber));
        fieldsGrid.add(createFieldWrapper("Room Type *", cmbRoomType));
        fieldsGrid.add(createFieldWrapper("Floor Number *", txtFloor));
        fieldsGrid.add(createFieldWrapper("Price Per Night (₹) *", txtPrice));
        fieldsGrid.add(createFieldWrapper("Room Status *", cmbStatus));
        fieldsGrid.add(createFieldWrapper("Description / Amenities", txtDescription));

        formCard.add(fieldsGrid, BorderLayout.CENTER);

        // Buttons
        JPanel btnBar = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 8));
        btnBar.setOpaque(false);

        btnAdd = UIUtil.createPrimaryButton("Add Room");
        btnAdd.addActionListener(e -> saveRoom());

        btnUpdate = UIUtil.createSecondaryButton("Update Room");
        btnUpdate.setEnabled(false);
        btnUpdate.addActionListener(e -> updateRoom());

        btnDelete = UIUtil.createDangerButton("Delete Room");
        btnDelete.setEnabled(false);
        btnDelete.addActionListener(e -> deleteRoom());

        btnClear = UIUtil.createSecondaryButton("Clear");
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

    public void loadRooms() {
        tableModel.setRowCount(0);
        List<Room> list = roomService.getAllRooms();
        for (Room r : list) {
            tableModel.addRow(new Object[]{
                r.getRoomId(),
                r.getRoomNumber(),
                r.getRoomType(),
                r.getFloor(),
                r.getPricePerNight(),
                r.getStatus(),
                r.getDescription() != null ? r.getDescription() : ""
            });
        }
    }

    private void performFilter() {
        String kw = txtSearch.getText().trim();
        String type = (String) cmbFilterType.getSelectedItem();
        String status = (String) cmbFilterStatus.getSelectedItem();

        tableModel.setRowCount(0);
        List<Room> list = roomService.searchRooms(kw, type, status);
        for (Room r : list) {
            tableModel.addRow(new Object[]{
                r.getRoomId(),
                r.getRoomNumber(),
                r.getRoomType(),
                r.getFloor(),
                r.getPricePerNight(),
                r.getStatus(),
                r.getDescription() != null ? r.getDescription() : ""
            });
        }
    }

    private void populateFormFromSelectedRow() {
        int row = tblRooms.getSelectedRow();
        if (row >= 0) {
            selectedRoomId = (int) tableModel.getValueAt(row, 0);
            txtRoomNumber.setText(String.valueOf(tableModel.getValueAt(row, 1)));
            cmbRoomType.setSelectedItem(tableModel.getValueAt(row, 2));
            txtFloor.setText(String.valueOf(tableModel.getValueAt(row, 3)));
            txtPrice.setText(String.valueOf(tableModel.getValueAt(row, 4)));
            cmbStatus.setSelectedItem(tableModel.getValueAt(row, 5));
            txtDescription.setText(String.valueOf(tableModel.getValueAt(row, 6)));

            btnAdd.setEnabled(false);
            btnUpdate.setEnabled(true);
            btnDelete.setEnabled(true);
        }
    }

    private void saveRoom() {
        try {
            Room r = buildRoomFromInputs();
            if (roomService.addRoom(r)) {
                UIUtil.showSuccess(this, "Room " + r.getRoomNumber() + " added successfully!");
                clearForm();
                loadRooms();
            }
        } catch (Exception ex) {
            UIUtil.showError(this, ex.getMessage());
        }
    }

    private void updateRoom() {
        if (selectedRoomId <= 0) return;
        try {
            Room r = buildRoomFromInputs();
            r.setRoomId(selectedRoomId);
            if (roomService.updateRoom(r)) {
                UIUtil.showSuccess(this, "Room " + r.getRoomNumber() + " updated successfully!");
                clearForm();
                loadRooms();
            }
        } catch (Exception ex) {
            UIUtil.showError(this, ex.getMessage());
        }
    }

    private void deleteRoom() {
        if (selectedRoomId <= 0) return;
        if (!UIUtil.confirmAction(this, "Confirm Deletion", "Are you sure you want to delete this room?")) {
            return;
        }
        try {
            if (roomService.deleteRoom(selectedRoomId)) {
                UIUtil.showSuccess(this, "Room deleted successfully.");
                clearForm();
                loadRooms();
            }
        } catch (Exception ex) {
            UIUtil.showError(this, ex.getMessage());
        }
    }

    private void clearForm() {
        selectedRoomId = 0;
        txtRoomNumber.setText("");
        cmbRoomType.setSelectedIndex(0);
        txtFloor.setText("");
        txtPrice.setText("");
        cmbStatus.setSelectedIndex(0);
        txtDescription.setText("");

        tblRooms.clearSelection();
        btnAdd.setEnabled(true);
        btnUpdate.setEnabled(false);
        btnDelete.setEnabled(false);
    }

    private Room buildRoomFromInputs() {
        Room r = new Room();
        r.setRoomNumber(txtRoomNumber.getText().trim());
        r.setRoomType((String) cmbRoomType.getSelectedItem());

        String floorStr = txtFloor.getText().trim();
        if (!floorStr.isEmpty()) {
            r.setFloor(Integer.parseInt(floorStr));
        }

        String priceStr = txtPrice.getText().trim();
        if (!priceStr.isEmpty()) {
            r.setPricePerNight(new BigDecimal(priceStr));
        }

        r.setStatus((String) cmbStatus.getSelectedItem());
        r.setDescription(txtDescription.getText().trim());
        return r;
    }
}
