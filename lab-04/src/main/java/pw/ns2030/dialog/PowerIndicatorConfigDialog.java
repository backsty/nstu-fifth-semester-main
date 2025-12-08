package pw.ns2030.dialog;

import pw.ns2030.component.LevelIndicatorConfig;

import javax.swing.*;
import javax.swing.text.DefaultFormatterFactory;
import javax.swing.text.NumberFormatter;
import java.awt.*;
import java.text.DecimalFormat;

/**
 * Диалог настройки индикатора потребления энергии.
 * Позволяет изменять максимальную мощность и границы зон.
 */
public class PowerIndicatorConfigDialog extends JDialog {
    private JSpinner maxPowerSpinner;
    private JSpinner criticalLowSpinner;
    private JSpinner criticalHighSpinner;
    private JSpinner warningLowSpinner;
    private JSpinner warningHighSpinner;
    
    private boolean confirmed = false;

    public PowerIndicatorConfigDialog(Frame owner, LevelIndicatorConfig currentConfig) {
        super(owner, "Настройка индикатора потребления", true);
        
        setLayout(new BorderLayout(10, 10));
        ((JPanel) getContentPane()).setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));
        
        JPanel mainPanel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        
        // Строка 0: Максимальная мощность
        gbc.gridx = 0; gbc.gridy = 0; gbc.weightx = 0.4;
        mainPanel.add(new JLabel("Макс. мощность (Вт):"), gbc);
        
        gbc.gridx = 1; gbc.weightx = 0.6;
        maxPowerSpinner = createFormattedSpinner(currentConfig.getMaxValue(), 1000.0, 50000.0, 100.0);
        mainPanel.add(maxPowerSpinner, gbc);
        
        // Разделитель
        gbc.gridx = 0; gbc.gridy = 1; gbc.gridwidth = 2;
        JSeparator separator1 = new JSeparator();
        separator1.setPreferredSize(new Dimension(300, 10));
        mainPanel.add(separator1, gbc);
        gbc.gridwidth = 1;
        
        // Информационная строка
        gbc.gridx = 0; gbc.gridy = 2; gbc.gridwidth = 2;
        JLabel infoLabel = new JLabel("<html><b>Границы зон (в Ваттах):</b></html>");
        mainPanel.add(infoLabel, gbc);
        gbc.gridwidth = 1;
        
        // Строка 3: Критическая зона (низ)
        gbc.gridx = 0; gbc.gridy = 3; gbc.weightx = 0.4;
        mainPanel.add(new JLabel("Критич. зона (низ):"), gbc);
        
        gbc.gridx = 1; gbc.weightx = 0.6;
        criticalLowSpinner = createFormattedSpinner(currentConfig.getCriticalLow(), 0.0, 10000.0, 10.0);
        mainPanel.add(criticalLowSpinner, gbc);
        
        // Строка 4: Предупреждение (низ)
        gbc.gridx = 0; gbc.gridy = 4;
        mainPanel.add(new JLabel("Предупр. зона (низ):"), gbc);
        
        gbc.gridx = 1;
        warningLowSpinner = createFormattedSpinner(currentConfig.getWarningLow(), 0.0, 10000.0, 10.0);
        mainPanel.add(warningLowSpinner, gbc);
        
        // Строка 5: Предупреждение (верх)
        gbc.gridx = 0; gbc.gridy = 5;
        mainPanel.add(new JLabel("Предупр. зона (верх):"), gbc);
        
        gbc.gridx = 1;
        warningHighSpinner = createFormattedSpinner(currentConfig.getWarningHigh(), 0.0, 10000.0, 10.0);
        mainPanel.add(warningHighSpinner, gbc);
        
        // Строка 6: Критическая зона (верх)
        gbc.gridx = 0; gbc.gridy = 6;
        mainPanel.add(new JLabel("Критич. зона (верх):"), gbc);
        
        gbc.gridx = 1;
        criticalHighSpinner = createFormattedSpinner(currentConfig.getCriticalHigh(), 0.0, 50000.0, 10.0);
        mainPanel.add(criticalHighSpinner, gbc);
        
        // Разделитель
        gbc.gridx = 0; gbc.gridy = 7; gbc.gridwidth = 2;
        JSeparator separator2 = new JSeparator();
        separator2.setPreferredSize(new Dimension(300, 10));
        mainPanel.add(separator2, gbc);
        gbc.gridwidth = 1;
        
        // Подсказка
        gbc.gridx = 0; gbc.gridy = 8; gbc.gridwidth = 2;
        JLabel hintLabel = new JLabel("<html><i>Порядок: 0 < критич.низ < предупр.низ < предупр.верх < критич.верх < макс</i></html>");
        hintLabel.setFont(new Font("Arial", Font.PLAIN, 10));
        hintLabel.setForeground(Color.GRAY);
        mainPanel.add(hintLabel, gbc);
        
        add(mainPanel, BorderLayout.CENTER);
        
        // Панель кнопок
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 5));
        
        JButton okButton = new JButton("Применить");
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
     *  Создание JSpinner с правильным форматированием.
     * Разрешаем временный ввод невалидных значений (setAllowsInvalid(true)).
     */
    private JSpinner createFormattedSpinner(double value, double min, double max, double step) {
        SpinnerNumberModel model = new SpinnerNumberModel(value, min, max, step);
        JSpinner spinner = new JSpinner(model);
        
        // Создаем редактор с форматом без группировки разрядов
        JSpinner.NumberEditor editor = new JSpinner.NumberEditor(spinner, "0.0");
        spinner.setEditor(editor);
        
        // Получаем текстовое поле внутри спиннера
        JFormattedTextField textField = editor.getTextField();
        
        // Выравнивание текста вправо
        textField.setHorizontalAlignment(JTextField.RIGHT);
        
        // Форматтер без группировки разрядов
        DecimalFormat format = new DecimalFormat("0.0");
        format.setGroupingUsed(false);
        
        NumberFormatter formatter = new NumberFormatter(format);
        formatter.setValueClass(Double.class);
        formatter.setMinimum(min);
        formatter.setMaximum(max);
        
        //  Разрешаем временный ввод невалидных значений
        formatter.setAllowsInvalid(true);  // Было: false
        
        // Валидация происходит при потере фокуса
        formatter.setCommitsOnValidEdit(false);  // Было: true
        
        textField.setFormatterFactory(new DefaultFormatterFactory(formatter));
        
        // При получении фокуса - выделяем весь текст
        textField.addFocusListener(new java.awt.event.FocusAdapter() {
            @Override
            public void focusGained(java.awt.event.FocusEvent e) {
                SwingUtilities.invokeLater(() -> textField.selectAll());
            }
            
            //  При потере фокуса - принудительный коммит значения
            @Override
            public void focusLost(java.awt.event.FocusEvent e) {
                try {
                    textField.commitEdit();  // Фиксируем изменения
                } catch (java.text.ParseException ex) {
                    // Если значение невалидно - возвращаем предыдущее
                    textField.setValue(textField.getValue());
                }
            }
        });
        
        return spinner;
    }

    private void onOk() {
        //  Принудительный коммит всех полей перед валидацией
        try {
            commitAllSpinners();
        } catch (Exception e) {
            showError("Некорректное значение в одном из полей!");
            return;
        }
        
        try {
            double maxPower = (Double) maxPowerSpinner.getValue();
            double criticalLow = (Double) criticalLowSpinner.getValue();
            double warningLow = (Double) warningLowSpinner.getValue();
            double warningHigh = (Double) warningHighSpinner.getValue();
            double criticalHigh = (Double) criticalHighSpinner.getValue();
            
            // Валидация порядка значений
            if (criticalLow >= warningLow) {
                showError("Критическая зона (низ) должна быть меньше зоны предупреждения (низ)!");
                return;
            }
            
            if (warningLow >= warningHigh) {
                showError("Зона предупреждения (низ) должна быть меньше зоны предупреждения (верх)!");
                return;
            }
            
            if (warningHigh >= criticalHigh) {
                showError("Зона предупреждения (верх) должна быть меньше критической зоны (верх)!");
                return;
            }
            
            if (criticalHigh >= maxPower) {
                showError("Критическая зона (верх) должна быть меньше максимальной мощности!");
                return;
            }
            
            // Проверка минимальных диапазонов
            double minZoneWidth = maxPower * 0.01;
            
            if ((warningLow - criticalLow) < minZoneWidth) {
                showError("Ширина критической зоны (низ) слишком мала!");
                return;
            }
            
            if ((warningHigh - warningLow) < minZoneWidth) {
                showError("Ширина нормальной зоны слишком мала!");
                return;
            }
            
            if ((criticalHigh - warningHigh) < minZoneWidth) {
                showError("Ширина критической зоны (верх) слишком мала!");
                return;
            }
            
            confirmed = true;
            dispose();
            
        } catch (Exception ex) {
            showError("Ошибка валидации: " + ex.getMessage());
        }
    }

    /**
     *  Принудительный коммит всех спиннеров перед валидацией.
     */
    private void commitAllSpinners() throws java.text.ParseException {
        commitSpinner(maxPowerSpinner);
        commitSpinner(criticalLowSpinner);
        commitSpinner(warningLowSpinner);
        commitSpinner(warningHighSpinner);
        commitSpinner(criticalHighSpinner);
    }

    /**
     *  Коммит значения спиннера.
     */
    private void commitSpinner(JSpinner spinner) throws java.text.ParseException {
        JSpinner.NumberEditor editor = (JSpinner.NumberEditor) spinner.getEditor();
        JFormattedTextField textField = editor.getTextField();
        textField.commitEdit();
    }

    private void onCancel() {
        confirmed = false;
        dispose();
    }

    private void showError(String message) {
        JOptionPane.showMessageDialog(
            this,
            message,
            "Ошибка валидации",
            JOptionPane.ERROR_MESSAGE
        );
    }

    public boolean isConfirmed() {
        return confirmed;
    }

    public double getMaxPower() {
        return (Double) maxPowerSpinner.getValue();
    }

    public double getCriticalLow() {
        return (Double) criticalLowSpinner.getValue();
    }

    public double getCriticalHigh() {
        return (Double) criticalHighSpinner.getValue();
    }

    public double getWarningLow() {
        return (Double) warningLowSpinner.getValue();
    }

    public double getWarningHigh() {
        return (Double) warningHighSpinner.getValue();
    }
}