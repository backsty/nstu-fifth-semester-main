package pw.ns2030.demo;

import pw.ns2030.component.ApplianceControlPanel;
import pw.ns2030.component.PowerMeterPanel;
import pw.ns2030.controller.*;
import pw.ns2030.model.*;

import javax.swing.*;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.ArrayList;
import java.util.List;

/**
 * Главное демонстрационное окно системы потребителей энергии.
 * Объединяет контроллер системы, панель потребления и панели устройств.
 */
public class PowerSystemDemo extends JFrame {
    private PowerSystemController powerSystem;
    private PowerMeterPanel powerMeterPanel;
    private JPanel devicesPanel;
    private List<ApplianceControlPanel> devicePanels;
    
    private int deviceCounter = 1;

    public PowerSystemDemo() {
        setTitle("Система потребителей электроэнергии - Лабораторная работа №4");
        setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        setLayout(new BorderLayout(10, 10));
        
        devicePanels = new ArrayList<>();
        
        initComponents();
        setupLayout();
        setupMenuBar();
        addWindowCloseListener();
        
        // Запуск системы
        powerSystem.start();
        
        pack();
        setMinimumSize(new Dimension(900, 600));
        setLocationRelativeTo(null);
        
        // Добавляем начальные устройства для демонстрации
        addInitialDevices();
    }

    private void initComponents() {
        powerSystem = new PowerSystemController();
        powerMeterPanel = new PowerMeterPanel(powerSystem);
        
        devicesPanel = new JPanel();
        devicesPanel.setLayout(new BoxLayout(devicesPanel, BoxLayout.Y_AXIS));
        devicesPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
    }

    private void setupLayout() {
        // Левая панель - общее потребление
        add(powerMeterPanel, BorderLayout.WEST);
        
        // Центральная панель - список устройств в scrollpane
        JScrollPane scrollPane = new JScrollPane(devicesPanel);
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        scrollPane.setBorder(BorderFactory.createTitledBorder("🔌 Подключенные устройства"));
        add(scrollPane, BorderLayout.CENTER);
        
        // Нижняя панель - кнопки управления
        JPanel controlPanel = createControlPanel();
        add(controlPanel, BorderLayout.SOUTH);
    }

