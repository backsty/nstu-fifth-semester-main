package pw.ns2030;

import com.formdev.flatlaf.FlatLightLaf;
import com.formdev.flatlaf.extras.FlatSVGIcon;
import pw.ns2030.ui.MainFrame;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * Главный класс приложения "Турнирная таблица".
 * Настраивает Look&Feel, загружает конфигурацию и запускает главное окно.
 */
public class Main {
    private static final String DEFAULT_APP_NAME = "Турнирная таблица";
    private static Properties config;

    public static void main(String[] args) {
        setupSystemProperties();
        loadConfiguration();
        setupLookAndFeel();
        
        // Запускаем GUI в Event Dispatch Thread
        SwingUtilities.invokeLater(() -> {
            try {
                createAndShowMainFrame();
            } catch (Exception e) {
                showFatalError("Critical error during application startup", e);
            }
        });
    }
    
    // Настраивает системные свойства для оптимальной работы приложения.
    private static void setupSystemProperties() {
        System.setProperty("file.encoding", "UTF-8");
        System.setProperty("console.encoding", "UTF-8");
        
        // Включаем аппаратное ускорение
        System.setProperty("sun.java2d.opengl", "true");
        
        // Улучшаем рендеринг текста с правильной кодировкой
        System.setProperty("awt.useSystemAAFontSettings", "on");
        System.setProperty("swing.aatext", "true");
        
        // Устанавливаем UTF-8 для Swing
        System.setProperty("swing.defaultlaf.encoding", "UTF-8");
        
        // Включаем сглаживание
        System.setProperty("swing.plaf.metal.controlFont", "Arial-12");
        System.setProperty("swing.plaf.metal.userFont", "Arial-12");
        
        // Настройки для Windows
        if (System.getProperty("os.name").toLowerCase().contains("windows")) {
            System.setProperty("sun.java2d.dpiaware", "true");
        }
        
        // Настройки JVM для GUI
        System.setProperty("java.awt.headless", "false");
    }
    
    // Загружает конфигурацию приложения из файла config.properties.
    private static void loadConfiguration() {
        config = new Properties();
        
        try (InputStream inputStream = Main.class.getClassLoader()
                .getResourceAsStream("config.properties")) {
            
            if (inputStream != null) {
                config.load(new java.io.InputStreamReader(inputStream, java.nio.charset.StandardCharsets.UTF_8));
                System.out.println("Configuration loaded successfully");
            } else {
                System.err.println("config.properties not found, using defaults");
                loadDefaultConfiguration();
            }
            
        } catch (IOException e) {
            System.err.println("Configuration loading error: " + e.getMessage());
            loadDefaultConfiguration();
        }
    }
    
    // Загружает конфигурацию по умолчанию в случае ошибки чтения файла.
    private static void loadDefaultConfiguration() {
        config.setProperty("app.name", DEFAULT_APP_NAME);
        config.setProperty("app.version", "1.0.0");
        config.setProperty("ui.default.width", "1400");
        config.setProperty("ui.default.height", "900");
        config.setProperty("ui.min.width", "1200");
        config.setProperty("ui.min.height", "800");
        config.setProperty("tournament.default.teams", "6");
        config.setProperty("ui.theme", "FlatLaf Light");
        config.setProperty("ui.font.family", "Segoe UI");
        config.setProperty("ui.font.size", "12");
    }
    
    // Настраивает тему оформления FlatLaf
    private static void setupLookAndFeel() {
        try {
            // Устанавливаем FlatLaf Light тему
            UIManager.setLookAndFeel(new FlatLightLaf());
            
            // Настраиваем дополнительные свойства темы
            setupFlatLafProperties();
            
            System.out.println("FlatLaf theme installed successfully");
            
        } catch (UnsupportedLookAndFeelException e) {
            System.err.println("Failed to install FlatLaf theme: " + e.getMessage());
            
            try {
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
                System.out.println("System theme installed");
            } catch (Exception fallbackException) {
                System.err.println("Failed to install system theme: " + fallbackException.getMessage());
            }
        }
        setupFonts();
    }
    
