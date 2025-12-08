package pw.ns2030.demo;

import pw.ns2030.component.LevelIndicator;
import pw.ns2030.component.LevelIndicatorConfig;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import java.awt.*;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Панель управления индикатором - реализует MVC-подход.
 * Объединяет ручное управление через слайдер и автоматический симулятор.
 * 
 * НОВОЕ: Добавлена секция редактирования зон (min/max, critical, warning).
 * 
 * Ключевая особенность: защита от циклических обновлений между
 * слайдером и индикатором через флаг isUpdatingSlider.
 */
public class ControlPanel extends JPanel {
    private final LevelIndicator indicator;
    private final DataSimulator simulator;
    private final JFrame parentFrame;
    private final AtomicBoolean isUpdatingSlider = new AtomicBoolean(false);  // Защита от рекурсии

    private JSlider valueSlider;
    private JLabel valueLabel;
    private JButton startStopButton;
    private JSpinner frequencySpinner;
    private JCheckBox smoothCheckBox;
    private JLabel statusLabel;

    // НОВЫЕ поля для редактирования зон
    private JSpinner minValueSpinner;
    private JSpinner maxValueSpinner;
    private JSpinner criticalLowSpinner;
    private JSpinner criticalHighSpinner;
    private JSpinner warningLowSpinner;
    private JSpinner warningHighSpinner;
    private JButton applyZonesButton;

    public ControlPanel(LevelIndicator indicator, DataSimulator simulator, JFrame parentFrame) {
        this.indicator = indicator;
        this.simulator = simulator;
        this.parentFrame = parentFrame;

        setLayout(new BorderLayout(10, 10));
        setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        setPreferredSize(new Dimension(280, 700));  // УВЕЛИЧЕНО для новой секции

        initComponents();
        setupListeners();
    }

