package pw.ns2030.component;

import pw.ns2030.controller.*;
import pw.ns2030.model.*;
import pw.ns2030.window.*;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

/**
 * Панель управления одним устройством в главном окне.
 * Показывает состояние, мощность, кнопки управления и переключатель.
 * Двойной клик открывает отдельное окно устройства.
 */
public class ApplianceControlPanel extends JPanel {
    private final ApplianceController controller;
    private final Appliance appliance;
    private PowerSystemController powerSystem;
    
    private JLabel stateLabel;
    private JLabel powerLabel;
    private JLabel extraInfoLabel;
    private JButton toggleButton;
    private JButton removeButton;
    private ToggleSwitch toggleSwitch;
    
    private Timer updateTimer;

    public ApplianceControlPanel(ApplianceController controller) {
        this.controller = controller;
        this.appliance = controller.getAppliance();
        
        initComponents();
        setupLayout();
        setupListeners();
        updateDeviceUI();
        
        updateTimer = new Timer(500, e -> updateDeviceUI());
        updateTimer.start();
    }

    public void setPowerSystemController(PowerSystemController powerSystem) {
        this.powerSystem = powerSystem;
    }

    private void initComponents() {
        setLayout(new BorderLayout(8, 8));
        
        String title = appliance.getName();
        
        TitledBorder border = BorderFactory.createTitledBorder(title);
        border.setTitleFont(new Font("Arial", Font.BOLD, 13));
        border.setTitleColor(getDeviceColor());
        
        setBorder(BorderFactory.createCompoundBorder(
            border,
            BorderFactory.createEmptyBorder(5, 10, 5, 10)
        ));

        setPreferredSize(new Dimension(450, 130));

        setCursor(new Cursor(Cursor.HAND_CURSOR));
        
        stateLabel = new JLabel("Состояние: " + appliance.getState().getDisplayName());
        stateLabel.setFont(new Font("Arial", Font.PLAIN, 12));
        
        powerLabel = new JLabel(String.format("Мощность: %.0f Вт", appliance.getCurrentPower()));
        powerLabel.setFont(new Font("Arial", Font.PLAIN, 12));
        
        extraInfoLabel = new JLabel("");
        extraInfoLabel.setFont(new Font("Arial", Font.PLAIN, 11));
        
        toggleButton = createStyledButton("Включить", new Color(76, 175, 80), new Color(56, 142, 60));
        toggleButton.setPreferredSize(new Dimension(120, 32));
        
        removeButton = createStyledButton("Удалить", new Color(244, 67, 54), new Color(211, 47, 47));
        removeButton.setPreferredSize(new Dimension(120, 32));
        
        toggleSwitch = new ToggleSwitch(appliance.isOn());
    }

    private JButton createStyledButton(String text, Color bgColor, Color hoverColor) {
        JButton button = new JButton(text);
        button.setFont(new Font("Arial", Font.BOLD, 12));
        button.setBackground(bgColor);
        button.setForeground(Color.WHITE);
        button.setFocusPainted(false);
        button.setBorderPainted(false);
        button.setOpaque(true);
        button.setCursor(new Cursor(Cursor.HAND_CURSOR));
        
        button.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                if (button.isEnabled()) {
                    button.setBackground(hoverColor);
                }
            }
            
