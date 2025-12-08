package pw.ns2030.window;

import pw.ns2030.controller.ApplianceController;
import pw.ns2030.controller.LampController;
import pw.ns2030.controller.PowerSystemController;
import pw.ns2030.model.Lamp;

import javax.swing.*;
import java.awt.*;

/**
 * Окно управления лампой с визуализацией света.
 * Показывает анимированное свечение при включенной лампе.
 */
public class LampWindow extends DeviceWindow {
    private Lamp lamp;
    private JLabel stateLabel;
    private JLabel powerLabel;
    private JButton toggleButton;
    private JPanel lightPanel;
    
    public LampWindow(ApplianceController controller, PowerSystemController powerSystem) {
        super(controller, powerSystem);
    }
    
    @Override
    protected void initComponents() {
        this.lamp = ((LampController) controller).getLamp();
        
        stateLabel = new JLabel("Состояние: " + lamp.getState().getDisplayName());
        stateLabel.setFont(new Font("Arial", Font.BOLD, 18));
        stateLabel.setHorizontalAlignment(SwingConstants.CENTER);
        
        powerLabel = new JLabel(String.format("Мощность: %.0f Вт", lamp.getCurrentPower()));
        powerLabel.setFont(new Font("Arial", Font.PLAIN, 14));
        powerLabel.setHorizontalAlignment(SwingConstants.CENTER);

        toggleButton = createStyledButton("Включить", new Color(76, 175, 80));
        toggleButton.setPreferredSize(new Dimension(150, 40));

        closeButton = createStyledButtonWithHover("Закрыть окно", new Color(158, 158, 158));
        closeButton.setPreferredSize(new Dimension(150, 40));
        closeButton.addActionListener(e -> dispose());
        
        // Кастомная панель с отрисовкой лампочки и свечения
        lightPanel = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2d = (Graphics2D) g;
                g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                
                int centerX = getWidth() / 2;
                int centerY = getHeight() / 2;
                int radius = Math.min(getWidth(), getHeight()) / 3;
                
                // Эффект свечения при включенной лампе
                if (lamp.isOn()) {
                    for (int i = 4; i > 0; i--) {
                        int alpha = 30 * i;
                        Color glowColor = new Color(255, 220, 0, alpha);
                        g2d.setColor(glowColor);
                        int glowRadius = radius + (30 * i);
                        g2d.fillOval(centerX - glowRadius, centerY - glowRadius, 
                                    glowRadius * 2, glowRadius * 2);
                    }
                    g2d.setColor(Color.YELLOW);
                } else {
                    g2d.setColor(Color.GRAY);
                }
                
                // Рисуем саму лампочку
                g2d.fillOval(centerX - radius, centerY - radius, radius * 2, radius * 2);
                g2d.setColor(Color.BLACK);
                g2d.setStroke(new BasicStroke(3));
                g2d.drawOval(centerX - radius, centerY - radius, radius * 2, radius * 2);
            }
        };
        lightPanel.setPreferredSize(new Dimension(250, 250));
    }
    
    @Override
    protected void setupLayout() {
        JPanel headerPanel = new JPanel(new BorderLayout());
        JLabel titleLabel = new JLabel(lamp.getName(), SwingConstants.CENTER);
        titleLabel.setFont(new Font("Arial", Font.BOLD, 20));
        headerPanel.add(titleLabel, BorderLayout.CENTER);
        
        JPanel centerPanel = new JPanel(new BorderLayout(10, 10));
        centerPanel.add(lightPanel, BorderLayout.CENTER);
        
        JPanel dataPanel = new JPanel(new GridLayout(2, 1, 5, 5));
        dataPanel.add(stateLabel);
        dataPanel.add(powerLabel);
        centerPanel.add(dataPanel, BorderLayout.SOUTH);
        
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
            if (lamp.isOn()) {
                lamp.turnOff();
            } else {
                if (!lamp.isPowerAvailable()) {
                    JOptionPane.showMessageDialog(
                        this,
                        "Невозможно включить лампу:\nПитание отключено!",
                        "Предупреждение",
                        JOptionPane.WARNING_MESSAGE
                    );
                    return;
                }
                lamp.turnOn();
            }
        });
    }
    
    /**
     * Обновление UI и перерисовка визуализации лампы.
     */
    @Override
    protected void updateData() {
        SwingUtilities.invokeLater(() -> {
            stateLabel.setText("Состояние: " + lamp.getState().getDisplayName());
            powerLabel.setText(String.format("Мощность: %.0f Вт", lamp.getCurrentPower()));
            
            toggleButton.setText(lamp.isOn() ? "Выключить" : "Включить");
            toggleButton.setBackground(lamp.isOn() ? 
                new Color(244, 67, 54) : new Color(76, 175, 80));
            toggleButton.setEnabled(lamp.isPowerAvailable() || lamp.isOn());
            
            lightPanel.repaint();
        });
    }
}