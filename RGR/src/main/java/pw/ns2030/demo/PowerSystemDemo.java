package pw.ns2030.demo;

import pw.ns2030.component.ApplianceControlPanel;
import pw.ns2030.component.PowerMeterPanel;
import pw.ns2030.controller.*;
import pw.ns2030.model.*;
import pw.ns2030.dialog.*;
import pw.ns2030.task.*;
import pw.ns2030.window.*;

import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Главное окно приложения системы потребителей электроэнергии.
 * Реализует многооконный интерфейс с фоновыми операциями.
 * Управляет устройствами, мониторингом и выполнением задач.
 */
public class PowerSystemDemo extends JFrame {
    private PowerSystemController powerSystem;
    private PowerMeterPanel powerMeterPanel;
    private JPanel devicesPanel;
    private List<ApplianceControlPanel> devicePanels;
    
    private int deviceCounter = 1;

    public PowerSystemDemo() {
        setTitle("Система потребителей электроэнергии - РГР (Многооконный интерфейс)");
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

    /**
     * Настройка главного меню с разделами для управления устройствами,
     * окнами, операциями и системой.
     */
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
        
        // Меню "Окна" с диалогом выбора
        JMenu windowsMenu = new JMenu("Окна");
        JMenuItem openKettleWindowItem = new JMenuItem("Открыть окно чайника...");
        openKettleWindowItem.addActionListener(e -> openKettleWindowWithSelection());
        JMenuItem openLampWindowItem = new JMenuItem("Открыть окно лампы...");
        openLampWindowItem.addActionListener(e -> openLampWindowWithSelection());
        JMenuItem openComputerWindowItem = new JMenuItem("Открыть окно компьютера...");
        openComputerWindowItem.addActionListener(e -> openComputerWindowWithSelection());
        
        windowsMenu.add(openKettleWindowItem);
        windowsMenu.add(openLampWindowItem);
        windowsMenu.add(openComputerWindowItem);
        
        // Новое меню: "Операции" (фоновые задачи)
        JMenu operationsMenu = new JMenu("Операции");
        
        JMenuItem exportItem = new JMenuItem("Экспорт данных в CSV...");
        exportItem.addActionListener(e -> executeExportTask());
        
        JMenuItem importItem = new JMenuItem("Импорт данных из CSV...");
        importItem.addActionListener(e -> executeImportTask());
        
        JMenuItem analysisItem = new JMenuItem("Анализ системы...");
        analysisItem.addActionListener(e -> executeAnalysisTask());
        
        JMenuItem loadTestItem = new JMenuItem("Нагрузочный тест...");
        loadTestItem.addActionListener(e -> executeLoadTestTask());
        
        operationsMenu.add(exportItem);
        operationsMenu.add(importItem);
        operationsMenu.addSeparator();
        operationsMenu.add(analysisItem);
        operationsMenu.add(loadTestItem);
        
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
        menuBar.add(windowsMenu);
        menuBar.add(operationsMenu);
        menuBar.add(systemMenu);
        menuBar.add(helpMenu);
        
        setJMenuBar(menuBar);
    }

    // ========== МЕТОДЫ ФОНОВЫХ ОПЕРАЦИЙ ==========

    /**
     * Экспорт данных системы в CSV файл с прогресс-баром.
     */
    private void executeExportTask() {
        if (devicePanels.isEmpty()) {
            JOptionPane.showMessageDialog(
                this,
                "Нет устройств для экспорта!\nДобавьте хотя бы одно устройство.",
                "Предупреждение",
                JOptionPane.WARNING_MESSAGE
            );
            return;
        }
        
        // Диалог выбора файла
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("Сохранить данные как CSV");
        fileChooser.setFileFilter(new FileNameExtensionFilter("CSV файлы (*.csv)", "csv"));
        fileChooser.setSelectedFile(new File("system_export.csv"));
        
        if (fileChooser.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
            File file = fileChooser.getSelectedFile();
            
            // Добавляем расширение .csv если отсутствует
            if (!file.getName().toLowerCase().endsWith(".csv")) {
                file = new File(file.getAbsolutePath() + ".csv");
            }
            
            // Опция включения истории
            int includeHistory = JOptionPane.showConfirmDialog(
                this,
                "Включить историю потребления в экспорт?\n\n" +
                "(Увеличит размер файла и время экспорта)",
                "Параметры экспорта",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.QUESTION_MESSAGE
            );
            
            // Собираем контроллеры
            List<ApplianceController> controllers = devicePanels.stream()
                .map(ApplianceControlPanel::getController)
                .collect(Collectors.toList());
            
            // Создаем задачу
            ExportDataTask task = new ExportDataTask(
                controllers,
                file,
                includeHistory == JOptionPane.YES_OPTION
            );
            
            // Показываем диалог прогресса
            ProgressDialog dialog = new ProgressDialog(this, "Экспорт данных");
            dialog.executeTask(task);
        }
    }
    
