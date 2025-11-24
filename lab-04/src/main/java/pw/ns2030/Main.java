package pw.ns2030;

import pw.ns2030.demo.PowerSystemDemo;

import javax.swing.*;

/**
 * Точка входа приложения - запуск системы потребителей энергии.
 */
public class Main {
    public static void main(String[] args) {
        configureLookAndFeel();
        
        SwingUtilities.invokeLater(() -> {
            try {
                PowerSystemDemo demo = new PowerSystemDemo();
                demo.setVisible(true);
                
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
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            
            UIManager.put("Button.font", new java.awt.Font("Arial", java.awt.Font.PLAIN, 12));
            UIManager.put("Label.font", new java.awt.Font("Arial", java.awt.Font.PLAIN, 12));
            UIManager.put("TextField.font", new java.awt.Font("Arial", java.awt.Font.PLAIN, 12));
            
        } catch (Exception e) {
            System.err.println("Не удалось установить системный Look and Feel");
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