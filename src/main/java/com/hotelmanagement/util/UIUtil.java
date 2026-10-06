package com.hotelmanagement.util;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.JTableHeader;
import java.awt.*;

/**
 * UI styling and component factory utility for a clean, modern desktop experience.
 */
public class UIUtil {

    // Modern color palette
    public static final Color COLOR_PRIMARY = new Color(15, 23, 42);       // Dark Slate (Sidebar / Header)
    public static final Color COLOR_PRIMARY_LIGHT = new Color(30, 41, 59); // Hover State
    public static final Color COLOR_ACCENT = new Color(14, 165, 233);      // Sky Blue (Buttons, Active items)
    public static final Color COLOR_ACCENT_HOVER = new Color(2, 132, 199);
    public static final Color COLOR_BACKGROUND = new Color(248, 250, 252); // Canvas Light Gray
    public static final Color COLOR_CARD = Color.WHITE;
    public static final Color COLOR_BORDER = new Color(226, 232, 240);
    public static final Color COLOR_TEXT_MAIN = new Color(30, 41, 59);
    public static final Color COLOR_TEXT_MUTED = new Color(100, 116, 139);
    public static final Color COLOR_SUCCESS = new Color(16, 185, 129);
    public static final Color COLOR_WARNING = new Color(245, 158, 11);
    public static final Color COLOR_DANGER = new Color(239, 68, 68);

    // Standard Fonts
    public static final Font FONT_HEADER = new Font("Segoe UI", Font.BOLD, 20);
    public static final Font FONT_SUBHEADER = new Font("Segoe UI", Font.BOLD, 15);
    public static final Font FONT_BOLD = new Font("Segoe UI", Font.BOLD, 13);
    public static final Font FONT_REGULAR = new Font("Segoe UI", Font.PLAIN, 13);
    public static final Font FONT_SMALL = new Font("Segoe UI", Font.PLAIN, 11);
    public static final Font FONT_CARD_VALUE = new Font("Segoe UI", Font.BOLD, 26);

    public static JButton createPrimaryButton(String text) {
        return styleButton(new JButton(text), COLOR_ACCENT, Color.WHITE);
    }

    public static JButton createSuccessButton(String text) {
        return styleButton(new JButton(text), COLOR_SUCCESS, Color.WHITE);
    }

    public static JButton createDangerButton(String text) {
        return styleButton(new JButton(text), COLOR_DANGER, Color.WHITE);
    }

    public static JButton createSecondaryButton(String text) {
        JButton btn = styleButton(new JButton(text), new Color(241, 245, 249), COLOR_TEXT_MAIN);
        btn.setBorder(BorderFactory.createCompoundBorder(
            new LineBorder(COLOR_BORDER, 1),
            new EmptyBorder(7, 14, 7, 14)
        ));
        return btn;
    }

    public static JButton styleButton(JButton btn, Color bg, Color fg) {
        btn.setFont(FONT_BOLD);
        btn.setBackground(bg);
        btn.setForeground(fg);
        btn.setFocusPainted(false);
        btn.setBorderPainted(false);
        btn.setOpaque(true);
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btn.setBorder(new EmptyBorder(8, 16, 8, 16));
        return btn;
    }

    public static JTextField createTextField(int columns) {
        JTextField tf = new JTextField(columns);
        tf.setFont(FONT_REGULAR);
        tf.setBorder(BorderFactory.createCompoundBorder(
            new LineBorder(COLOR_BORDER, 1),
            new EmptyBorder(6, 8, 6, 8)
        ));
        return tf;
    }

    public static JPasswordField createPasswordField(int columns) {
        JPasswordField pf = new JPasswordField(columns);
        pf.setFont(FONT_REGULAR);
        pf.setBorder(BorderFactory.createCompoundBorder(
            new LineBorder(COLOR_BORDER, 1),
            new EmptyBorder(6, 8, 6, 8)
        ));
        return pf;
    }

    public static <T> JComboBox<T> createComboBox(T[] items) {
        JComboBox<T> cb = new JComboBox<>(items);
        cb.setFont(FONT_REGULAR);
        cb.setBackground(Color.WHITE);
        return cb;
    }

    public static JPanel createCardPanel() {
        JPanel card = new JPanel();
        card.setBackground(COLOR_CARD);
        card.setBorder(BorderFactory.createCompoundBorder(
            new LineBorder(COLOR_BORDER, 1),
            new EmptyBorder(14, 14, 14, 14)
        ));
        return card;
    }

    public static void styleTable(JTable table) {
        table.setFont(FONT_REGULAR);
        table.setRowHeight(32);
        table.setShowGrid(false);
        table.setIntercellSpacing(new Dimension(0, 0));
        table.setSelectionBackground(new Color(224, 242, 254));
        table.setSelectionForeground(COLOR_PRIMARY);

        JTableHeader header = table.getTableHeader();
        header.setFont(FONT_BOLD);
        header.setBackground(new Color(241, 245, 249));
        header.setForeground(COLOR_TEXT_MAIN);
        header.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, COLOR_BORDER));
        header.setPreferredSize(new Dimension(header.getWidth(), 36));

        // Alternate row colors
        table.setDefaultRenderer(Object.class, new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable t, Object val, boolean isSel, boolean hasFocus, int r, int c) {
                Component comp = super.getTableCellRendererComponent(t, val, isSel, hasFocus, r, c);
                if (!isSel) {
                    comp.setBackground(r % 2 == 0 ? Color.WHITE : new Color(248, 250, 252));
                    comp.setForeground(COLOR_TEXT_MAIN);
                }
                setBorder(new EmptyBorder(0, 8, 0, 8));
                return comp;
            }
        });
    }

    public static void showError(Component parent, String message) {
        JOptionPane.showMessageDialog(
            parent,
            message,
            "Error",
            JOptionPane.ERROR_MESSAGE
        );
    }

    public static void showSuccess(Component parent, String message) {
        JOptionPane.showMessageDialog(
            parent,
            message,
            "Success",
            JOptionPane.INFORMATION_MESSAGE
        );
    }

    public static void showWarning(Component parent, String message) {
        JOptionPane.showMessageDialog(
            parent,
            message,
            "Validation Warning",
            JOptionPane.WARNING_MESSAGE
        );
    }

    public static boolean confirmAction(Component parent, String title, String message) {
        int res = JOptionPane.showConfirmDialog(
            parent,
            message,
            title,
            JOptionPane.YES_NO_OPTION,
            JOptionPane.QUESTION_MESSAGE
        );
        return res == JOptionPane.YES_OPTION;
    }
}