    /**
     * Импорт данных с callback для добавления в GUI.
     */
    private void executeImportTask() {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("Выберите CSV файл для импорта");
        fileChooser.setFileFilter(new FileNameExtensionFilter("CSV файлы (*.csv)", "csv"));
        
        if (fileChooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            File file = fileChooser.getSelectedFile();
            
            // Предлагаем выбор режима импорта
            String[] options = {"Восстановить устройства", "Только просмотр", "Отмена"};
            int choice = JOptionPane.showOptionDialog(
                this,
                "Импорт данных из файла:\n" + file.getName() + "\n\n" +
                "Выберите режим импорта:\n\n" +
                "• Восстановить устройства - создаст устройства в системе\n" +
                "• Только просмотр - покажет содержимое без изменений",
                "Режим импорта",
                JOptionPane.YES_NO_CANCEL_OPTION,
                JOptionPane.QUESTION_MESSAGE,
                null,
                options,
                options[0]
            );
            
            if (choice == JOptionPane.CANCEL_OPTION || choice == JOptionPane.CLOSED_OPTION) {
                return;  // Отмена
            }
            
            boolean restoreMode = (choice == 0);  // 0 = "Восстановить устройства"
            
            ImportDataTask task;
            
            if (restoreMode) {
                // Передаем callback для добавления в GUI
                ImportDataTask.DeviceAddCallback callback = controller -> {
                    // Этот код выполнится в EDT
                    addDevice(controller);
                };
                
                task = new ImportDataTask(file, powerSystem, callback);
            } else {
                // Режим "только просмотр" - без callback
                task = new ImportDataTask(file);
            }
            
            ProgressDialog dialog = new ProgressDialog(this, "Импорт данных");
            dialog.executeTask(task);
        }
    }

    /**
     * Анализ системы с генерацией подробного отчета.
     */
    private void executeAnalysisTask() {
        if (devicePanels.isEmpty()) {
            JOptionPane.showMessageDialog(
                this,
                "Нет устройств для анализа!\nДобавьте хотя бы одно устройство.",
                "Предупреждение",
                JOptionPane.WARNING_MESSAGE
            );
            return;
        }
        
        // Создаем задачу
        SystemAnalysisTask task = new SystemAnalysisTask(powerSystem);
        
        // Показываем диалог прогресса
        ProgressDialog dialog = new ProgressDialog(this, "Анализ системы");
        dialog.executeTask(task);
    }

    /**
     * Нагрузочное тестирование системы.
     */
    private void executeLoadTestTask() {
        // Диалог параметров теста
        JPanel panel = new JPanel(new GridLayout(2, 2, 10, 10));
        panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        
        JLabel iterationsLabel = new JLabel("Количество измерений:");
        JTextField iterationsField = new JTextField("50", 10);
        
        JLabel intervalLabel = new JLabel("Интервал (мс):");
        JTextField intervalField = new JTextField("100", 10);
        
        panel.add(iterationsLabel);
        panel.add(iterationsField);
        panel.add(intervalLabel);
        panel.add(intervalField);
        
        int result = JOptionPane.showConfirmDialog(
            this,
            panel,
            "Параметры нагрузочного теста",
            JOptionPane.OK_CANCEL_OPTION,
            JOptionPane.QUESTION_MESSAGE
        );
        
        if (result == JOptionPane.OK_OPTION) {
            try {
                int iterations = Integer.parseInt(iterationsField.getText().trim());
                int interval = Integer.parseInt(intervalField.getText().trim());
                
                // Валидация
                if (iterations < 10 || iterations > 1000) {
                    JOptionPane.showMessageDialog(
                        this,
                        "Количество измерений должно быть от 10 до 1000!",
                        "Ошибка",
                        JOptionPane.ERROR_MESSAGE
                    );
                    return;
                }
                
                if (interval < 50 || interval > 5000) {
                    JOptionPane.showMessageDialog(
                        this,
                        "Интервал должен быть от 50 до 5000 мс!",
                        "Ошибка",
                        JOptionPane.ERROR_MESSAGE
                    );
                    return;
                }
                
                // Создаем задачу
                LoadTestTask task = new LoadTestTask(powerSystem, iterations, interval);
                
                // Показываем диалог прогресса
                ProgressDialog dialog = new ProgressDialog(this, "Нагрузочный тест");
                dialog.executeTask(task);
                
            } catch (NumberFormatException ex) {
                JOptionPane.showMessageDialog(
                    this,
                    "Некорректные числовые значения!\nВведите целые числа.",
                    "Ошибка",
                    JOptionPane.ERROR_MESSAGE
                );
            }
        }
    }

