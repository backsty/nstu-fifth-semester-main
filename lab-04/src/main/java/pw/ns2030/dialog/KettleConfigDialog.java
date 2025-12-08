package pw.ns2030.dialog;

import javax.swing.*;
import java.awt.*;

/**
 * Диалог настройки чайника.
 * Дефолтные значения: "Чайник #N", 2000 Вт.
 */
public class KettleConfigDialog extends DeviceConfigDialog {

    public KettleConfigDialog(Frame owner, String defaultName) {
        super(owner, "Добавить чайник", defaultName, 2000.0);
    }

    @Override
    protected void addCustomFields(JPanel panel, GridBagConstraints gbc) {
        // Информационная строка о модели нагрева
        gbc.gridx = 0; gbc.gridy = 2; gbc.gridwidth = 2;
        JLabel infoLabel = new JLabel("<html><i>Модель нагрева: 20°C → 100°C за ~40 сек</i></html>");
        infoLabel.setFont(new Font("Arial", Font.PLAIN, 11));
        infoLabel.setForeground(Color.GRAY);
        panel.add(infoLabel, gbc);
        gbc.gridwidth = 1;  // Возвращаем обратно
    }
}