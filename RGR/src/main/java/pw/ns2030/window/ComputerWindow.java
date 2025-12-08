package pw.ns2030.window;

import pw.ns2030.component.LevelIndicator;
import pw.ns2030.component.LevelIndicatorConfig;
import pw.ns2030.controller.ApplianceController;
import pw.ns2030.controller.ComputerController;
import pw.ns2030.controller.PowerSystemController;
import pw.ns2030.model.Computer;

import javax.swing.*;
import java.awt.*;

/**
 * Окно управления компьютером с индикатором батареи.
 * Показывает уровень заряда, режим питания и оставшееся время работы от батареи.
 */
public class ComputerWindow extends DeviceWindow {
    private Computer computer;
    private LevelIndicator batteryIndicator;
    private JLabel batteryLabel;
    private JLabel stateLabel;
    private JLabel powerLabel;
    private JLabel remainingTimeLabel;
    private JButton toggleButton;
    
    public ComputerWindow(ApplianceController controller, PowerSystemController powerSystem) {
        super(controller, powerSystem);
    }
    
    @Override
    protected void initComponents() {
        this.computer = ((ComputerController) controller).getComputer();
        
        // Индикатор батареи от 0 до 100 процентов
        LevelIndicatorConfig config = new LevelIndicatorConfig.Builder()
            .setMinValue(0.0)
            .setMaxValue(100.0)
            .setCriticalRange(5.0, 95.0)
            .setWarningRange(20.0, 80.0)
            .build();
        
        batteryIndicator = new LevelIndicator(config);
        batteryIndicator.setValue(computer.getBatteryLevel());
        
        String chargingIcon = computer.isCharging() ? " [+]" : 
                             (computer.isOnBattery() ? " [BAT]" : "");
        batteryLabel = new JLabel(String.format("%.0f%%%s", computer.getBatteryLevel(), chargingIcon));
        batteryLabel.setFont(new Font("Arial", Font.BOLD, 24));
        batteryLabel.setHorizontalAlignment(SwingConstants.CENTER);
        batteryLabel.setForeground(new Color(156, 39, 176));
        
        stateLabel = new JLabel("Состояние: " + computer.getState().getDisplayName());
        stateLabel.setFont(new Font("Arial", Font.PLAIN, 14));
        stateLabel.setHorizontalAlignment(SwingConstants.CENTER);
        
        powerLabel = new JLabel(String.format("Мощность: %.0f Вт", computer.getCurrentPower()));
        powerLabel.setFont(new Font("Arial", Font.PLAIN, 14));
        powerLabel.setHorizontalAlignment(SwingConstants.CENTER);
        
        remainingTimeLabel = new JLabel("Оставшееся время: N/A");
        remainingTimeLabel.setFont(new Font("Arial", Font.PLAIN, 12));
        remainingTimeLabel.setHorizontalAlignment(SwingConstants.CENTER);

        toggleButton = createStyledButton("Включить", new Color(76, 175, 80));
        toggleButton.setPreferredSize(new Dimension(150, 40));

        closeButton = createStyledButtonWithHover("Закрыть окно", new Color(158, 158, 158));
        closeButton.setPreferredSize(new Dimension(150, 40));
        closeButton.addActionListener(e -> dispose());
    }
    
    @Override
    protected void setupLayout() {
        JPanel headerPanel = new JPanel(new BorderLayout());
        JLabel titleLabel = new JLabel(computer.getName(), SwingConstants.CENTER);
        titleLabel.setFont(new Font("Arial", Font.BOLD, 20));
        headerPanel.add(titleLabel, BorderLayout.CENTER);
        
        JPanel centerPanel = new JPanel(new BorderLayout(10, 10));
        
        JPanel indicatorPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        indicatorPanel.add(batteryIndicator);
        
        JPanel displayPanel = new JPanel(new BorderLayout(5, 5));
        displayPanel.add(batteryLabel, BorderLayout.CENTER);
        
        JPanel infoPanel = new JPanel(new GridLayout(3, 1, 5, 5));
        infoPanel.add(stateLabel);
        infoPanel.add(powerLabel);
        infoPanel.add(remainingTimeLabel);
        displayPanel.add(infoPanel, BorderLayout.SOUTH);
        
        centerPanel.add(indicatorPanel, BorderLayout.WEST);
        centerPanel.add(displayPanel, BorderLayout.CENTER);
        
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 10));
        buttonPanel.add(toggleButton);
        buttonPanel.add(closeButton);
        
        add(headerPanel, BorderLayout.NORTH);
        add(centerPanel, BorderLayout.CENTER);
        add(buttonPanel, BorderLayout.SOUTH);
        
        pack();
        setLocationRelativeTo(null);
        setResizable(false);
    }
    
    @Override
    protected void setupListeners() {
        super.setupListeners();
        
        toggleButton.addActionListener(e -> {
            if (computer.isOn()) {
                computer.turnOff();
            } else {
                if (!computer.isPowerAvailable() && computer.getBatteryLevel() <= 0) {
                    JOptionPane.showMessageDialog(
                        this,
                        "Невозможно включить компьютер:\nНет питания и батарея разряжена!",
                        "Предупреждение",
                        JOptionPane.WARNING_MESSAGE
                    );
                    return;
                }
                computer.turnOn();
            }
        });
    }
    
    /**
     * Обновление UI с изменением цвета батареи в зависимости от уровня заряда.
     * Показывает оставшееся время при работе от батареи.
     */
    @Override
    protected void updateData() {
        SwingUtilities.invokeLater(() -> {
            double battery = computer.getBatteryLevel();
            
            batteryIndicator.setValue(battery);
            
            String chargingIcon = computer.isCharging() ? " [+]" : 
                                 (computer.isOnBattery() ? " [BAT]" : "");
            batteryLabel.setText(String.format("%.0f%%%s", battery, chargingIcon));
            
            // Цвет батареи зависит от уровня заряда
            if (battery <= 10) {
                batteryLabel.setForeground(new Color(244, 67, 54));  // Красный - критично
            } else if (battery <= 20) {
                batteryLabel.setForeground(new Color(255, 193, 7));  // Желтый - предупреждение
            } else if (computer.isCharging()) {
                batteryLabel.setForeground(new Color(76, 175, 80));  // Зеленый - заряжается
            } else {
                batteryLabel.setForeground(new Color(156, 39, 176)); // Фиолетовый - норма
            }
            
            stateLabel.setText("Состояние: " + computer.getState().getDisplayName());
            powerLabel.setText(String.format("Мощность: %.0f Вт", computer.getCurrentPower()));
            
            // Показываем оставшееся время при работе от батареи
            if (computer.isOnBattery()) {
                double remainingMin = computer.getRemainingBatteryMinutes();
                remainingTimeLabel.setText(String.format("Осталось: %.1f мин", remainingMin));
                
                if (battery <= 10) {
                    remainingTimeLabel.setForeground(Color.RED);
                } else {
                    remainingTimeLabel.setForeground(Color.BLACK);
                }
            } else {
                remainingTimeLabel.setText("Оставшееся время: N/A");
                remainingTimeLabel.setForeground(Color.BLACK);
            }

            toggleButton.setText(computer.isOn() ? "Выключить" : "Включить");
            toggleButton.setBackground(computer.isOn() ? 
                new Color(244, 67, 54) : new Color(76, 175, 80));
        });
    }
}