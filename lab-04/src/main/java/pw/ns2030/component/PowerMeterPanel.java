package pw.ns2030.component;

import pw.ns2030.controller.PowerSystemController;
import pw.ns2030.event.ConsumptionEvent;
import pw.ns2030.event.OverloadEvent;
import pw.ns2030.event.PowerEvent;
import pw.ns2030.listener.PowerSystemListener;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import java.awt.*;

/**
 * Панель отображения общего потребления энергии системы.
 * Использует LevelIndicator из л.р.3 для визуализации нагрузки.
 * Реагирует на события системы через паттерн Observer.
 */
public class PowerMeterPanel extends JPanel implements PowerSystemListener {
    private final PowerSystemController powerSystem;
    private final LevelIndicator powerIndicator;
    
    private JLabel statusLabel;
    private JLabel consumptionLabel;
    private JLabel devicesLabel;
    private JLabel powerStateLabel;

    public PowerMeterPanel(PowerSystemController powerSystem) {
        this.powerSystem = powerSystem;
        
        // Зоны нагрузки (от краев к центру):
        // 0-500 Вт (0-10%) = КРИТИЧЕСКАЯ (слишком низкая нагрузка)
        // 500-1000 Вт (10-20%) = ПРЕДУПРЕЖДЕНИЕ
        // 1000-4000 Вт (20-80%) = НОРМА
        // 4000-4500 Вт (80-90%) = ПРЕДУПРЕЖДЕНИЕ
        // 4500-5000 Вт (90-100%) = КРИТИЧЕСКАЯ (перегрузка)
        
        LevelIndicatorConfig config = new LevelIndicatorConfig.Builder()
            .setMinValue(0.0)
            .setMaxValue(5000.0)
            .setCriticalRange(500.0, 4500.0)
            .setWarningRange(1000.0, 4000.0)
            .build();
        
        this.powerIndicator = new LevelIndicator(config);
        this.powerIndicator.setValue(0.0);
        
        initComponents();
        setupLayout();
        
        // Подписка на события системы
        powerSystem.addListener(this);
    }

    private void initComponents() {
        setLayout(new BorderLayout(10, 10));
        setBorder(new TitledBorder("📊 Общее потребление энергии"));
        setPreferredSize(new Dimension(300, 600));
        
        statusLabel = new JLabel("Система готова", SwingConstants.CENTER);
        statusLabel.setFont(new Font("Arial", Font.BOLD, 14));
        statusLabel.setForeground(new Color(76, 175, 80));
        
        consumptionLabel = new JLabel("0 Вт / 5000 Вт", SwingConstants.CENTER);
        consumptionLabel.setFont(new Font("Arial", Font.PLAIN, 16));
        
        devicesLabel = new JLabel("Устройств: 0 (активных: 0)", SwingConstants.CENTER);
        devicesLabel.setFont(new Font("Arial", Font.PLAIN, 12));
        
        powerStateLabel = new JLabel("✅ Питание в норме", SwingConstants.CENTER);
        powerStateLabel.setFont(new Font("Arial", Font.BOLD, 12));
        powerStateLabel.setOpaque(true);
        powerStateLabel.setBackground(new Color(76, 175, 80));
        powerStateLabel.setForeground(Color.WHITE);
    }

    private void setupLayout() {
        // Верхняя панель - статус
        JPanel topPanel = new JPanel(new GridLayout(4, 1, 5, 5));
        topPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        topPanel.add(statusLabel);
        topPanel.add(consumptionLabel);
        topPanel.add(devicesLabel);
        topPanel.add(powerStateLabel);
        
        // Центральная панель - индикатор
        JPanel centerPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        centerPanel.add(powerIndicator);
        
        // Нижняя панель - легенда
        JPanel legendPanel = createLegendPanel();
        
        add(topPanel, BorderLayout.NORTH);
        add(centerPanel, BorderLayout.CENTER);
        add(legendPanel, BorderLayout.SOUTH);
    }

    private JPanel createLegendPanel() {
        JPanel panel = new JPanel(new GridLayout(3, 1, 3, 3));
        panel.setBorder(BorderFactory.createCompoundBorder(
            new TitledBorder("Зоны нагрузки"),
            BorderFactory.createEmptyBorder(5, 5, 5, 5)
        ));

        JLabel normalLabel = createLegendLabel("● Норма: 1000-4000 Вт (20-80%)", new Color(76, 175, 80));
        JLabel warningLabel = createLegendLabel("● Предупреждение: 500-1000, 4000-4500 Вт", new Color(255, 193, 7));
        JLabel criticalLabel = createLegendLabel("● Опасно: 0-500, 4500-5000 Вт", new Color(244, 67, 54));
        
        panel.add(normalLabel);
        panel.add(warningLabel);
        panel.add(criticalLabel);
        
        return panel;
    }

    private JLabel createLegendLabel(String text, Color color) {
        JLabel label = new JLabel(text);
        label.setFont(new Font("Arial", Font.PLAIN, 11));
        label.setForeground(color);
        return label;
    }

    // Реализация PowerSystemListener
    
    @Override
    public void onConsumptionChanged(ConsumptionEvent event) {
        SwingUtilities.invokeLater(() -> {
            double totalPower = event.getTotalPower();
            int activeDevices = event.getActiveDevices();
            
            // Обновление индикатора
            powerIndicator.setValue(totalPower);
            
            // Обновление текстовых меток
            consumptionLabel.setText(String.format("%.0f Вт / 5000 Вт", totalPower));
            devicesLabel.setText(String.format("Устройств: %d (активных: %d)", 
                powerSystem.getDeviceCount(), activeDevices));

            double percentage = (totalPower / 5000.0) * 100.0;
            if (percentage >= 90) {
                statusLabel.setText("⚠️ КРИТИЧЕСКАЯ НАГРУЗКА!");
                statusLabel.setForeground(new Color(244, 67, 54));
            } else if (percentage >= 80) {
                statusLabel.setText("⚠ Высокая нагрузка");
                statusLabel.setForeground(new Color(255, 193, 7));
            } else if (percentage <= 10) {
                statusLabel.setText("⚠ Низкая нагрузка");
                statusLabel.setForeground(new Color(255, 193, 7));
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
                "⚠️ ПЕРЕГРУЗКА СЕТИ!\n\n" +
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
            
            statusLabel.setText("❌ ПЕРЕГРУЗКА!");
            statusLabel.setForeground(Color.RED);
        });
    }

    @Override
    public void onPowerStateChanged(PowerEvent event) {
        SwingUtilities.invokeLater(() -> {
            boolean available = powerSystem.isPowerAvailable();
            
            if (available) {
                powerStateLabel.setText("✅ Питание в норме");
                powerStateLabel.setBackground(new Color(76, 175, 80));
            } else {
                powerStateLabel.setText("❌ Питание отключено");
                powerStateLabel.setBackground(new Color(244, 67, 54));
            }
        });
    }

    @Override
    public void onBatteryLevelChanged(pw.ns2030.event.BatteryLevelEvent event) {
        // Не используется в этой панели
    }

    /**
     * Отписка от событий при закрытии панели.
     */
    public void cleanup() {
        powerSystem.removeListener(this);
    }
}