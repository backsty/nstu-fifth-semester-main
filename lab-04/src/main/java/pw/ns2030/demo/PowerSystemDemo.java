package pw.ns2030.demo;

import pw.ns2030.component.ApplianceControlPanel;
import pw.ns2030.component.PowerMeterPanel;
import pw.ns2030.controller.*;
import pw.ns2030.model.*;
import pw.ns2030.dialog.*;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.ArrayList;
import java.util.List;

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
        
        powerSystem.start();
        
        pack();
        setMinimumSize(new Dimension(900, 700));
        setLocationRelativeTo(null);
        
        // addInitialDevices();
    }

    private void initComponents() {
        powerSystem = new PowerSystemController();
        powerMeterPanel = new PowerMeterPanel(powerSystem);
        
        devicesPanel = new JPanel();
        devicesPanel.setLayout(new BoxLayout(devicesPanel, BoxLayout.Y_AXIS));
        devicesPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
    }

    private void setupLayout() {
        add(powerMeterPanel, BorderLayout.WEST);

        devicesPanel = new JPanel();
        devicesPanel.setLayout(new BoxLayout(devicesPanel, BoxLayout.Y_AXIS));
        devicesPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        
        JScrollPane scrollPane = new JScrollPane(devicesPanel);
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        scrollPane.setPreferredSize(new Dimension(330, 700));
        scrollPane.setBorder(BorderFactory.createTitledBorder("Подключенные устройства"));
        
        scrollPane.getVerticalScrollBar().setUnitIncrement(30);
        scrollPane.getVerticalScrollBar().setBlockIncrement(90);
        
        add(scrollPane, BorderLayout.CENTER);
        
        JPanel controlPanel = createControlPanel();
        add(controlPanel, BorderLayout.SOUTH);
    }

    private JPanel createControlPanel() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 10));
        
        Font buttonFont = new Font("Arial", Font.BOLD, 12);
        
        JButton addKettleBtn = createStyledButton("+ Добавить чайник", new Color(33, 150, 243), new Color(25, 118, 210), buttonFont);
        addKettleBtn.addActionListener(e -> addKettle());
        
        JButton addLampBtn = createStyledButton("+ Добавить лампу", new Color(156, 39, 176), new Color(123, 31, 162), buttonFont);
        addLampBtn.addActionListener(e -> addLamp());
        
        JButton addComputerBtn = createStyledButton("+ Добавить компьютер", new Color(0, 150, 136), new Color(0, 121, 107), buttonFont);
        addComputerBtn.addActionListener(e -> addComputer());
        
        JButton restorePowerBtn = createStyledButton("Восстановить питание", new Color(76, 175, 80), new Color(56, 142, 60), buttonFont);
        restorePowerBtn.addActionListener(e -> powerSystem.restorePower());
        
        panel.add(addKettleBtn);
        panel.add(addLampBtn);
        panel.add(addComputerBtn);
        panel.add(restorePowerBtn);
        
        return panel;
    }

    private JButton createStyledButton(String text, Color bgColor, Color hoverColor, Font font) {
        JButton button = new JButton(text);
        button.setFont(font);
        button.setBackground(bgColor);
        button.setForeground(Color.WHITE);
        button.setFocusPainted(false);
        button.setBorderPainted(false);
        button.setOpaque(true);
        button.setPreferredSize(new Dimension(200, 35));
        button.setCursor(new Cursor(Cursor.HAND_CURSOR));
        
        button.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                button.setBackground(hoverColor);
            }
            
            @Override
            public void mouseExited(MouseEvent e) {
                button.setBackground(bgColor);
            }
        });
        
        return button;
    }

    private void setupMenuBar() {
        JMenuBar menuBar = new JMenuBar();
        
        JMenu fileMenu = new JMenu("Файл");
        JMenuItem exitItem = new JMenuItem("Выход");
        exitItem.addActionListener(e -> closeApplication());
        fileMenu.add(exitItem);
        
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
        
        JMenu systemMenu = new JMenu("Система");
        JMenuItem restorePowerItem = new JMenuItem("Восстановить питание");
        restorePowerItem.addActionListener(e -> powerSystem.restorePower());
        JMenuItem statsItem = new JMenuItem("Показать статистику");
        statsItem.addActionListener(e -> showSystemStats());
        
        systemMenu.add(restorePowerItem);
        systemMenu.add(statsItem);
        
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

    public void addInitialDevices() {
        addKettle();
        addLamp();
        addComputer();
    }

    /**
     * Добавление чайника через диалог.
     */
    private void addKettle() {
        String defaultName = "Чайник #" + deviceCounter;
        KettleConfigDialog dialog = new KettleConfigDialog(this, defaultName);
        dialog.setVisible(true);
        
        if (dialog.isConfirmed()) {
            String id = "kettle-" + deviceCounter++;
            String name = dialog.getDeviceName();
            double power = dialog.getPower();
            
            Kettle kettle = new Kettle(id, name, power);
            KettleController controller = new KettleController(kettle);
            addDevice(controller);
        }
    }

    /**
     *  Добавление лампы через диалог.
     */
    private void addLamp() {
        String defaultName = "Лампа #" + deviceCounter;
        LampConfigDialog dialog = new LampConfigDialog(this, defaultName);
        dialog.setVisible(true);
        
        if (dialog.isConfirmed()) {
            String id = "lamp-" + deviceCounter++;
            String name = dialog.getDeviceName();
            double power = dialog.getPower();
            
            Lamp lamp = new Lamp(id, name, power);
            LampController controller = new LampController(lamp);
            addDevice(controller);
        }
    }

    /**
     * Добавление компьютера через диалог.
     */
    private void addComputer() {
        String defaultName = "Компьютер #" + deviceCounter;
        ComputerConfigDialog dialog = new ComputerConfigDialog(this, defaultName);
        dialog.setVisible(true);
        
        if (dialog.isConfirmed()) {
            String id = "computer-" + deviceCounter++;
            String name = dialog.getDeviceName();
            double power = dialog.getPower();
            
            Computer computer = new Computer(id, name, power);
            ComputerController controller = new ComputerController(computer);
            addDevice(controller);
        }
    }

    private void addDevice(ApplianceController controller) {
        ApplianceControlPanel panel = new ApplianceControlPanel(controller);
        
        panel.addPropertyChangeListener("removeDevice", evt -> {
            ApplianceController ctrlToRemove = (ApplianceController) evt.getNewValue();
            removeDevice(panel, ctrlToRemove);
        });

        panel.setAlignmentX(Component.LEFT_ALIGNMENT);
        panel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 140));
        
        devicePanels.add(panel);
        devicesPanel.add(panel);
        devicesPanel.add(Box.createVerticalStrut(10));
        
        powerSystem.addDevice(controller);
        
        devicesPanel.revalidate();
        devicesPanel.repaint();
    }

    private void removeDevice(ApplianceControlPanel panel, ApplianceController controller) {
        panel.cleanup();
        
        int index = getComponentIndex(devicesPanel, panel);
        if (index >= 0) {
            devicesPanel.remove(panel);
            
            if (index < devicesPanel.getComponentCount()) {
                Component nextComponent = devicesPanel.getComponent(index);
                if (nextComponent instanceof Box.Filler) {
                    devicesPanel.remove(nextComponent);
                }
            }
        }
        
        devicePanels.remove(panel);
        powerSystem.removeDevice(controller);
        
        devicesPanel.revalidate();
        devicesPanel.repaint();
    }

    private int getComponentIndex(Container container, Component component) {
        Component[] components = container.getComponents();
        for (int i = 0; i < components.length; i++) {
            if (components[i] == component) {
                return i;
            }
        }
        return -1;
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
            powerSystem.isPowerAvailable() ? "[V] Включено" : "[X] Отключено"
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
            "- Динамическое добавление устройств\n" +
            "- Настраиваемая мощность устройств\n" +
            "- Мониторинг потребления в реальном времени\n" +
            "- Автоматическая защита от перегрузки\n" +
            "- Модели поведения устройств\n" +
            "- ИБП с моделью батареи\n\n" +
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