package pw.ns2030.component;

import pw.ns2030.controller.ApplianceController;
import pw.ns2030.controller.ComputerController;
import pw.ns2030.controller.KettleController;
import pw.ns2030.model.*;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import java.awt.*;

/**
 * Панель управления одним устройством - отображает состояние и команды.
 * Адаптируется под тип устройства (чайник/лампа/компьютер).
 */
public class ApplianceControlPanel extends JPanel {
    private final ApplianceController controller;
    private final Appliance appliance;
    
    private JLabel nameLabel;
    private JLabel stateLabel;
    private JLabel powerLabel;
    private JLabel extraInfoLabel;
    private JButton toggleButton;
    private JButton removeButton;
    private LevelIndicator extraIndicator;

    public ApplianceControlPanel(ApplianceController controller) {
        this.controller = controller;
        this.appliance = controller.getAppliance();
        
        initComponents();
        setupLayout();
        setupListeners();
        updateDeviceUI();
        
        // Таймер для обновления UI (каждую секунду)
        Timer updateTimer = new Timer(1000, e -> updateDeviceUI());
        updateTimer.start();
    }

    private void initComponents() {
        setLayout(new BorderLayout(5, 5));
        TitledBorder border = new TitledBorder(getDeviceIcon() + " " + appliance.getName());
        border.setTitleFont(new Font("Segoe UI Emoji", Font.BOLD, 12));
        setBorder(border);
        setPreferredSize(new Dimension(280, 200));
        
        nameLabel = new JLabel(appliance.getName(), SwingConstants.CENTER);
        nameLabel.setFont(new Font("Arial", Font.BOLD, 14));
        
        stateLabel = new JLabel("Состояние: " + appliance.getState().getDisplayName(), SwingConstants.CENTER);
        stateLabel.setFont(new Font("Arial", Font.PLAIN, 12));
        
        powerLabel = new JLabel(String.format("Мощность: %.0f Вт", appliance.getCurrentPower()), SwingConstants.CENTER);
        powerLabel.setFont(new Font("Arial", Font.PLAIN, 12));
        
        extraInfoLabel = new JLabel("", SwingConstants.CENTER);
        extraInfoLabel.setFont(new Font("Arial", Font.PLAIN, 11));
        
        toggleButton = new JButton("Включить");
        toggleButton.setPreferredSize(new Dimension(120, 30));
        
        removeButton = new JButton("Удалить");
        removeButton.setPreferredSize(new Dimension(120, 30));
        removeButton.setBackground(new Color(244, 67, 54));
        removeButton.setForeground(Color.WHITE);
        
        if (appliance instanceof Kettle) {
            // Температура чайника: 20-100°C
            // Структура зон (снизу вверх):
            // 20-30°C (критическая холодная)
            // 30-40°C (предупреждение холодная)
            // 40-85°C (норма)
            // 85-95°C (предупреждение горячая)
            // 95-100°C (критическая кипение)
            
            LevelIndicatorConfig tempConfig = new LevelIndicatorConfig.Builder()
                .setMinValue(20.0)
                .setMaxValue(100.0)
                .setCriticalRange(30.0, 95.0)   // ✅ БЛИЖЕ к краям [20, 100]
                .setWarningRange(40.0, 85.0)    // ✅ ВНУТРИ критической
                .build();
            extraIndicator = new LevelIndicator(tempConfig);
            extraIndicator.setPreferredSize(new Dimension(40, 120));
            
        } else if (appliance instanceof Computer) {
            // Батарея компьютера: 0-100%
            // Структура зон (снизу вверх):
            // 0-10% (критическая разряд)
            // 10-20% (предупреждение низкая)
            // 20-80% (норма)
            // 80-90% (предупреждение высокая)
            // 90-100% (критическая полная)
            
            LevelIndicatorConfig batteryConfig = new LevelIndicatorConfig.Builder()
                .setMinValue(0.0)
                .setMaxValue(100.0)
                .setCriticalRange(10.0, 90.0)   // ✅ БЛИЖЕ к краям [0, 100]
                .setWarningRange(20.0, 80.0)    // ✅ ВНУТРИ критической
                .build();
            extraIndicator = new LevelIndicator(batteryConfig);
            extraIndicator.setPreferredSize(new Dimension(40, 120));
        }
    }

