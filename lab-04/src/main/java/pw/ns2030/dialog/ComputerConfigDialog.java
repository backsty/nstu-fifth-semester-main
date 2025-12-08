package pw.ns2030.dialog;

import javax.swing.*;
import java.awt.*;

/**
 * Диалог настройки компьютера с ИБП.
 * Дефолтные значения: "Компьютер #N", 300 Вт.
 */
public class ComputerConfigDialog extends DeviceConfigDialog {

    public ComputerConfigDialog(Frame owner, String defaultName) {
        super(owner, "Добавить компьютер", defaultName, 300.0);
    }

    @Override
    protected void addCustomFields(JPanel panel, GridBagConstraints gbc) {
        // Информация о ИБП
        gbc.gridx = 0; gbc.gridy = 2; gbc.gridwidth = 2;
        JLabel infoLabel = new JLabel("<html><i>ИБП: автопереключение на батарею (2 мин автономии)</i></html>");
        infoLabel.setFont(new Font("Arial", Font.PLAIN, 11));
        infoLabel.setForeground(Color.GRAY);
        panel.add(infoLabel, gbc);
        gbc.gridwidth = 1;
    }
}