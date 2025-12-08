package pw.ns2030.window;

import pw.ns2030.controller.ApplianceController;
import pw.ns2030.controller.PowerSystemController;

import javax.swing.*;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

/**
 * Базовый абстрактный класс для окон устройств.
 * Реализует паттерн Template Method для создания окон с общей структурой.
 * Автоматически обновляет данные через таймер.
 */
public abstract class DeviceWindow extends JFrame {
    protected final ApplianceController controller;
    protected final PowerSystemController powerSystem;
    protected Timer updateTimer;
    
    protected JPanel contentPanel;
    protected JLabel statusLabel;
    protected JButton closeButton;
    
    private static int windowCounter = 0;

    public DeviceWindow(ApplianceController controller, PowerSystemController powerSystem) {
        this.controller = controller;
        this.powerSystem = powerSystem;
        
        setupWindow();
        initComponents();
        setupLayout();
        setupListeners();
        startUpdates();
    }

    /**
     * Настройка базовых параметров окна с уникальным номером.
     */
    protected void setupWindow() {
        windowCounter++;
        String deviceName = controller.getAppliance().getName();
        setTitle(deviceName + " - Окно #" + windowCounter);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setLayout(new BorderLayout(10, 10));
        ((JPanel) getContentPane()).setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));
    }

    /**
     * Инициализация компонентов окна. Переопределяется наследниками.
     */
    protected abstract void initComponents();
    
    /**
     * Настройка layout менеджеров. Переопределяется наследниками.
     */
    protected abstract void setupLayout();
    
    /**
     * Обновление данных на UI. Переопределяется наследниками.
     */
    protected abstract void updateData();

    protected void setupListeners() {
        controller.addListener(event -> SwingUtilities.invokeLater(this::updateData));
        
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                stopUpdates();
                System.out.println("[DeviceWindow] Окно " + getTitle() + " закрыто");
            }
        });
    }

    protected void startUpdates() {
        updateTimer = new Timer(500, e -> updateData());
        updateTimer.start();
    }

    protected void stopUpdates() {
        if (updateTimer != null) {
            updateTimer.stop();
            updateTimer = null;
        }
    }

    protected JButton createStyledButton(String text, Color bgColor) {
        JButton button = new JButton(text);
        button.setFont(new Font("Arial", Font.BOLD, 12));
        button.setBackground(bgColor);
        button.setForeground(Color.WHITE);
        button.setFocusPainted(false);
        button.setBorderPainted(false);
        button.setOpaque(true);
        button.setCursor(new Cursor(Cursor.HAND_CURSOR));
        
        return button;
    }

    protected JButton createStyledButtonWithHover(String text, Color bgColor) {
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
            public void mouseEntered(java.awt.event.MouseEvent evt) {
                if (button.isEnabled()) {
                    button.setBackground(hoverColor);
                }
            }
            @Override
            public void mouseExited(java.awt.event.MouseEvent evt) {
                button.setBackground(bgColor);
            }
        });
        
        return button;
    }

    @Override
    public void dispose() {
        stopUpdates();
        super.dispose();
    }
}