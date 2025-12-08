package pw.ns2030.component;

import pw.ns2030.controller.PowerSystemController;
import pw.ns2030.dialog.PowerIndicatorConfigDialog;
import pw.ns2030.event.ConsumptionEvent;
import pw.ns2030.event.OverloadEvent;
import pw.ns2030.event.PowerEvent;
import pw.ns2030.listener.PowerSystemListener;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import java.awt.*;

/**
 * Панель мониторинга общего потребления энергии.
 * Отображает индикатор уровня, статистику и кнопку конфигурации.
 */
public class PowerMeterPanel extends JPanel implements PowerSystemListener {
    private final PowerSystemController powerSystem;
    private final LevelIndicator powerIndicator;
    
    private JLabel statusLabel;
    private JLabel consumptionLabel;
    private JLabel devicesLabel;
    private JLabel powerStateLabel;
    private JButton configButton;

    public PowerMeterPanel(PowerSystemController powerSystem) {
        this.powerSystem = powerSystem;
        
        LevelIndicatorConfig config = new LevelIndicatorConfig.Builder()
            .setMinValue(0.0)
            .setMaxValue(5000.0)
            .setCriticalRange(100.0, 4800.0)
            .setWarningRange(500.0, 4000.0)
            .build();
        
        this.powerIndicator = new LevelIndicator(config);
        this.powerIndicator.setValue(0.0);
        
        initComponents();
        setupLayout();
        
        powerSystem.addListener(this);
    }

    private void initComponents() {
        setLayout(new BorderLayout(10, 10));
        
        TitledBorder border = BorderFactory.createTitledBorder("Общее потребление энергии");
        border.setTitleFont(new Font("Arial", Font.BOLD, 12));
        setBorder(border);
        
        setPreferredSize(new Dimension(300, 700));
        
        Font labelFont = new Font("Arial", Font.PLAIN, 12);
        Font boldLabelFont = new Font("Arial", Font.BOLD, 14);
        
        statusLabel = new JLabel("Система готова", SwingConstants.CENTER);
        statusLabel.setFont(boldLabelFont);
        statusLabel.setForeground(new Color(76, 175, 80));
        
        consumptionLabel = new JLabel("0 Вт / 5000 Вт", SwingConstants.CENTER);
        consumptionLabel.setFont(new Font("Arial", Font.PLAIN, 16));
        
        devicesLabel = new JLabel("Устройств: 0 (активных: 0)", SwingConstants.CENTER);
        devicesLabel.setFont(labelFont);
        
        powerStateLabel = new JLabel("[OK] Питание в норме", SwingConstants.CENTER);
        powerStateLabel.setFont(new Font("Arial", Font.BOLD, 12));
        powerStateLabel.setOpaque(true);
        powerStateLabel.setBackground(new Color(76, 175, 80));
        powerStateLabel.setForeground(Color.WHITE);
        
        // кнопка конфигурации
        configButton = new JButton("Настроить индикатор");
        configButton.setFont(new Font("Arial", Font.BOLD, 11));
        configButton.setBackground(new Color(33, 150, 243));
        configButton.setForeground(Color.WHITE);
        configButton.setFocusPainted(false);
        configButton.setBorderPainted(false);
        configButton.setOpaque(true);
        configButton.setCursor(new Cursor(Cursor.HAND_CURSOR));
        configButton.setPreferredSize(new Dimension(200, 32));
        
        configButton.addActionListener(e -> openConfigDialog());
    }