    /**
     * Инициализация структуры панели - вертикальная компоновка секций.
     */
    private void initComponents() {
        JPanel mainPanel = new JPanel();
        mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.Y_AXIS));

        mainPanel.add(createManualControlPanel());
        mainPanel.add(Box.createVerticalStrut(10));
        mainPanel.add(createSimulatorPanel());
        mainPanel.add(Box.createVerticalStrut(10));
        mainPanel.add(createPresetPanel());
        mainPanel.add(Box.createVerticalStrut(10));
        mainPanel.add(createZoneEditorPanel());  // НОВАЯ секция
        mainPanel.add(Box.createVerticalStrut(10));
        mainPanel.add(createStatusPanel());

        JScrollPane scrollPane = new JScrollPane(mainPanel);
        scrollPane.setBorder(null);
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        add(scrollPane, BorderLayout.CENTER);
    }

    /**
     * Секция ручного управления - слайдер с отображением значения.
     */
    private JPanel createManualControlPanel() {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBorder(new TitledBorder("Ручное управление"));

        LevelIndicatorConfig config = indicator.getConfig();

        valueSlider = new JSlider(
                (int) config.getMinValue(),
                (int) config.getMaxValue(),
                (int) indicator.getValue()
        );
        valueSlider.setMajorTickSpacing(20);
        valueSlider.setMinorTickSpacing(5);
        valueSlider.setPaintTicks(true);
        valueSlider.setPaintLabels(true);

        valueLabel = new JLabel(String.format("Значение: %.1f", indicator.getValue()), SwingConstants.CENTER);
        valueLabel.setFont(new Font("Arial", Font.BOLD, 14));
        valueLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        panel.add(valueLabel);
        panel.add(Box.createVerticalStrut(5));
        panel.add(valueSlider);

        return panel;
    }

    /**
     * Секция симулятора - старт/стоп и настройки генерации.
     * GridBagLayout обеспечивает центрирование элементов.
     */
    private JPanel createSimulatorPanel() {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBorder(new TitledBorder("Симулятор"));

        JPanel buttonPanel = new JPanel(new GridBagLayout());
        startStopButton = new JButton("► Запустить");
        startStopButton.setPreferredSize(new Dimension(220, 35));
        startStopButton.setBackground(new Color(76, 175, 80));
        startStopButton.setForeground(Color.WHITE);
        startStopButton.setFocusPainted(false);
        startStopButton.setFont(new Font("Arial", Font.BOLD, 12));
        startStopButton.setOpaque(true);
        startStopButton.setBorderPainted(false);
        buttonPanel.add(startStopButton);

        JPanel freqPanel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.anchor = GridBagConstraints.CENTER;
        gbc.insets = new Insets(0, 5, 0, 5);
        
        JLabel freqLabel = new JLabel("Частота (мс):");
        freqPanel.add(freqLabel, gbc);
        
        gbc.gridx = 1;
        frequencySpinner = new JSpinner(new SpinnerNumberModel(1000, 100, 5000, 100));
        ((JSpinner.DefaultEditor) frequencySpinner.getEditor()).getTextField().setColumns(5);
        freqPanel.add(frequencySpinner, gbc);

        JPanel checkPanel = new JPanel(new GridBagLayout());
        smoothCheckBox = new JCheckBox("Плавное изменение", true);
        checkPanel.add(smoothCheckBox);

        panel.add(buttonPanel);
        panel.add(Box.createVerticalStrut(10));
        panel.add(freqPanel);
        panel.add(Box.createVerticalStrut(5));
        panel.add(checkPanel);

        return panel;
    }

    /**
     * Секция предустановленных значений для быстрой проверки зон.
     */
    private JPanel createPresetPanel() {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBorder(new TitledBorder("Быстрые установки"));

        JButton btnNormal = createPresetButton("Норма (50)");
        JButton btnWarning = createPresetButton("Предупреждение (25)");
        JButton btnCritical = createPresetButton("Авария (5)");
        JButton btnReset = createPresetButton("Сброс");

        btnNormal.addActionListener(e -> setValueAndUpdate(50));
        btnWarning.addActionListener(e -> setValueAndUpdate(25));
        btnCritical.addActionListener(e -> setValueAndUpdate(5));
        btnReset.addActionListener(e -> {
            setValueAndUpdate(50);
            if (simulator.isRunning()) {
                simulator.stop();
                updateStartStopButton();
            }
        });

        panel.add(createCenteredButtonPanel(btnNormal));
        panel.add(Box.createVerticalStrut(5));
        panel.add(createCenteredButtonPanel(btnWarning));
        panel.add(Box.createVerticalStrut(5));
        panel.add(createCenteredButtonPanel(btnCritical));
        panel.add(Box.createVerticalStrut(5));
        panel.add(createCenteredButtonPanel(btnReset));

        return panel;
    }

    /**
     * НОВАЯ СЕКЦИЯ: Редактор границ зон с валидацией.
     * Позволяет изменять min/max, critical и warning диапазоны в реальном времени.
     */
    private JPanel createZoneEditorPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBorder(new TitledBorder("Настройка зон"));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(3, 5, 3, 5);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        LevelIndicatorConfig config = indicator.getConfig();

        // Строка 0: Min Value
        gbc.gridx = 0; gbc.gridy = 0; gbc.weightx = 0.4;
        panel.add(new JLabel("Min:"), gbc);
        gbc.gridx = 1; gbc.weightx = 0.6;
        minValueSpinner = new JSpinner(new SpinnerNumberModel(config.getMinValue(), -1000.0, 1000.0, 1.0));
        panel.add(minValueSpinner, gbc);

        // Строка 1: Max Value
        gbc.gridx = 0; gbc.gridy = 1; gbc.weightx = 0.4;
        panel.add(new JLabel("Max:"), gbc);
        gbc.gridx = 1; gbc.weightx = 0.6;
        maxValueSpinner = new JSpinner(new SpinnerNumberModel(config.getMaxValue(), -1000.0, 1000.0, 1.0));
        panel.add(maxValueSpinner, gbc);

        // Строка 2: Critical Low
        gbc.gridx = 0; gbc.gridy = 2; gbc.weightx = 0.4;
        panel.add(new JLabel("Крит. низ:"), gbc);
        gbc.gridx = 1; gbc.weightx = 0.6;
        criticalLowSpinner = new JSpinner(new SpinnerNumberModel(config.getCriticalLow(), -1000.0, 1000.0, 1.0));
        panel.add(criticalLowSpinner, gbc);

        // Строка 3: Critical High
        gbc.gridx = 0; gbc.gridy = 3; gbc.weightx = 0.4;
        panel.add(new JLabel("Крит. верх:"), gbc);
        gbc.gridx = 1; gbc.weightx = 0.6;
        criticalHighSpinner = new JSpinner(new SpinnerNumberModel(config.getCriticalHigh(), -1000.0, 1000.0, 1.0));
        panel.add(criticalHighSpinner, gbc);

        // Строка 4: Warning Low
        gbc.gridx = 0; gbc.gridy = 4; gbc.weightx = 0.4;
        panel.add(new JLabel("Предупр. низ:"), gbc);
        gbc.gridx = 1; gbc.weightx = 0.6;
        warningLowSpinner = new JSpinner(new SpinnerNumberModel(config.getWarningLow(), -1000.0, 1000.0, 1.0));
        panel.add(warningLowSpinner, gbc);

        // Строка 5: Warning High
        gbc.gridx = 0; gbc.gridy = 5; gbc.weightx = 0.4;
        panel.add(new JLabel("Предупр. верх:"), gbc);
        gbc.gridx = 1; gbc.weightx = 0.6;
        warningHighSpinner = new JSpinner(new SpinnerNumberModel(config.getWarningHigh(), -1000.0, 1000.0, 1.0));
        panel.add(warningHighSpinner, gbc);

        // Строка 6: Кнопка применения
        gbc.gridx = 0; gbc.gridy = 6; gbc.gridwidth = 2;
        applyZonesButton = new JButton("✓ Применить зоны");
        applyZonesButton.setBackground(new Color(33, 150, 243));
        applyZonesButton.setForeground(Color.WHITE);
        applyZonesButton.setFocusPainted(false);
        applyZonesButton.setOpaque(true);
        applyZonesButton.setBorderPainted(false);
        panel.add(applyZonesButton, gbc);

        applyZonesButton.addActionListener(e -> applyZoneChanges());

        return panel;
    }

    /**
     * НОВЫЙ МЕТОД: Применение изменений зон с валидацией.
     * Показывает ошибки через диалоги, обновляет слайдер при изменении диапазона.
     */
    private void applyZoneChanges() {
        try {
            // ✅ ИСПРАВЛЕНИЕ #6: Валидация NaN/Infinity перед передачей в Builder
            double min = ((Number) minValueSpinner.getValue()).doubleValue();
            double max = ((Number) maxValueSpinner.getValue()).doubleValue();
            double critLow = ((Number) criticalLowSpinner.getValue()).doubleValue();
            double critHigh = ((Number) criticalHighSpinner.getValue()).doubleValue();
            double warnLow = ((Number) warningLowSpinner.getValue()).doubleValue();
            double warnHigh = ((Number) warningHighSpinner.getValue()).doubleValue();

            // Проверка на NaN и Infinity
            if (Double.isNaN(min) || Double.isInfinite(min)) {
                throw new IllegalArgumentException("Min не может быть NaN/Infinity");
            }
            if (Double.isNaN(max) || Double.isInfinite(max)) {
                throw new IllegalArgumentException("Max не может быть NaN/Infinity");
            }
            if (Double.isNaN(critLow) || Double.isInfinite(critLow)) {
                throw new IllegalArgumentException("Critical Low не может быть NaN/Infinity");
            }
            if (Double.isNaN(critHigh) || Double.isInfinite(critHigh)) {
                throw new IllegalArgumentException("Critical High не может быть NaN/Infinity");
            }
            if (Double.isNaN(warnLow) || Double.isInfinite(warnLow)) {
                throw new IllegalArgumentException("Warning Low не может быть NaN/Infinity");
            }
            if (Double.isNaN(warnHigh) || Double.isInfinite(warnHigh)) {
                throw new IllegalArgumentException("Warning High не может быть NaN/Infinity");
            }

            // Создаем новую конфигурацию (Builder валидирует порядок)
            LevelIndicatorConfig newConfig = new LevelIndicatorConfig.Builder()
                    .setMinValue(min)
                    .setMaxValue(max)
                    .setCriticalLow(critLow)
                    .setCriticalHigh(critHigh)
                    .setWarningLow(warnLow)
                    .setWarningHigh(warnHigh)
                    .build();

            // Применяем к индикатору
            indicator.updateConfig(newConfig);

            // Обновляем диапазон слайдера
            isUpdatingSlider.set(true);
            try {
                valueSlider.setMinimum((int) min);
                valueSlider.setMaximum((int) max);
                
                // Корректируем текущее значение если вышло за границы
                double currentValue = indicator.getValue();
                if (currentValue < min || currentValue > max) {
                    double clampedValue = Math.max(min, Math.min(max, currentValue));
                    indicator.setValue(clampedValue);
                    valueSlider.setValue((int) clampedValue);
                }
            } finally {
                isUpdatingSlider.set(false);
            }

            // Уведомление об успехе
            JOptionPane.showMessageDialog(
                    this,
                    "Зоны успешно обновлены!",
                    "Успех",
                    JOptionPane.INFORMATION_MESSAGE
            );

        } catch (IllegalStateException ex) {
            // Ошибка валидации из Builder
            JOptionPane.showMessageDialog(
                    this,
                    "Ошибка валидации зон:\n" + ex.getMessage(),
                    "Некорректные значения",
                    JOptionPane.ERROR_MESSAGE
            );
        } catch (IllegalArgumentException ex) {
            // Ошибка валидации NaN/Infinity
            JOptionPane.showMessageDialog(
                    this,
                    "Ошибка валидации:\n" + ex.getMessage(),
                    "Некорректные значения",
                    JOptionPane.ERROR_MESSAGE
            );
        } catch (Exception ex) {
            // Непредвиденная ошибка
            JOptionPane.showMessageDialog(
                    this,
                    "Неожиданная ошибка:\n" + ex.getMessage(),
                    "Ошибка",
                    JOptionPane.ERROR_MESSAGE
            );
            ex.printStackTrace();
        }
    }

    private JButton createPresetButton(String text) {
        JButton button = new JButton(text);
        button.setPreferredSize(new Dimension(220, 30));
        return button;
    }

    /**
     * Обертка для центрирования кнопки через GridBagLayout.
     */
    private JPanel createCenteredButtonPanel(JButton button) {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.add(button);
        return panel;
    }

    /**
     * Секция отображения текущего состояния и доступа к статистике.
     */
    private JPanel createStatusPanel() {
        JPanel panel = new JPanel(new BorderLayout(5, 5));
        panel.setBorder(new TitledBorder("Статус"));

        statusLabel = new JLabel("<html><center>Готов к работе</center></html>", SwingConstants.CENTER);
        statusLabel.setVerticalAlignment(SwingConstants.CENTER);
        statusLabel.setPreferredSize(new Dimension(250, 60));

        JPanel statsButtonPanel = new JPanel(new GridBagLayout());
        JButton statsButton = new JButton("Показать статистику");
        statsButton.setPreferredSize(new Dimension(220, 30));
        statsButton.addActionListener(e -> showStatistics());
        statsButtonPanel.add(statsButton);

        panel.add(statusLabel, BorderLayout.CENTER);
        panel.add(statsButtonPanel, BorderLayout.SOUTH);

        return panel;
    }

    /**
     * Настройка взаимодействия между компонентами.
     * Ключевой момент: флаг isUpdatingSlider предотвращает циклические обновления.
     */
    private void setupListeners() {
        // Слайдер -> Индикатор (только в ручном режиме)
        valueSlider.addChangeListener(e -> {
            if (isUpdatingSlider.get()) {
                return;  // Блокируем обработку при программном изменении
            }
            
            if (!simulator.isRunning()) {
                double value = valueSlider.getValue();
                indicator.setValue(value);
                simulator.setCurrentValue(value);
                updateValueLabel(value);
            }
        });

        startStopButton.addActionListener(e -> toggleSimulator());

        frequencySpinner.addChangeListener(e -> {
            int frequency = (Integer) frequencySpinner.getValue();
            simulator.setFrequency(frequency);
        });

        smoothCheckBox.addActionListener(e -> {
            simulator.setSmoothTransition(smoothCheckBox.isSelected());
        });

        // Симулятор -> Индикатор + GUI (защищено флагом от циклов)
        simulator.setDataUpdateListener(value -> {
            SwingUtilities.invokeLater(() -> {
                isUpdatingSlider.set(true);
                try {
                    indicator.setValue(value);
                    valueSlider.setValue((int) value);
                    updateValueLabel(value);
                } finally {
                    isUpdatingSlider.set(false);
                }
            });
        });

        // Индикатор -> Статус (реактивное обновление при изменении зоны)
        indicator.addLevelDataListener(event -> {
            SwingUtilities.invokeLater(() -> {
                String zoneName = event.getNewZone().getDisplayName();
                String status = String.format(
                    "<html><center>Зона: %s<br>Изменение: %+.1f</center></html>",
                    zoneName,
                    event.getValueChange()
                );
                statusLabel.setText(status);
                statusLabel.setForeground(event.getNewZone().getColor());
            });
        });
    }

    private void toggleSimulator() {
        if (simulator.isRunning()) {
            simulator.stop();
        } else {
            simulator.start();
        }
        updateStartStopButton();
    }

    /**
     * Обновление UI кнопки в зависимости от состояния симулятора.
     */
    private void updateStartStopButton() {
        if (simulator.isRunning()) {
            startStopButton.setText("■ Остановить");
            startStopButton.setBackground(new Color(244, 67, 54));
            valueSlider.setEnabled(false);
        } else {
            startStopButton.setText("► Запустить");
            startStopButton.setBackground(new Color(76, 175, 80));
            valueSlider.setEnabled(true);
        }
        startStopButton.setForeground(Color.WHITE);
        startStopButton.setOpaque(true);
        startStopButton.setBorderPainted(false);
    }

    /**
     * Централизованное изменение значения с защитой от циклических обновлений.
     */
    private void setValueAndUpdate(double value) {
        isUpdatingSlider.set(true);
        try {
            indicator.setValue(value);
            simulator.setCurrentValue(value);
            valueSlider.setValue((int) value);
            updateValueLabel(value);
        } finally {
            isUpdatingSlider.set(false);
        }
    }

    private void updateValueLabel(double value) {
        valueLabel.setText(String.format("Значение: %.1f", value));
    }

    /**
     * Отображение агрегированной статистики через модальное окно.
     */
    private void showStatistics() {
        var stats = indicator.getStatistics();
        String message = String.format(
                "Статистика накопленных данных:\n\n" +
                "Всего записей: %d\n" +
                "Среднее значение: %.2f\n" +
                "Минимум: %.2f\n" +
                "Максимум: %.2f\n\n" +
                "Распределение по зонам:\n" +
                "  Норма: %d (%.1f%%)\n" +
                "  Предупреждение: %d (%.1f%%)\n" +
                "  Авария: %d (%.1f%%)",
                stats.getTotalCount(),
                stats.getAverage(),
                stats.getMin(),
                stats.getMax(),
                stats.getNormalCount(),
                stats.getNormalPercentage(),
                stats.getWarningCount(),
                stats.getWarningPercentage(),
                stats.getCriticalCount(),
                stats.getCriticalPercentage()
        );

        JOptionPane.showMessageDialog(
                parentFrame,
                message,
                "Статистика индикатора",
                JOptionPane.INFORMATION_MESSAGE
        );
    }
}