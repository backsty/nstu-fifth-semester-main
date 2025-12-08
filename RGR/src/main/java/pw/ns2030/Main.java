package pw.ns2030;

import pw.ns2030.demo.PowerSystemDemo;

import com.formdev.flatlaf.FlatIntelliJLaf;

import javax.swing.*;
import java.awt.*;

public class Main {
    public static void main(String[] args) {
        configureLookAndFeel();
        
        SwingUtilities.invokeLater(() -> {
            try {
                PowerSystemDemo demo = new PowerSystemDemo();
                demo.setVisible(true);

                // Откладываем на 100ms чтобы окно успело отрисоваться
                Timer initialDevicesTimer = new Timer(100, e -> {
                    demo.addInitialDevices(); // Вызываем публичный метод
                    ((Timer) e.getSource()).stop();
                });
                initialDevicesTimer.setRepeats(false);
                initialDevicesTimer.start();
                
                System.out.println("=== Система потребителей энергии запущена ===");
                System.out.println("Добавляйте устройства и наблюдайте за потреблением!");
                
            } catch (Exception e) {
                System.err.println("Ошибка запуска: " + e.getMessage());
                e.printStackTrace();
                showErrorDialog(e);
            }
        });
    }

    private static void configureLookAndFeel() {
        try {
            // Установка FlatLaf IntelliJ тема (современный светлый интерфейс)
            FlatIntelliJLaf.setup();
            
            // Скругление углов кнопок и компонентов
            UIManager.put("Button.arc", 8);
            UIManager.put("Component.arc", 8);
            UIManager.put("TextComponent.arc", 8);
            UIManager.put("ScrollBar.width", 12);
            UIManager.put("TabbedPane.tabHeight", 32);
            
            // Современные шрифты
            Font segoeUI = new Font("Segoe UI", Font.PLAIN, 12);
            Font segoeUIBold = new Font("Segoe UI", Font.BOLD, 12);
            
            UIManager.put("Button.font", segoeUI);
            UIManager.put("Label.font", segoeUI);
            UIManager.put("TextField.font", segoeUI);
            UIManager.put("Menu.font", segoeUI);
            UIManager.put("MenuItem.font", segoeUI);
            UIManager.put("TitledBorder.font", segoeUIBold);
            
            // Отступы в меню
            UIManager.put("MenuItem.margin", new Insets(4, 6, 4, 6));
            UIManager.put("Menu.margin", new Insets(2, 6, 2, 6));
            
            // Плавная прокрутка
            UIManager.put("ScrollPane.smoothScrolling", true);
            
            // Цвет акцентов (синий)
            UIManager.put("Component.accentColor", new Color(33, 150, 243));
            
        } catch (Exception e) {
            System.err.println("Не удалось установить FlatLaf Look and Feel");
            e.printStackTrace();
            
            // Запасной вариант - системная тема
            try {
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }
    }

    private static void showErrorDialog(Exception e) {
        String message = String.format(
            "Ошибка запуска:\n\n%s\n\nПроверьте консоль для деталей.",
            e.getMessage()
        );
        
        JOptionPane.showMessageDialog(
            null,
            message,
            "Ошибка",
            JOptionPane.ERROR_MESSAGE
        );
    }
}