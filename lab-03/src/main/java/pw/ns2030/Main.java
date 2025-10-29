package pw.ns2030;

import pw.ns2030.demo.LevelIndicatorDemo;

import javax.swing.*;

/**
 * Точка входа приложения - конфигурация окружения и запуск GUI.
 * Применяет системный Look&Feel для нативного вида на каждой ОС.
 */
public class Main {
    /**
     * Точка входа в приложение.
     * Запускает GUI в Event Dispatch Thread (EDT) согласно Swing best practices.
     * 
     * @param args аргументы командной строки (не используются)
     */
    public static void main(String[] args) {
        configureLookAndFeel();
        
        // Запуск в «Event Dispatch Thread» (EDT) для thread-safety Swing компонентов
        SwingUtilities.invokeLater(() -> {
            try {
                LevelIndicatorDemo demo = new LevelIndicatorDemo();
                demo.setVisible(true);
                
                // Инструкция для пользователя в консоли
                System.out.println("=== Индикатор уровня запущен ===");
                System.out.println("Используйте панель управления для изменения значений");
                System.out.println("Симулятор позволяет автоматически генерировать данные");
                
            } catch (Exception e) {
                System.err.println("Ошибка запуска приложения: " + e.getMessage());
                e.printStackTrace();
                showErrorDialog(e);
            }
        });
    }

    /**
     * Настройка внешнего вида приложения под текущую ОС.
     * Fallback на кросс-платформенный Metal L&F при невозможности применить системный.
     */
    private static void configureLookAndFeel() {
        try {
            // Системный L&F (Windows на Win, Aqua на Mac, GTK на Linux)
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            
            // Кастомизация шрифтов для лучшей читаемости
            UIManager.put("Button.font", new java.awt.Font("Arial", java.awt.Font.PLAIN, 12));
            UIManager.put("Label.font", new java.awt.Font("Arial", java.awt.Font.PLAIN, 12));
            UIManager.put("TextField.font", new java.awt.Font("Arial", java.awt.Font.PLAIN, 12));
            
        } catch (ClassNotFoundException | InstantiationException | 
                 IllegalAccessException | UnsupportedLookAndFeelException e) {
            System.err.println("Не удалось установить системный Look and Feel: " + e.getMessage());
            
            // Fallback на Metal L&F (встроенный Java, работает везде)
            try {
                UIManager.setLookAndFeel(UIManager.getCrossPlatformLookAndFeelClassName());
            } catch (Exception ex) {
                System.err.println("Не удалось установить кросс-платформенный Look and Feel");
            }
        }
    }

    /**
     * Отображение модального диалога с ошибкой запуска.
     * Используется при критических сбоях инициализации.
     */
    private static void showErrorDialog(Exception e) {
        String message = String.format(
                "Произошла ошибка при запуске приложения:\n\n%s\n\nПроверьте консоль для подробностей.",
                e.getMessage()
        );
        
        JOptionPane.showMessageDialog(
                null,
                message,
                "Ошибка запуска",
                JOptionPane.ERROR_MESSAGE
        );
    }
}