    // Настраивает дополнительные свойства FlatLaf темы
    private static void setupFlatLafProperties() {
        // Настройки цветов
        UIManager.put("Component.focusColor", new Color(0, 120, 215, 50));
        UIManager.put("Component.borderColor", new Color(200, 200, 200));
        
        // Настройки таблиц
        UIManager.put("Table.selectionBackground", new Color(0, 120, 215, 50));
        UIManager.put("Table.selectionForeground", Color.BLACK);
        
        // Настройки текстовых полей
        UIManager.put("TextField.focusedBorderColor", new Color(0, 120, 215));
        UIManager.put("TextField.hoverBorderColor", new Color(0, 120, 215, 100));
        
        // Настройки кнопок
        UIManager.put("Button.hoverBorderColor", new Color(0, 120, 215));
        UIManager.put("Button.focusedBorderColor", new Color(0, 120, 215));
        
        // Настройки меню
        UIManager.put("PopupMenu.borderColor", new Color(200, 200, 200));
        UIManager.put("MenuItem.selectionBackground", new Color(0, 120, 215, 50));
    }
    
    // Настраивает шрифты для всего приложения.
    private static void setupFonts() {
        String fontFamily = config.getProperty("ui.font.family", "Segoe UI");
        int fontSize = Integer.parseInt(config.getProperty("ui.font.size", "12"));
        
        Font defaultFont = new Font(fontFamily, Font.PLAIN, fontSize);
        Font boldFont = new Font(fontFamily, Font.BOLD, fontSize);

        UIManager.put("Label.font", defaultFont);
        UIManager.put("Button.font", defaultFont);
        UIManager.put("TextField.font", defaultFont);
        UIManager.put("Table.font", defaultFont);
        UIManager.put("TableHeader.font", boldFont);
        UIManager.put("Menu.font", defaultFont);
        UIManager.put("MenuItem.font", defaultFont);
        UIManager.put("PopupMenu.font", defaultFont);
        UIManager.put("ToolTip.font", defaultFont);
    }
    
    // Создает и отображает главное окно приложения.
    private static void createAndShowMainFrame() {
        int width = Integer.parseInt(config.getProperty("ui.default.width", "1400"));
        int height = Integer.parseInt(config.getProperty("ui.default.height", "900"));
        int minWidth = Integer.parseInt(config.getProperty("ui.min.width", "1200"));
        int minHeight = Integer.parseInt(config.getProperty("ui.min.height", "800"));

        MainFrame mainFrame = new MainFrame(config);

        setupMainFrame(mainFrame, width, height, minWidth, minHeight);

        setApplicationIcon(mainFrame);

        mainFrame.setLocationRelativeTo(null);

        mainFrame.setVisible(true);
        
        System.out.println("Main window created and displayed successfully");
    }
    
    // Настраивает основные параметры главного окна.
    private static void setupMainFrame(MainFrame mainFrame, int width, int height, 
                                     int minWidth, int minHeight) {
        String appName = config.getProperty("app.name", DEFAULT_APP_NAME);
        String version = config.getProperty("app.version", "1.0");
        mainFrame.setTitle(appName + " v" + version);
        
        mainFrame.setSize(width, height);
        mainFrame.setMinimumSize(new Dimension(minWidth, minHeight));
        mainFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        mainFrame.setExtendedState(JFrame.NORMAL);
        mainFrame.setResizable(true);
    }
    
    // Устанавливает иконку приложения из SVG файла.
    private static void setApplicationIcon(JFrame frame) {
        try {
            java.util.List<Image> iconImages = java.util.Arrays.asList(
                createIconImage("icons/app-icon.svg", 16),
                createIconImage("icons/app-icon.svg", 24),
                createIconImage("icons/app-icon.svg", 32),
                createIconImage("icons/app-icon.svg", 48),
                createIconImage("icons/app-icon.svg", 64)
            );
            
            frame.setIconImages(iconImages);
            System.out.println("Application icon set successfully");
            
        } catch (Exception e) {
            System.err.println("Failed to load application icon: " + e.getMessage());

            try {
                BufferedImage defaultIcon = createDefaultIcon();
                frame.setIconImage(defaultIcon);
            } catch (Exception fallbackException) {
                System.err.println("Failed to create default icon: " + fallbackException.getMessage());
            }
        }
    }
    