    // ========== МЕТОДЫ ОТКРЫТИЯ ОКОН С ДИАЛОГОМ ВЫБОРА ==========

    /**
     * Открывает диалог выбора чайника, затем его окно.
     */
    private void openKettleWindowWithSelection() {
        List<KettleController> kettles = findAllControllers(KettleController.class);
        
        if (kettles.isEmpty()) {
            int result = JOptionPane.showConfirmDialog(
                this,
                "Нет ни одного чайника в системе.\nДобавить чайник сейчас?",
                "Чайники не найдены",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.QUESTION_MESSAGE
            );
            
            if (result == JOptionPane.YES_OPTION) {
                addKettle();
                kettles = findAllControllers(KettleController.class);
                if (kettles.isEmpty()) {
                    return;
                }
            } else {
                return;
            }
        }
        
        if (kettles.size() == 1) {
            openDeviceWindow(kettles.get(0));
            return;
        }
        
        List<ApplianceController> controllersForDialog = new ArrayList<>(kettles);
        
        DeviceSelectionDialog dialog = new DeviceSelectionDialog(
            this,
            "Выбор чайника",
            controllersForDialog
        );
        dialog.setVisible(true);
        
        if (dialog.isConfirmed()) {
            ApplianceController selected = dialog.getSelectedDevice();
            openDeviceWindow(selected);
        }
    }

    /**
     * Открывает диалог выбора лампы, затем её окно.
     */
    private void openLampWindowWithSelection() {
        List<LampController> lamps = findAllControllers(LampController.class);
        
        if (lamps.isEmpty()) {
            int result = JOptionPane.showConfirmDialog(
                this,
                "Нет ни одной лампы в системе.\nДобавить лампу сейчас?",
                "Лампы не найдены",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.QUESTION_MESSAGE
            );
            
            if (result == JOptionPane.YES_OPTION) {
                addLamp();
                lamps = findAllControllers(LampController.class);
                if (lamps.isEmpty()) {
                    return;
                }
            } else {
                return;
            }
        }
        
        if (lamps.size() == 1) {
            openDeviceWindow(lamps.get(0));
            return;
        }
        
        List<ApplianceController> controllersForDialog = new ArrayList<>(lamps);
        
        DeviceSelectionDialog dialog = new DeviceSelectionDialog(
            this,
            "Выбор лампы",
            controllersForDialog
        );
        dialog.setVisible(true);
        
        if (dialog.isConfirmed()) {
            ApplianceController selected = dialog.getSelectedDevice();
            openDeviceWindow(selected);
        }
    }

    /**
     * Открывает диалог выбора компьютера, затем его окно.
     */
    private void openComputerWindowWithSelection() {
        List<ComputerController> computers = findAllControllers(ComputerController.class);
        
        if (computers.isEmpty()) {
            int result = JOptionPane.showConfirmDialog(
                this,
                "Нет ни одного компьютера в системе.\nДобавить компьютер сейчас?",
                "Компьютеры не найдены",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.QUESTION_MESSAGE
            );
            
            if (result == JOptionPane.YES_OPTION) {
                addComputer();
                computers = findAllControllers(ComputerController.class);
                if (computers.isEmpty()) {
                    return;
                }
            } else {
                return;
            }
        }
        
        if (computers.size() == 1) {
            openDeviceWindow(computers.get(0));
            return;
        }
        
        List<ApplianceController> controllersForDialog = new ArrayList<>(computers);
        
        DeviceSelectionDialog dialog = new DeviceSelectionDialog(
            this,
            "Выбор компьютера",
            controllersForDialog
        );
        dialog.setVisible(true);
        
        if (dialog.isConfirmed()) {
            ApplianceController selected = dialog.getSelectedDevice();
            openDeviceWindow(selected);
        }
    }

