package pw.ns2030.dialog;

import javax.swing.*;
import java.awt.*;

/**
 * Базовый диалог для настройки параметров устройства перед добавлением.
 * Использует Template Method для кастомизации наследниками.
 */
public abstract class DeviceConfigDialog extends JDialog {
    protected JTextField nameField;
    protected JSpinner powerSpinner;
    protected boolean confirmed = false;

    public DeviceConfigDialog(Frame owner, String title, String defaultName, double defaultPower) {
        super(owner, title, true);  // modal dialog
        
        setLayout(new BorderLayout(10, 10));
        ((JPanel) getContentPane()).setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));
        
        JPanel mainPanel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        
        // Строка 1: Название устройства
        gbc.gridx = 0; gbc.gridy = 0; gbc.weightx = 0.3;
        mainPanel.add(new JLabel("Название:"), gbc);
        
        gbc.gridx = 1; gbc.weightx = 0.7;
        nameField = new JTextField(defaultName, 20);
        mainPanel.add(nameField, gbc);
        
        // Строка 2: Мощность (Вт)
        gbc.gridx = 0; gbc.gridy = 1; gbc.weightx = 0.3;
        mainPanel.add(new JLabel("Мощность (Вт):"), gbc);
        
        gbc.gridx = 1; gbc.weightx = 0.7;
        powerSpinner = new JSpinner(new SpinnerNumberModel(defaultPower, 10.0, 10000.0, 10.0));
        JSpinner.NumberEditor editor = new JSpinner.NumberEditor(powerSpinner, "0.0");
        powerSpinner.setEditor(editor);
        mainPanel.add(powerSpinner, gbc);
        
        // Hook-метод для добавления дополнительных полей
        gbc.gridy = 2;
        addCustomFields(mainPanel, gbc);
        
        add(mainPanel, BorderLayout.CENTER);
        
        // Панель кнопок
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 5));
        
        JButton okButton = new JButton("Добавить");
        okButton.setPreferredSize(new Dimension(100, 30));
        okButton.addActionListener(e -> onOk());
        
        JButton cancelButton = new JButton("Отмена");
        cancelButton.setPreferredSize(new Dimension(100, 30));
        cancelButton.addActionListener(e -> onCancel());
        
        buttonPanel.add(okButton);
        buttonPanel.add(cancelButton);
        
        add(buttonPanel, BorderLayout.SOUTH);
        
        pack();
        setLocationRelativeTo(owner);
        setResizable(false);
    }

    /**
     * Hook-метод: добавление специфичных полей устройства.
     */
    protected void addCustomFields(JPanel panel, GridBagConstraints gbc) {
        // По умолчанию ничего не добавляем
    }

    /**
     * Hook-метод: валидация специфичных полей.
     */
    protected boolean validateCustomFields() {
        return true;
    }

    private void onOk() {
        // Валидация общих полей
        String name = nameField.getText().trim();
        if (name.isEmpty()) {
            JOptionPane.showMessageDialog(this, 
                "Название не может быть пустым!", 
                "Ошибка", 
                JOptionPane.ERROR_MESSAGE);
            return;
        }
        
        double power = (Double) powerSpinner.getValue();
        if (power < 10 || power > 10000) {
            JOptionPane.showMessageDialog(this, 
                "Мощность должна быть в диапазоне 10-10000 Вт!", 
                "Ошибка", 
                JOptionPane.ERROR_MESSAGE);
            return;
        }
        // Валидация специфичных полей
        if (!validateCustomFields()) {
            return;
        }
        
        confirmed = true;
        dispose();
    }

    private void onCancel() {
        confirmed = false;
        dispose();
    }

    public boolean isConfirmed() {
        return confirmed;
    }

    public String getDeviceName() {
        return nameField.getText().trim();
    }

    public double getPower() {
        return (Double) powerSpinner.getValue();
    }
}