    private void setupLayout() {
        JPanel topPanel = new JPanel(new GridLayout(4, 1, 5, 5));
        topPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        topPanel.add(statusLabel);
        topPanel.add(consumptionLabel);
        topPanel.add(devicesLabel);
        topPanel.add(powerStateLabel);
        
        JPanel centerPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        centerPanel.add(powerIndicator);
        
        // панель с кнопкой конфигурации
        JPanel configPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 10));
        configPanel.add(configButton);
        
        JPanel legendPanel = createLegendPanel();
        
        // компоновка: добавлена панель с кнопкой
        JPanel bottomPanel = new JPanel(new BorderLayout(5, 5));
        bottomPanel.add(configPanel, BorderLayout.NORTH);
        bottomPanel.add(legendPanel, BorderLayout.CENTER);
        
        add(topPanel, BorderLayout.NORTH);
        add(centerPanel, BorderLayout.CENTER);
        add(bottomPanel, BorderLayout.SOUTH);
    }

    /**
     * Открытие диалога конфигурации индикатора с применением новых настроек.
     */
    private void openConfigDialog() {
        LevelIndicatorConfig currentConfig = powerIndicator.getConfig();
        
        Frame parentFrame = (Frame) SwingUtilities.getWindowAncestor(this);
        PowerIndicatorConfigDialog dialog = new PowerIndicatorConfigDialog(parentFrame, currentConfig);
        dialog.setVisible(true);
        
        if (dialog.isConfirmed()) {
            try {
                // Создаем новую конфигурацию
                LevelIndicatorConfig newConfig = new LevelIndicatorConfig.Builder()
                    .setMinValue(0.0)
                    .setMaxValue(dialog.getMaxPower())
                    .setCriticalRange(dialog.getCriticalLow(), dialog.getCriticalHigh())
                    .setWarningRange(dialog.getWarningLow(), dialog.getWarningHigh())
                    .build();
                
                // Применяем к индикатору
                powerIndicator.updateConfig(newConfig);
                
                // Обновляем текст максимума
                double currentPower = powerSystem.getTotalConsumption();
                consumptionLabel.setText(String.format("%.0f Вт / %.0f Вт", 
                    currentPower, 
                    dialog.getMaxPower()));
                
                // Обновляем легенду
                updateLegend(newConfig);
                
                JOptionPane.showMessageDialog(
                    this,
                    "Конфигурация индикатора успешно обновлена!",
                    "Успех",
                    JOptionPane.INFORMATION_MESSAGE
                );
                
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(
                    this,
                    "Ошибка применения конфигурации:\n" + ex.getMessage(),
                    "Ошибка",
                    JOptionPane.ERROR_MESSAGE
                );
            }
        }
    }

    private JPanel createLegendPanel() {
        JPanel panel = new JPanel(new GridLayout(3, 1, 3, 3));
        panel.setBorder(BorderFactory.createCompoundBorder(
            new TitledBorder("Зоны нагрузки"),
            BorderFactory.createEmptyBorder(5, 5, 5, 5)
        ));

        LevelIndicatorConfig config = powerIndicator.getConfig();
        
        JLabel normalLabel = createLegendLabel(
            String.format("Норма: %.0f-%.0f Вт", config.getWarningLow(), config.getWarningHigh()), 
            new Color(56, 142, 60)
        );
        JLabel warningLabel = createLegendLabel(
            String.format("Предупреждение: %.0f-%.0f, %.0f-%.0f Вт", 
                config.getCriticalLow(), config.getWarningLow(),
                config.getWarningHigh(), config.getCriticalHigh()), 
            new Color(230, 170, 0)
        );
        JLabel criticalLabel = createLegendLabel(
            String.format("Критично: 0-%.0f, %.0f-%.0f Вт", 
                config.getCriticalLow(), config.getCriticalHigh(), config.getMaxValue()), 
            new Color(211, 47, 47)
        );
        
        panel.add(normalLabel);
        panel.add(warningLabel);
        panel.add(criticalLabel);
        
        return panel;
    }

    private void updateLegend(LevelIndicatorConfig config) {
        Component[] components = getComponents();
        for (Component comp : components) {
            if (comp instanceof JPanel) {
                JPanel panel = (JPanel) comp;
                if (panel.getBorder() instanceof BorderLayout) {
                    Component south = ((BorderLayout) panel.getLayout()).getLayoutComponent(BorderLayout.CENTER);
                    if (south instanceof JPanel && ((JPanel) south).getBorder() instanceof TitledBorder) {
                        TitledBorder border = (TitledBorder) ((JPanel) south).getBorder();
                        if ("Зоны нагрузки".equals(border.getTitle())) {
                            panel.remove(south);
                            panel.add(createLegendPanel(), BorderLayout.CENTER);
                            panel.revalidate();
                            panel.repaint();
                            break;
                        }
                    }
                }
            }
        }
    }

    private JLabel createLegendLabel(String text, Color color) {
        JLabel label = new JLabel(text);
        label.setFont(new Font("Arial", Font.BOLD, 11));
        label.setForeground(color);
        return label;
    }

    @Override
    public void onConsumptionChanged(ConsumptionEvent event) {
        SwingUtilities.invokeLater(() -> {
            double totalPower = event.getTotalPower();
            int activeDevices = event.getActiveDevices();
            double maxPower = powerIndicator.getConfig().getMaxValue();  // Динамический максимум
            
            powerIndicator.setValue(totalPower);
            consumptionLabel.setText(String.format("%.0f Вт / %.0f Вт", totalPower, maxPower));
            devicesLabel.setText(String.format("Устройств: %d (активных: %d)", 
                powerSystem.getDeviceCount(), activeDevices));

            double percentage = (totalPower / maxPower) * 100.0;
            
            if (percentage >= 96) {
                statusLabel.setText("[!] КРИТИЧЕСКАЯ НАГРУЗКА!");
                statusLabel.setForeground(new Color(244, 67, 54));
            } else if (percentage >= 80) {
                statusLabel.setText("[!] Высокая нагрузка");
                statusLabel.setForeground(new Color(255, 193, 7));
            } else if (totalPower <= 0.1 && activeDevices == 0) {
                statusLabel.setText("Система готова");
                statusLabel.setForeground(new Color(76, 175, 80));
            } else if (percentage < 10) {
                statusLabel.setText("Низкая нагрузка");
                statusLabel.setForeground(new Color(76, 175, 80));
            } else {
                statusLabel.setText("Нагрузка в норме");
                statusLabel.setForeground(new Color(76, 175, 80));
            }
        });
    }

    @Override
    public void onOverload(OverloadEvent event) {
        SwingUtilities.invokeLater(() -> {
            String message = String.format(
                "[!] ПЕРЕГРУЗКА СЕТИ!\n\n" +
                "Текущее потребление: %.0f Вт\n" +
                "Лимит системы: %.0f Вт\n" +
                "Превышение: +%.0f Вт (+%.1f%%)\n\n" +
                "Питание автоматически отключено!",
                event.getCurrentPower(),
                event.getMaxPower(),
                event.getOverloadAmount(),
                event.getOverloadPercent()
            );
            
            JOptionPane.showMessageDialog(
                this,
                message,
                "Предупреждение системы",
                JOptionPane.WARNING_MESSAGE
            );
            
            statusLabel.setText("[X] ПЕРЕГРУЗКА!");
            statusLabel.setForeground(Color.RED);
        });
    }

    @Override
    public void onPowerStateChanged(PowerEvent event) {
        SwingUtilities.invokeLater(() -> {
            boolean available = powerSystem.isPowerAvailable();
            
            if (available) {
                powerStateLabel.setText("[OK] Питание в норме");
                powerStateLabel.setBackground(new Color(76, 175, 80));
            } else {
                powerStateLabel.setText("[X] Питание отключено");
                powerStateLabel.setBackground(new Color(244, 67, 54));
            }
        });
    }

    @Override
    public void onBatteryLevelChanged(pw.ns2030.event.BatteryLevelEvent event) {
    }

    public void cleanup() {
        powerSystem.removeListener(this);
    }
}