    /**
     * Универсальный метод открытия окна устройства.
     */
    private void openDeviceWindow(ApplianceController controller) {
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
                System.out.println("[PowerSystemDemo] Открыто окно: " + window.getTitle());
            }
            
        } catch (Exception ex) {
            System.err.println("[PowerSystemDemo] Ошибка открытия окна: " + ex.getMessage());
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
     * Поиск ВСЕХ контроллеров заданного типа.
     */
    @SuppressWarnings("unchecked")
    private <T extends ApplianceController> List<T> findAllControllers(Class<T> controllerClass) {
        return devicePanels.stream()
            .map(ApplianceControlPanel::getController)
            .filter(controllerClass::isInstance)
            .map(controller -> (T) controller)
            .collect(Collectors.toList());
    }

    // ========== УПРАВЛЕНИЕ УСТРОЙСТВАМИ ==========

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

    private void addKettle() {
        String defaultName = "Чайник #" + deviceCounter;
        KettleConfigDialog dialog = new KettleConfigDialog(this, defaultName);
        dialog.setVisible(true);
        
        if (dialog.isConfirmed()) {
            String name = dialog.getDeviceName();
            double power = dialog.getPower();
            
            Kettle kettle = new Kettle("kettle-" + deviceCounter, name, power);
            KettleController controller = new KettleController(kettle);
            
            addDevice(controller);
            deviceCounter++;
        }
    }

    private void addLamp() {
        String defaultName = "Лампа #" + deviceCounter;
        LampConfigDialog dialog = new LampConfigDialog(this, defaultName);
        dialog.setVisible(true);
        
        if (dialog.isConfirmed()) {
            String name = dialog.getDeviceName();
            double power = dialog.getPower();
            
            Lamp lamp = new Lamp("lamp-" + deviceCounter, name, power);
            LampController controller = new LampController(lamp);
            
            addDevice(controller);
            deviceCounter++;
        }
    }

    private void addComputer() {
        String defaultName = "Компьютер #" + deviceCounter;
        ComputerConfigDialog dialog = new ComputerConfigDialog(this, defaultName);
        dialog.setVisible(true);
        
        if (dialog.isConfirmed()) {
            String name = dialog.getDeviceName();
            double power = dialog.getPower();
            
            Computer computer = new Computer("computer-" + deviceCounter, name, power);
            ComputerController controller = new ComputerController(computer);
            
            addDevice(controller);
            deviceCounter++;
        }
    }

    private void addDevice(ApplianceController controller) {
        ApplianceControlPanel panel = new ApplianceControlPanel(controller);
        
        panel.setPowerSystemController(powerSystem);
        
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
            devicesPanel.remove(index);
            if (index < devicesPanel.getComponentCount() && 
                devicesPanel.getComponent(index) instanceof Box.Filler) {
                devicesPanel.remove(index);
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
            "Удалить ВСЕ устройства из системы?",
            "Подтверждение",
            JOptionPane.YES_NO_OPTION,
            JOptionPane.WARNING_MESSAGE
        );
        
        if (result == JOptionPane.YES_OPTION) {
            List<ApplianceControlPanel> panelsCopy = new ArrayList<>(devicePanels);
            
            for (ApplianceControlPanel panel : panelsCopy) {
                removeDevice(panel, panel.getController());
            }
            
            JOptionPane.showMessageDialog(
                this,
                "Все устройства удалены!",
                "Информация",
                JOptionPane.INFORMATION_MESSAGE
            );
        }
    }

    // ========== ИНФОРМАЦИОННЫЕ ДИАЛОГИ ==========

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
            powerSystem.isPowerAvailable() ? "[OK] Включено" : "[X] Отключено"
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
                "Расчетно-графическая работа\n" +
                "Задание: Многооконный интерфейс (20 баллов)\n" +
                "Доп. задание: Потоковое программирование (10 баллов)\n\n" +
                "Студент: Шаламов А.Е.\n" +
                "Группа: АВТ-343\n\n" +
                "Реализованные возможности:\n" +
                "[OK] Многооконный GUI (отдельные окна для устройств)\n" +
                "[OK] Уникальные датчики в каждом окне\n" +
                "[OK] Неограниченное количество экземпляров окон\n" +
                "[OK] Передача ссылки на PowerSystemController\n" +
                "[OK] Динамическое добавление устройств\n" +
                "[OK] Диалог выбора устройства для открытия окна\n" +
                "[OK] Двойной клик на панели устройства\n" +
                "[OK] Мониторинг в реальном времени\n" +
                "[OK] Автоматическая защита от перегрузки\n\n" +
                "Дополнительные возможности (+10 баллов):\n" +
                "[OK] Фоновые операции через SwingWorker\n" +
                "[OK] Индикатор прогресса с процентами\n" +
                "[OK] Периодический вывод параметров задачи\n" +
                "[OK] Аварийное завершение операции\n" +
                "[OK] Синхронизация окончания операции\n" +
                "[OK] Экспорт/импорт данных в CSV\n" +
                "[OK] Анализ системы с отчетом\n" +
                "[OK] Нагрузочное тестирование\n\n" +
                "Паттерны проектирования:\n" +
                "- Template Method (DeviceWindow, BackgroundTask)\n" +
                "- Observer (события устройств)\n" +
                "- MVC (разделение логики/UI)\n" +
                "- Builder (конфигурация индикаторов)\n\n" +
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
            "Завершить работу приложения?",
            "Подтверждение выхода",
            JOptionPane.YES_NO_OPTION
        );
        
        if (result == JOptionPane.YES_OPTION) {
            System.out.println("[PowerSystemDemo] Завершение работы...");
            
            powerMeterPanel.cleanup();
            
            for (ApplianceControlPanel panel : devicePanels) {
                panel.cleanup();
            }
            
            powerSystem.shutdown();
            
            dispose();
            System.exit(0);
        }
    }
}