            @Override
            public void mouseExited(MouseEvent e) {
                button.setBackground(bgColor);
            }
        });
        
        return button;
    }

    private void setupLayout() {
        JPanel infoPanel = new JPanel();
        infoPanel.setLayout(new BoxLayout(infoPanel, BoxLayout.Y_AXIS));
        infoPanel.add(stateLabel);
        infoPanel.add(Box.createVerticalStrut(5));
        infoPanel.add(powerLabel);
        infoPanel.add(Box.createVerticalStrut(5));
        infoPanel.add(extraInfoLabel);
        
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 5));
        buttonPanel.add(toggleButton);
        buttonPanel.add(removeButton);
        
        JPanel switchPanel = new JPanel(new BorderLayout());
        switchPanel.add(toggleSwitch, BorderLayout.CENTER);
        switchPanel.setPreferredSize(new Dimension(110, 100));
        
        JPanel leftPanel = new JPanel(new BorderLayout(5, 5));
        leftPanel.add(infoPanel, BorderLayout.NORTH);
        leftPanel.add(buttonPanel, BorderLayout.SOUTH);
        
        add(leftPanel, BorderLayout.CENTER);
        add(switchPanel, BorderLayout.EAST);
    }

    private void setupListeners() {
        addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {  // Двойной клик
                    openDeviceWindow();
                }
            }
        });
        
        toggleButton.addActionListener(e -> {
            SwingUtilities.invokeLater(() -> {
                try {
                    if (appliance instanceof Kettle) {
                        Kettle kettle = (Kettle) appliance;
                        PowerState state = kettle.getState();
                        
                        if (state == PowerState.HEATING || state == PowerState.COOLING) {
                            kettle.turnOff();
                        } else {
                            kettle.turnOn();
                        }
                    } else {
                        if (appliance.isOn()) {
                            appliance.turnOff();
                        } else {
                            appliance.turnOn();
                        }
                    }
                } catch (Exception ex) {
                    System.err.println("[ApplianceControlPanel] Ошибка переключения: " + ex.getMessage());
                    ex.printStackTrace();
                }
            });
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
        
        controller.addListener(event -> {
            SwingUtilities.invokeLater(this::updateDeviceUI);
        });
    }

    /**
     * Открытие отдельного окна устройства с передачей ссылки на систему.
     */
    private void openDeviceWindow() {
        if (powerSystem == null) {
            System.err.println("[ApplianceControlPanel] PowerSystemController не установлен!");
            JOptionPane.showMessageDialog(
                this,
                "Невозможно открыть окно:\nОтсутствует ссылка на контроллер системы",
                "Ошибка",
                JOptionPane.ERROR_MESSAGE
            );
            return;
        }
        
        try {
            DeviceWindow window = null;
            
            if (controller instanceof KettleController) {
                window = new KettleWindow(controller, powerSystem);
            } else if (controller instanceof LampController) {
                window = new LampWindow(controller, powerSystem);
            } else if (controller instanceof ComputerController) {
                window = new ComputerWindow(controller, powerSystem);
            }
            
            if (window != null) {
                window.setVisible(true);
                System.out.println("[ApplianceControlPanel] Открыто окно: " + window.getTitle());
            }
            
        } catch (Exception ex) {
            System.err.println("[ApplianceControlPanel] Ошибка открытия окна: " + ex.getMessage());
            ex.printStackTrace();
            
            JOptionPane.showMessageDialog(
                this,
                "Ошибка открытия окна:\n" + ex.getMessage(),
                "Ошибка",
                JOptionPane.ERROR_MESSAGE
            );
        }
    }

    /**
     * Обновление UI панели на основе текущего состояния устройства.
     */
    private void updateDeviceUI() {
        SwingUtilities.invokeLater(() -> {
            boolean powerAvailable = appliance.isPowerAvailable();
            PowerState currentState = appliance.getState();
            
            stateLabel.setText("Состояние: " + currentState.getDisplayName());
            powerLabel.setText(String.format("Мощность: %.0f Вт", appliance.getCurrentPower()));
            
            updateToggleSwitch();
            
            if (!powerAvailable) {
                updateButtonForNoPower();
            } else {
                updateButtonForNormalPower();
            }
            
            updateExtraInfo();
            updateTitleColor(powerAvailable);
            
            repaint();
        });
    }

    private void updateToggleSwitch() {
        if (toggleSwitch == null) return;
        
        boolean shouldBeOn = false;
        
        if (appliance instanceof Kettle) {
            Kettle kettle = (Kettle) appliance;
            PowerState state = kettle.getState();
            shouldBeOn = (state == PowerState.HEATING || state == PowerState.COOLING);
        } else if (appliance instanceof Computer) {
            shouldBeOn = ((Computer) appliance).isOn();
        } else if (appliance instanceof Lamp) {
            shouldBeOn = ((Lamp) appliance).isOn();
        } else {
            shouldBeOn = appliance.isOn();
        }
        
        toggleSwitch.setState(shouldBeOn);
    }

    private void updateButtonForNoPower() {
        if (appliance instanceof Kettle) {
            Kettle kettle = (Kettle) appliance;
            PowerState state = kettle.getState();
            
            if (state == PowerState.HEATING || state == PowerState.COOLING) {
                toggleButton.setText("Выключить");
                toggleButton.setEnabled(true);
                updateButtonStyle(toggleButton, new Color(244, 67, 54), new Color(211, 47, 47));
            } else {
                toggleButton.setText("Включить");
                toggleButton.setEnabled(false);
                updateButtonStyle(toggleButton, Color.GRAY, Color.GRAY);
            }
        } else if (appliance instanceof Computer) {
            Computer computer = (Computer) appliance;
            
            if (computer.isOn()) {
                toggleButton.setText("Выключить");
                toggleButton.setEnabled(true);
                updateButtonStyle(toggleButton, new Color(244, 67, 54), new Color(211, 47, 47));
            } else {
                toggleButton.setText("Включить");
                toggleButton.setEnabled(computer.getBatteryLevel() > 0);
                updateButtonStyle(toggleButton, Color.GRAY, Color.GRAY);
            }
        } else {
            if (appliance.isOn()) {
                toggleButton.setText("Выключить");
                toggleButton.setEnabled(true);
                updateButtonStyle(toggleButton, new Color(244, 67, 54), new Color(211, 47, 47));
            } else {
                toggleButton.setText("Включить");
                toggleButton.setEnabled(false);
                updateButtonStyle(toggleButton, Color.GRAY, Color.GRAY);
            }
        }
    }

    private void updateButtonForNormalPower() {
        toggleButton.setEnabled(true);
        
        if (appliance instanceof Kettle) {
            Kettle kettle = (Kettle) appliance;
            PowerState state = kettle.getState();
            
            if (state == PowerState.HEATING || state == PowerState.COOLING) {
                toggleButton.setText("Выключить");
                updateButtonStyle(toggleButton, new Color(244, 67, 54), new Color(211, 47, 47));
            } else {
                toggleButton.setText("Включить");
                updateButtonStyle(toggleButton, new Color(76, 175, 80), new Color(56, 142, 60));
            }
        } else {
            if (appliance.isOn()) {
                toggleButton.setText("Выключить");
                updateButtonStyle(toggleButton, new Color(244, 67, 54), new Color(211, 47, 47));
            } else {
                toggleButton.setText("Включить");
                updateButtonStyle(toggleButton, new Color(76, 175, 80), new Color(56, 142, 60));
            }
        }
    }

    private void updateExtraInfo() {
        if (appliance instanceof Kettle) {
            Kettle kettle = (Kettle) appliance;
            extraInfoLabel.setText(String.format("Температура: %.1f°C (дважды кликните для окна)", kettle.getTemperature()));
        } else if (appliance instanceof Computer) {
            Computer computer = (Computer) appliance;
            String chargingStatus = computer.isCharging() ? " [+]" : (computer.isOnBattery() ? " [BAT]" : "");
            extraInfoLabel.setText(String.format("Батарея: %.0f%%%s (дважды кликните для окна)", computer.getBatteryLevel(), chargingStatus));
        } else {
            extraInfoLabel.setText("Дважды кликните для открытия окна устройства");
        }
    }

    private void updateTitleColor(boolean powerAvailable) {
        TitledBorder border = (TitledBorder) ((javax.swing.border.CompoundBorder) getBorder()).getOutsideBorder();
        
        if (!powerAvailable) {
            border.setTitleColor(Color.RED);
        } else if (appliance.isOn()) {
            border.setTitleColor(getDeviceColor());
        } else {
            border.setTitleColor(Color.GRAY);
        }
    }

    private Color getDeviceColor() {
        if (appliance instanceof Kettle) return new Color(33, 150, 243);
        if (appliance instanceof Lamp) return new Color(255, 193, 7);
        if (appliance instanceof Computer) return new Color(156, 39, 176);
        return new Color(76, 175, 80);
    }

    private void updateButtonStyle(JButton button, Color bgColor, Color hoverColor) {
        button.setBackground(bgColor);
        
        for (java.awt.event.MouseListener ml : button.getMouseListeners()) {
            if (ml instanceof MouseAdapter) {
                button.removeMouseListener(ml);
            }
        }
        
        button.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                if (button.isEnabled()) {
                    button.setBackground(hoverColor);
                }
            }
            
            @Override
            public void mouseExited(MouseEvent e) {
                button.setBackground(bgColor);
            }
        });
    }

    public void cleanup() {
        if (updateTimer != null) {
            updateTimer.stop();
            updateTimer = null;
        }
    }

    public ApplianceController getController() {
        return controller;
    }
}