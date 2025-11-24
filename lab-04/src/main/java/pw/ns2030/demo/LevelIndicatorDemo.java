package pw.ns2030.demo;

import pw.ns2030.component.LevelIndicator;
import pw.ns2030.component.LevelIndicatorConfig;

import javax.swing.*;
import java.awt.*;

/**
 * Демонстрационное приложение для индикатора уровня.
 * Реализует паттерн MVC: индикатор (Model), панель управления (Controller), окно (View).
 * 
 * Структура:
 * - Центральная область: сам индикатор + легенда зон
 * - Правая панель: элементы управления
 * - Меню: дополнительные операции
 */
public class LevelIndicatorDemo extends JFrame {
    private LevelIndicator indicator;
    private DataSimulator simulator;
    private ControlPanel controlPanel;

    public LevelIndicatorDemo() {
        setTitle("Индикатор уровня - Лабораторная работа №3");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout(10, 10));

        initComponents();
        setupLayout();
        setupMenuBar();

        pack(); // Подгоняем размер под содержимое
        setMinimumSize(new Dimension(600, 600));
        setLocationRelativeTo(null);
        setResizable(true);
    }

    /**
     * Создание основных компонентов с конфигурацией индикатора.
     * Builder pattern упрощает настройку зон.
     */
    private void initComponents() {
        LevelIndicatorConfig config = new LevelIndicatorConfig.Builder()
                .setMinValue(0.0)
                .setMaxValue(100.0)
                .setCriticalRange(10.0, 90.0)
                .setWarningRange(20.0, 80.0)
                .build();

        indicator = new LevelIndicator(config);
        indicator.setValue(50.0);

        simulator = new DataSimulator(0.0, 100.0, 50.0);

        controlPanel = new ControlPanel(indicator, simulator, this);
    }

    /**
     * Компоновка интерфейса: индикатор по центру, управление справа.
     */
    private void setupLayout() {
        JPanel centerPanel = new JPanel(new BorderLayout());
        centerPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        
        JPanel indicatorWrapper = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 10));
        indicatorWrapper.add(indicator);
        centerPanel.add(indicatorWrapper, BorderLayout.CENTER);

        JPanel infoPanel = createInfoPanel();
        centerPanel.add(infoPanel, BorderLayout.SOUTH);

        add(centerPanel, BorderLayout.CENTER);
        add(controlPanel, BorderLayout.EAST);
    }

    /**
     * Легенда цветовых зон - отображается под индикатором.
     */
    private JPanel createInfoPanel() {
        JPanel panel = new JPanel(new GridLayout(1, 3, 10, 0));
        panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        JLabel normalLabel = createZoneLabel("● Норма: 20-80", new Color(76, 175, 80));
        JLabel warningLabel = createZoneLabel("● Предупреждение: 10-20, 80-90", new Color(255, 193, 7));
        JLabel criticalLabel = createZoneLabel("● Авария: 0-10, 90-100", new Color(244, 67, 54));

        panel.add(normalLabel);
        panel.add(warningLabel);
        panel.add(criticalLabel);

        return panel;
    }

    private JLabel createZoneLabel(String text, Color color) {
        JLabel label = new JLabel(text, SwingConstants.CENTER);
        label.setFont(new Font("Arial", Font.PLAIN, 11));
        label.setForeground(color);
        return label;
    }

    /**
     * Меню приложения: управление данными, настройки вида, справка.
     */
    private void setupMenuBar() {
        JMenuBar menuBar = new JMenuBar();

        // Файл: базовые операции
        JMenu fileMenu = new JMenu("Файл");
        JMenuItem exitItem = new JMenuItem("Выход");
        exitItem.addActionListener(e -> {
            simulator.stop();
            System.exit(0);
        });
        fileMenu.add(exitItem);

        // Данные: операции с накопленной историей
        JMenu dataMenu = new JMenu("Данные");
        JMenuItem clearItem = new JMenuItem("Очистить историю");
        clearItem.addActionListener(e -> {
            indicator.clearData();
            JOptionPane.showMessageDialog(this, "История данных очищена", "Информация", JOptionPane.INFORMATION_MESSAGE);
        });
        JMenuItem exportItem = new JMenuItem("Экспортировать данные");
        exportItem.addActionListener(e -> exportData());
        dataMenu.add(clearItem);
        dataMenu.add(exportItem);

        // Вид: переключатели элементов отображения
        JMenu viewMenu = new JMenu("Вид");
        JCheckBoxMenuItem showScaleItem = new JCheckBoxMenuItem("Показать шкалу", true);
        showScaleItem.addActionListener(e -> indicator.setShowScale(showScaleItem.isSelected()));
        JCheckBoxMenuItem showValueItem = new JCheckBoxMenuItem("Показать значение", true);
        showValueItem.addActionListener(e -> indicator.setShowValue(showValueItem.isSelected()));
        JCheckBoxMenuItem showBordersItem = new JCheckBoxMenuItem("Показать границы зон", true);
        showBordersItem.addActionListener(e -> indicator.setShowZoneBorders(showBordersItem.isSelected()));
        viewMenu.add(showScaleItem);
        viewMenu.add(showValueItem);
        viewMenu.add(showBordersItem);

        // Справка: информация о программе
        JMenu helpMenu = new JMenu("Справка");
        JMenuItem aboutItem = new JMenuItem("О программе");
        aboutItem.addActionListener(e -> showAbout());
        helpMenu.add(aboutItem);

        menuBar.add(fileMenu);
        menuBar.add(dataMenu);
        menuBar.add(viewMenu);
        menuBar.add(helpMenu);

        setJMenuBar(menuBar);
    }

    /**
     * Экспорт накопленных данных в CSV формат через диалоговое окно.
     */
    private void exportData() {
        var data = indicator.getAccumulatedData();
        if (data.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Нет данных для экспорта", "Предупреждение", JOptionPane.WARNING_MESSAGE);
            return;
        }

        StringBuilder sb = new StringBuilder();
        sb.append("Timestamp,Value,Zone\n");
        for (var item : data) {
            sb.append(String.format("%s,%.2f,%s\n",
                    item.getFormattedTimestamp(),
                    item.getValue(),
                    item.getZone().getDisplayName()));
        }

        JTextArea textArea = new JTextArea(sb.toString(), 20, 50);
        textArea.setEditable(false);
        textArea.setFont(new Font("Monospaced", Font.PLAIN, 12));
        JScrollPane scrollPane = new JScrollPane(textArea);

        JOptionPane.showMessageDialog(this, scrollPane, "Экспорт данных (CSV)", JOptionPane.PLAIN_MESSAGE);
    }

    /**
     * Информация о программе и ее возможностях.
     */
    private void showAbout() {
        String message = 
                "Индикатор уровня с динамическим поведением\n\n" +
                "Лабораторная работа №3\n" +
                "Студент: Шаламов А.Е.\n" +
                "Группа: АВТ-343\n\n" +
                "Возможности:\n" +
                "• Визуальное отображение уровня\n" +
                "• Три цветовые зоны\n" +
                "• Накопление данных\n" +
                "• Симулятор значений\n" +
                "• Статистика\n\n" +
                "(c) 2025 НГТУ";

        JOptionPane.showMessageDialog(this, message, "О программе", JOptionPane.INFORMATION_MESSAGE);
    }

    /**
     * Точка входа - запуск в EDT потоке с системным Look&Feel.
     */
    public static void main(String[] args) {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception e) {
            e.printStackTrace();
        }

        SwingUtilities.invokeLater(() -> {
            LevelIndicatorDemo demo = new LevelIndicatorDemo();
            demo.setVisible(true);
        });
    }
}