    // Создает изображение иконки указанного размера из SVG файла.
    private static Image createIconImage(String iconPath, int size) {
        try {
            FlatSVGIcon svgIcon = new FlatSVGIcon(iconPath, size, size);
            return svgIcon.getImage();
        } catch (Exception e) {
            System.err.println("Error creating icon of size " + size + ": " + e.getMessage());
            return createDefaultIcon(size);
        }
    }
    
    // Создает простую иконку по умолчанию программно.
    private static BufferedImage createDefaultIcon() {
        return createDefaultIcon(32);
    }
    
    // Создает простую иконку по умолчанию указанного размера.
    private static BufferedImage createDefaultIcon(int size) {
        BufferedImage icon = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2d = icon.createGraphics();
        
        // Включаем сглаживание
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        
        // Рисуем фон
        g2d.setColor(new Color(0, 120, 215));
        g2d.fillRoundRect(2, 2, size - 4, size - 4, 8, 8);
        
        // Рисуем символ таблицы
        g2d.setColor(Color.WHITE);
        g2d.setStroke(new BasicStroke(size / 16f));
        
        int margin = size / 4;
        int cellSize = (size - 2 * margin) / 3;
        
        // Рисуем сетку таблицы
        for (int i = 0; i <= 3; i++) {
            int x = margin + i * cellSize;
            int y = margin + i * cellSize;
            g2d.drawLine(x, margin, x, size - margin);
            g2d.drawLine(margin, y, size - margin, y);
        }
        
        g2d.dispose();
        return icon;
    }
    
    // Отображает критическую ошибку пользователю.
    private static void showFatalError(String message, Exception e) {
        System.err.println("CRITICAL ERROR: " + message);
        e.printStackTrace();
        
        try {
            String errorMessage = message + "\n\nError details:\n" + e.getMessage();
            
            JOptionPane.showMessageDialog(
                null,
                errorMessage,
                "Critical Error",
                JOptionPane.ERROR_MESSAGE
            );
        } catch (Exception dialogException) {
            System.err.println("Failed to show error dialog: " + dialogException.getMessage());
        }
        
        System.exit(1);
    }
    
    // Возвращает загруженную конфигурацию приложения.
    public static Properties getConfiguration() {
        return config != null ? config : new Properties();
    }
    
    // Возвращает значение конфигурации по ключу с fallback значением.
    public static String getConfigValue(String key, String defaultValue) {
        return config != null ? config.getProperty(key, defaultValue) : defaultValue;
    }
    
    /**
     * Создает SVG иконку указанного размера.
     * Используется UI компонентами для загрузки иконок.
     */
    public static FlatSVGIcon createIcon(String iconName, int size) {
        try {
            String iconPath = "icons/" + iconName + ".svg";
            
            // Проверяем, что ресурс существует
            if (Main.class.getClassLoader().getResource(iconPath) == null) {
                System.err.println("Icon file not found: " + iconPath);
                return createFallbackSVGIcon(iconName, size);
            }
            
            FlatSVGIcon icon = new FlatSVGIcon(iconPath, size, size);
            
            // Принудительно устанавливаем цвет
            icon.setColorFilter(new FlatSVGIcon.ColorFilter(color -> {
                // Заменяем черный цвет на темно-серый
                if (color.equals(Color.BLACK) || color.getRGB() == 0xFF000000) {
                    return new Color(64, 64, 64); // Темно-серый
                }
                return color;
            }));
            
            return icon;
            
        } catch (Exception e) {
            System.err.println("Error loading icon " + iconName + ": " + e.getMessage());
            return createFallbackSVGIcon(iconName, size);
        }
    }
    
