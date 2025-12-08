package pw.ns2030.dialog;

import pw.ns2030.controller.ApplianceController;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.util.List;

/**
 * Модальный диалог выбора устройства из списка.
 * Используется для открытия окна конкретного устройства.
 */
public class DeviceSelectionDialog extends JDialog {
    private final List<ApplianceController> devices;
    private ApplianceController selectedDevice = null;
    private JList<String> deviceList;
    private boolean confirmed = false;

    /**
     * Конструктор диалога.
     * 
     * @param parent родительское окно
     * @param title заголовок окна
     * @param devices список устройств для выбора
     */
    public DeviceSelectionDialog(Frame parent, String title, List<ApplianceController> devices) {
        super(parent, title, true);  // modal dialog
        this.devices = devices;
        
        initComponents();
        setupLayout();
        setupListeners();
        
        pack();
        setLocationRelativeTo(parent);
        setResizable(false);
    }

    private void initComponents() {
        // Создаем массив имен устройств для отображения
        String[] deviceNames = devices.stream()
            .map(controller -> {
                String name = controller.getAppliance().getName();
                String state = controller.getAppliance().getState().getDisplayName();
                double power = controller.getAppliance().getCurrentPower();
                return String.format("%s [%s, %.0f Вт]", name, state, power);
            })
            .toArray(String[]::new);
        
        deviceList = new JList<>(deviceNames);
        deviceList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        deviceList.setVisibleRowCount(Math.min(10, deviceNames.length));
        deviceList.setFont(new Font("Arial", Font.PLAIN, 12));
        deviceList.setBorder(new EmptyBorder(5, 5, 5, 5));
        
        // Автоматически выбираем первое устройство
        if (deviceNames.length > 0) {
            deviceList.setSelectedIndex(0);
        }
        
        // Двойной клик = подтверждение
        deviceList.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseClicked(java.awt.event.MouseEvent e) {
                if (e.getClickCount() == 2 && deviceList.getSelectedIndex() != -1) {
                    onOk();
                }
            }
        });
    }

    private void setupLayout() {
        setLayout(new BorderLayout(10, 10));
        ((JPanel) getContentPane()).setBorder(new EmptyBorder(15, 15, 15, 15));
        
        // Заголовок с инструкцией
        JLabel instructionLabel = new JLabel("Выберите устройство для открытия:");
        instructionLabel.setFont(new Font("Arial", Font.BOLD, 13));
        add(instructionLabel, BorderLayout.NORTH);
        
        // Список устройств в скролл-панели
        JScrollPane scrollPane = new JScrollPane(deviceList);
        scrollPane.setPreferredSize(new Dimension(400, Math.min(250, devices.size() * 25 + 20)));
        add(scrollPane, BorderLayout.CENTER);
        
        // Панель кнопок
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 5));
        
        JButton okButton = createStyledButton("Открыть", new Color(76, 175, 80));
        okButton.setPreferredSize(new Dimension(100, 32));
        
        JButton cancelButton = createStyledButton("Отмена", new Color(158, 158, 158));
        cancelButton.setPreferredSize(new Dimension(100, 32));
        
        buttonPanel.add(okButton);
        buttonPanel.add(cancelButton);
        
        add(buttonPanel, BorderLayout.SOUTH);
    }

    private void setupListeners() {
        // Enter на списке = подтверждение
        deviceList.addKeyListener(new java.awt.event.KeyAdapter() {
            @Override
            public void keyPressed(java.awt.event.KeyEvent e) {
                if (e.getKeyCode() == java.awt.event.KeyEvent.VK_ENTER) {
                    onOk();
                }
            }
        });
    }

    private JButton createStyledButton(String text, Color bgColor) {
        JButton button = new JButton(text);
        button.setFont(new Font("Arial", Font.BOLD, 12));
        button.setBackground(bgColor);
        button.setForeground(Color.WHITE);
        button.setFocusPainted(false);
        button.setBorderPainted(false);
        button.setOpaque(true);
        button.setCursor(new Cursor(Cursor.HAND_CURSOR));
        
        Color hoverColor = bgColor.darker();
        button.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseEntered(java.awt.event.MouseEvent e) {
                if (button.isEnabled()) {
                    button.setBackground(hoverColor);
                }
            }
            @Override
            public void mouseExited(java.awt.event.MouseEvent e) {
                button.setBackground(bgColor);
            }
        });
        
        button.addActionListener(e -> {
            if (text.equals("Открыть")) {
                onOk();
            } else {
                onCancel();
            }
        });
        
        return button;
    }

    private void onOk() {
        int selectedIndex = deviceList.getSelectedIndex();
        
        if (selectedIndex == -1) {
            JOptionPane.showMessageDialog(
                this,
                "Пожалуйста, выберите устройство из списка!",
                "Предупреждение",
                JOptionPane.WARNING_MESSAGE
            );
            return;
        }
        
        selectedDevice = devices.get(selectedIndex);
        confirmed = true;
        dispose();
    }

    private void onCancel() {
        confirmed = false;
        selectedDevice = null;
        dispose();
    }

    /**
     * Проверка подтверждения выбора.
     */
    public boolean isConfirmed() {
        return confirmed;
    }

    /**
     * Получение выбранного устройства.
     */
    public ApplianceController getSelectedDevice() {
        return selectedDevice;
    }
}