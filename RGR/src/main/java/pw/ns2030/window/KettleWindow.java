package pw.ns2030.window;

import pw.ns2030.component.LevelIndicator;
import pw.ns2030.component.LevelIndicatorConfig;
import pw.ns2030.controller.ApplianceController;
import pw.ns2030.controller.KettleController;
import pw.ns2030.controller.PowerSystemController;
import pw.ns2030.model.Kettle;
import pw.ns2030.model.PowerState;

import javax.swing.*;
import java.awt.*;

/**
 * Окно управления чайником с индикатором температуры.
 * Показывает текущую температуру, состояние нагрева и мощность.
 */
public class KettleWindow extends DeviceWindow {
    private Kettle kettle;
    private LevelIndicator temperatureIndicator;
    private JLabel temperatureLabel;
    private JLabel stateLabel;
    private JLabel powerLabel;
    private JButton toggleButton;
    
    public KettleWindow(ApplianceController controller, PowerSystemController powerSystem) {
        super(controller, powerSystem);
    }
    
    @Override
    protected void initComponents() {
        this.kettle = ((KettleController) controller).getKettle();
        
        // Индикатор температуры от 20 до 100 градусов
        LevelIndicatorConfig config = new LevelIndicatorConfig.Builder()
            .setMinValue(20.0)
            .setMaxValue(100.0)
            .setCriticalRange(25.0, 95.0)
            .setWarningRange(30.0, 90.0)
            .build();
        
        temperatureIndicator = new LevelIndicator(config);
        temperatureIndicator.setValue(kettle.getTemperature());
        
        temperatureLabel = new JLabel(String.format("%.1f°C", kettle.getTemperature()));
        temperatureLabel.setFont(new Font("Arial", Font.BOLD, 24));
        temperatureLabel.setHorizontalAlignment(SwingConstants.CENTER);
        temperatureLabel.setForeground(new Color(33, 150, 243));
        
        stateLabel = new JLabel("Состояние: " + kettle.getState().getDisplayName());
        stateLabel.setFont(new Font("Arial", Font.PLAIN, 14));
        stateLabel.setHorizontalAlignment(SwingConstants.CENTER);
        
        powerLabel = new JLabel(String.format("Мощность: %.0f Вт", kettle.getCurrentPower()));
        powerLabel.setFont(new Font("Arial", Font.PLAIN, 14));
        powerLabel.setHorizontalAlignment(SwingConstants.CENTER);
        
        toggleButton = createStyledButton("Включить", new Color(76, 175, 80));
        toggleButton.setPreferredSize(new Dimension(150, 40));
        
        closeButton = createStyledButtonWithHover("Закрыть окно", new Color(158, 158, 158));
        closeButton.setPreferredSize(new Dimension(150, 40));
        closeButton.addActionListener(e -> dispose());
    }
    
    @Override
    protected void setupLayout() {
        JPanel headerPanel = new JPanel(new BorderLayout());
        JLabel titleLabel = new JLabel(kettle.getName(), SwingConstants.CENTER);
        titleLabel.setFont(new Font("Arial", Font.BOLD, 20));
        headerPanel.add(titleLabel, BorderLayout.CENTER);
        
        JPanel centerPanel = new JPanel(new BorderLayout(10, 10));
        
        JPanel indicatorPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        indicatorPanel.add(temperatureIndicator);
        
        JPanel displayPanel = new JPanel(new BorderLayout(5, 5));
        displayPanel.add(temperatureLabel, BorderLayout.CENTER);
        
        JPanel infoPanel = new JPanel(new GridLayout(2, 1, 5, 5));
        infoPanel.add(stateLabel);
        infoPanel.add(powerLabel);
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
            PowerState state = kettle.getState();
            
            if (state == PowerState.HEATING || state == PowerState.COOLING) {
                kettle.turnOff();
            } else {
                if (!kettle.isPowerAvailable()) {
                    JOptionPane.showMessageDialog(
                        this,
                        "Невозможно включить чайник:\nПитание отключено!",
                        "Предупреждение",
                        JOptionPane.WARNING_MESSAGE
                    );
                    return;
                }
                kettle.turnOn();
            }
        });
    }
    
    /**
     * Обновление UI на основе текущего состояния чайника.
     * Изменяет цвет температуры в зависимости от значения.
     */
    @Override
    protected void updateData() {
        SwingUtilities.invokeLater(() -> {
            double temp = kettle.getTemperature();
            PowerState state = kettle.getState();
            
            temperatureIndicator.setValue(temp);
            temperatureLabel.setText(String.format("%.1f°C", temp));
            
            // Цвет температуры зависит от значения
            if (temp >= 95) {
                temperatureLabel.setForeground(new Color(244, 67, 54));  // Красный
            } else if (temp >= 70) {
                temperatureLabel.setForeground(new Color(255, 193, 7));  // Желтый
            } else {
                temperatureLabel.setForeground(new Color(33, 150, 243)); // Синий
            }
            
            stateLabel.setText("Состояние: " + state.getDisplayName());
            powerLabel.setText(String.format("Мощность: %.0f Вт", kettle.getCurrentPower()));
            
            boolean isActive = (state == PowerState.HEATING || state == PowerState.COOLING);
            
            toggleButton.setText(isActive ? "Выключить" : "Включить");
            toggleButton.setBackground(isActive ? 
                new Color(244, 67, 54) : new Color(76, 175, 80));
            toggleButton.setEnabled(kettle.isPowerAvailable() || isActive);
        });
    }
}