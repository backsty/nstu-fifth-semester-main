package pw.ns2030.dialog;

import javax.swing.*;
import java.awt.*;

/**
 * Диалог настройки лампы.
 * Дефолтные значения: "Лампа #N", 60 Вт.
 */
public class LampConfigDialog extends DeviceConfigDialog {

    public LampConfigDialog(Frame owner, String defaultName) {
        super(owner, "Добавить лампу", defaultName, 60.0);
    }

    @Override
    protected void addCustomFields(JPanel panel, GridBagConstraints gbc) {
        // Информация о типичной мощности
        gbc.gridx = 0; gbc.gridy = 2; gbc.gridwidth = 2;
        JLabel infoLabel = new JLabel("<html><i>Типовые значения: LED 10-20 Вт, накаливания 40-100 Вт</i></html>");
        infoLabel.setFont(new Font("Arial", Font.PLAIN, 11));
        infoLabel.setForeground(Color.GRAY);
        panel.add(infoLabel, gbc);
        gbc.gridwidth = 1;
    }
}