    private void setupLayout() {
        JPanel infoPanel = new JPanel(new GridLayout(4, 1, 3, 3));
        infoPanel.add(stateLabel);
        infoPanel.add(powerLabel);
        infoPanel.add(extraInfoLabel);
        
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 5, 5));
        buttonPanel.add(toggleButton);
        buttonPanel.add(removeButton);
        
        add(infoPanel, BorderLayout.CENTER);
        add(buttonPanel, BorderLayout.SOUTH);
        
        if (extraIndicator != null) {
            JPanel indicatorPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
            indicatorPanel.add(extraIndicator);
            add(indicatorPanel, BorderLayout.EAST);
        }
    }

    private void setupListeners() {
        toggleButton.addActionListener(e -> {
            if (appliance.isOn()) {
                appliance.turnOff();
            } else {
                appliance.turnOn();
            }
            updateDeviceUI();
        });
        
        removeButton.addActionListener(e -> {
            int result = JOptionPane.showConfirmDialog(
                this,
                "Удалить устройство \"" + appliance.getName() + "\"?",
                "Подтверждение",
                JOptionPane.YES_NO_OPTION
            );
            
            if (result == JOptionPane.YES_OPTION) {
                firePropertyChange("removeDevice", null, controller);
            }
        });
    }

    private void updateDeviceUI() {
        SwingUtilities.invokeLater(() -> {
            // Обновление состояния
            stateLabel.setText("Состояние: " + appliance.getState().getDisplayName());
            powerLabel.setText(String.format("Мощность: %.0f Вт", appliance.getCurrentPower()));
            
            // Обновление кнопки
            if (appliance.isOn()) {
                toggleButton.setText("■ Выключить");
                toggleButton.setBackground(new Color(244, 67, 54));
                toggleButton.setForeground(Color.WHITE);
            } else {
                toggleButton.setText("► Включить");
                toggleButton.setBackground(new Color(76, 175, 80));
                toggleButton.setForeground(Color.WHITE);
            }
            
            // Специфичная информация для типов устройств
            if (appliance instanceof Kettle) {
                Kettle kettle = (Kettle) appliance;
                extraInfoLabel.setText(String.format("Температура: %.1f°C", kettle.getTemperature()));
                if (extraIndicator != null) {
                    extraIndicator.setValue(kettle.getTemperature());
                }
            } else if (appliance instanceof Computer) {
                Computer computer = (Computer) appliance;
                String batteryInfo = String.format("Батарея: %.0f%% %s", 
                    computer.getBatteryLevel(),
                    computer.isCharging() ? "⚡" : (computer.isOnBattery() ? "🔋" : ""));
                extraInfoLabel.setText(batteryInfo);
                if (extraIndicator != null) {
                    extraIndicator.setValue(computer.getBatteryLevel());
                }
            } else {
                extraInfoLabel.setText("");
            }
            
            // Цвет рамки в зависимости от состояния
            TitledBorder border = (TitledBorder) getBorder();
            if (!appliance.isPowerAvailable()) {
                border.setTitleColor(Color.RED);
            } else if (appliance.isOn()) {
                border.setTitleColor(new Color(76, 175, 80));
            } else {
                border.setTitleColor(Color.GRAY);
            }
            repaint();
        });
    }

    private String getDeviceIcon() {
        if (appliance instanceof Kettle) return "☕";
        if (appliance instanceof Lamp) return "💡";
        if (appliance instanceof Computer) return "💻";
        return "🔌";
    }

    public ApplianceController getController() {
        return controller;
    }
}