    private JPanel createControlPanel() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 10));
        
        Font emojiFont = new Font("Segoe UI Emoji", Font.BOLD, 12);
        
        JButton addKettleBtn = new JButton("➕ Добавить чайник");
        addKettleBtn.setFont(emojiFont);
        addKettleBtn.setBackground(new Color(33, 150, 243));  // Синий
        addKettleBtn.setForeground(Color.WHITE);
        addKettleBtn.setOpaque(true);
        addKettleBtn.setBorderPainted(false);
        addKettleBtn.setFocusPainted(false);
        addKettleBtn.addActionListener(e -> addKettle());
        
        JButton addLampBtn = new JButton("➕ Добавить лампу");
        addLampBtn.setFont(emojiFont);
        addLampBtn.setBackground(new Color(156, 39, 176));  // Фиолетовый
        addLampBtn.setForeground(Color.WHITE);
        addLampBtn.setOpaque(true);
        addLampBtn.setBorderPainted(false);
        addLampBtn.setFocusPainted(false);
        addLampBtn.addActionListener(e -> addLamp());
        
        JButton addComputerBtn = new JButton("➕ Добавить компьютер");
        addComputerBtn.setFont(emojiFont);
        addComputerBtn.setBackground(new Color(0, 150, 136));  // Бирюзовый
        addComputerBtn.setForeground(Color.WHITE);
        addComputerBtn.setOpaque(true);
        addComputerBtn.setBorderPainted(false);
        addComputerBtn.setFocusPainted(false);
        addComputerBtn.addActionListener(e -> addComputer());
        
        JButton restorePowerBtn = new JButton("🔌 Восстановить питание");
        restorePowerBtn.setFont(emojiFont);
        restorePowerBtn.setBackground(new Color(255, 193, 7));  // Жёлтый
        restorePowerBtn.setForeground(Color.BLACK);
        restorePowerBtn.setOpaque(true);
        restorePowerBtn.setBorderPainted(false);
        restorePowerBtn.setFocusPainted(false);
        restorePowerBtn.addActionListener(e -> powerSystem.restorePower());
        
        panel.add(addKettleBtn);
        panel.add(addLampBtn);
        panel.add(addComputerBtn);
        panel.add(restorePowerBtn);
        
        return panel;
    }

    private void setupMenuBar() {
        JMenuBar menuBar = new JMenuBar();
        
        // Меню "Файл"
        JMenu fileMenu = new JMenu("Файл");
        JMenuItem exitItem = new JMenuItem("Выход");
        exitItem.addActionListener(e -> closeApplication());
        fileMenu.add(exitItem);
        
        // Меню "Устройства"
        JMenu devicesMenu = new JMenu("Устройства");
        JMenuItem addKettleItem = new JMenuItem("Добавить чайник");
        addKettleItem.addActionListener(e -> addKettle());
        JMenuItem addLampItem = new JMenuItem("Добавить лампу");
        addLampItem.addActionListener(e -> addLamp());
        JMenuItem addComputerItem = new JMenuItem("Добавить компьютер");
        addComputerItem.addActionListener(e -> addComputer());
        JMenuItem removeAllItem = new JMenuItem("Удалить все устройства");
        removeAllItem.addActionListener(e -> removeAllDevices());
        
        devicesMenu.add(addKettleItem);
        devicesMenu.add(addLampItem);
        devicesMenu.add(addComputerItem);
        devicesMenu.addSeparator();
        devicesMenu.add(removeAllItem);
        
        // Меню "Система"
        JMenu systemMenu = new JMenu("Система");
        JMenuItem restorePowerItem = new JMenuItem("Восстановить питание");
        restorePowerItem.addActionListener(e -> powerSystem.restorePower());
        JMenuItem statsItem = new JMenuItem("Показать статистику");
        statsItem.addActionListener(e -> showSystemStats());
        
        systemMenu.add(restorePowerItem);
        systemMenu.add(statsItem);
        
        // Меню "Справка"
        JMenu helpMenu = new JMenu("Справка");
        JMenuItem aboutItem = new JMenuItem("О программе");
        aboutItem.addActionListener(e -> showAbout());
        helpMenu.add(aboutItem);
        
        menuBar.add(fileMenu);
        menuBar.add(devicesMenu);
        menuBar.add(systemMenu);
        menuBar.add(helpMenu);
        
        setJMenuBar(menuBar);
    }

    private void addWindowCloseListener() {
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                closeApplication();
            }
        });
    }

    private void addInitialDevices() {
        addKettle();
        addLamp();
        addComputer();
    }

    private void addKettle() {
        String id = "kettle-" + deviceCounter++;
        Kettle kettle = new Kettle(id, "Чайник #" + (deviceCounter - 1));
        KettleController controller = new KettleController(kettle);
        addDevice(controller);
    }

    private void addLamp() {
        String id = "lamp-" + deviceCounter++;
        Lamp lamp = new Lamp(id, "Лампа #" + (deviceCounter - 1));
        LampController controller = new LampController(lamp);
        addDevice(controller);
    }

    private void addComputer() {
        String id = "computer-" + deviceCounter++;
        Computer computer = new Computer(id, "Компьютер #" + (deviceCounter - 1));
        ComputerController controller = new ComputerController(computer);
        addDevice(controller);
    }

    private void addDevice(ApplianceController controller) {
        ApplianceControlPanel panel = new ApplianceControlPanel(controller);
        
        // Подписка на событие удаления устройства
        panel.addPropertyChangeListener("removeDevice", evt -> {
            ApplianceController ctrlToRemove = (ApplianceController) evt.getNewValue();
            removeDevice(panel, ctrlToRemove);
        });
        
        devicePanels.add(panel);
        devicesPanel.add(panel);
        devicesPanel.add(Box.createVerticalStrut(10));
        
        powerSystem.addDevice(controller);
        
        devicesPanel.revalidate();
        devicesPanel.repaint();
    }

    private void removeDevice(ApplianceControlPanel panel, ApplianceController controller) {
        devicePanels.remove(panel);
        devicesPanel.remove(panel);
        powerSystem.removeDevice(controller);
        
        devicesPanel.revalidate();
        devicesPanel.repaint();
    }

    private void removeAllDevices() {
        int result = JOptionPane.showConfirmDialog(
            this,
            "Удалить все устройства?",
            "Подтверждение",
            JOptionPane.YES_NO_OPTION
        );
        
        if (result == JOptionPane.YES_OPTION) {
            List<ApplianceControlPanel> panelsCopy = new ArrayList<>(devicePanels);
            for (ApplianceControlPanel panel : panelsCopy) {
                removeDevice(panel, panel.getController());
            }
        }
    }

    private void showSystemStats() {
        String message = String.format(
            "Статистика системы:\n\n" +
            "Текущее потребление: %.0f Вт\n" +
            "Лимит системы: %.0f Вт\n" +
            "Загрузка: %.1f%%\n\n" +
            "Всего устройств: %d\n" +
            "Активных устройств: %d\n\n" +
            "Состояние питания: %s",
            powerSystem.getTotalConsumption(),
            powerSystem.getMaxPower(),
            (powerSystem.getTotalConsumption() / powerSystem.getMaxPower()) * 100.0,
            powerSystem.getDeviceCount(),
            (int) powerSystem.getDevices().stream().filter(Appliance::isOn).count(),
            powerSystem.isPowerAvailable() ? "✅ Включено" : "❌ Отключено"
        );
        
        JOptionPane.showMessageDialog(
            this,
            message,
            "Статистика системы",
            JOptionPane.INFORMATION_MESSAGE
        );
    }

    private void showAbout() {
        String message = 
            "Система потребителей электроэнергии\n\n" +
            "Лабораторная работа №4\n" +
            "Студент: Шаламов А.Е.\n" +
            "Группа: АВТ-343\n\n" +
            "Возможности:\n" +
            "• Динамическое добавление устройств\n" +
            "• Мониторинг потребления в реальном времени\n" +
            "• Автоматическая защита от перегрузки\n" +
            "• Модели поведения устройств\n" +
            "• ИБП с моделью батареи\n\n" +
            "(c) 2025 НГТУ";
        
        JOptionPane.showMessageDialog(
            this,
            message,
            "О программе",
            JOptionPane.INFORMATION_MESSAGE
        );
    }

    private void closeApplication() {
        int result = JOptionPane.showConfirmDialog(
            this,
            "Завершить работу программы?",
            "Выход",
            JOptionPane.YES_NO_OPTION
        );
        
        if (result == JOptionPane.YES_OPTION) {
            powerMeterPanel.cleanup();
            powerSystem.shutdown();
            dispose();
            System.exit(0);
        }
    }
}