    // Создает резервную SVG иконку, если основная не загружается.
    private static FlatSVGIcon createFallbackSVGIcon(String iconName, int size) {
        try {
            // Создаем простую SVG иконку программно
            String svgContent = createFallbackSVGContent(iconName, size);
            
            // Создаем временный InputStream
            java.io.ByteArrayInputStream inputStream = 
                new java.io.ByteArrayInputStream(svgContent.getBytes("UTF-8"));
            
            // Создаем иконку из потока
            FlatSVGIcon icon = new FlatSVGIcon(inputStream);
            
            // Масштабируем до нужного размера
            return icon.derive(size, size);
            
        } catch (Exception e) {
            System.err.println("Failed to create fallback SVG icon: " + e.getMessage());
            return null;
        }
    }
    
    // Создает SVG содержимое для резервной иконки.
    private static String createFallbackSVGContent(String iconName, int size) {
        String symbol = "";
        String color = "#404040";
        
        switch (iconName) {
            case "football":
                symbol = "⚽";
                break;
            case "trophy":
                return String.format(
                    "<svg width='%d' height='%d' viewBox='0 0 24 24' xmlns='http://www.w3.org/2000/svg'>" +
                    "<path d='M12 2L13.09 8.26L20 9L14 14.74L15.18 21.02L12 17.77L8.82 21.02L10 14.74L4 9L10.91 8.26L12 2Z' " +
                    "fill='%s' stroke='%s' stroke-width='1'/>" +
                    "</svg>",
                    size, size, color, color
                );
            case "statistics":
                return String.format(
                    "<svg width='%d' height='%d' viewBox='0 0 24 24' xmlns='http://www.w3.org/2000/svg'>" +
                    "<rect x='3' y='14' width='4' height='6' fill='%s'/>" +
                    "<rect x='8' y='10' width='4' height='10' fill='%s'/>" +
                    "<rect x='13' y='6' width='4' height='14' fill='%s'/>" +
                    "<rect x='18' y='4' width='4' height='16' fill='%s'/>" +
                    "</svg>",
                    size, size, color, color, color, color
                );
            case "clear-circle":
                return String.format(
                    "<svg width='%d' height='%d' viewBox='0 0 24 24' xmlns='http://www.w3.org/2000/svg'>" +
                    "<circle cx='12' cy='12' r='10' fill='none' stroke='%s' stroke-width='2'/>" +
                    "<line x1='15' y1='9' x2='9' y2='15' stroke='%s' stroke-width='2'/>" +
                    "<line x1='9' y1='9' x2='15' y2='15' stroke='%s' stroke-width='2'/>" +
                    "</svg>",
                    size, size, color, color, color
                );
            case "help":
                symbol = "?";
                break;
            case "app-icon":
                return String.format(
                    "<svg width='%d' height='%d' viewBox='0 0 24 24' xmlns='http://www.w3.org/2000/svg'>" +
                    "<rect x='3' y='3' width='18' height='18' fill='none' stroke='%s' stroke-width='2'/>" +
                    "<line x1='9' y1='3' x2='9' y2='21' stroke='%s' stroke-width='1'/>" +
                    "<line x1='15' y1='3' x2='15' y2='21' stroke='%s' stroke-width='1'/>" +
                    "<line x1='3' y1='9' x2='21' y2='9' stroke='%s' stroke-width='1'/>" +
                    "<line x1='3' y1='15' x2='21' y2='15' stroke='%s' stroke-width='1'/>" +
                    "</svg>",
                    size, size, color, color, color, color, color
                );
            default:
                symbol = "●";
                break;
        }
        
        if (!symbol.isEmpty()) {
            return String.format(
                "<svg width='%d' height='%d' viewBox='0 0 %d %d' xmlns='http://www.w3.org/2000/svg'>" +
                "<text x='50%%' y='50%%' text-anchor='middle' dominant-baseline='central' " +
                "font-family='Segoe UI Symbol' font-size='%d' fill='%s'>%s</text>" +
                "</svg>",
                size, size, size, size, size * 3 / 4, color, symbol
            );
        }
        
        return "";
    }
    
    // Создает SVG иконку стандартного размера (16x16).
    public static FlatSVGIcon createIcon(String iconName) {
        return createIcon(iconName, 16);
    }
}