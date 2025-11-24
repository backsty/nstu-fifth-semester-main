package pw.ns2030.component;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.IOException;
import javax.imageio.ImageIO;

/**
 * Компонент тумблера с иконками ON/OFF.
 * Отображает состояние устройства визуально через PNG иконки.
 * Поддерживает пропорциональное масштабирование без искажений.
 */
public class ToggleSwitch extends JPanel {
    // ИСПРАВЛЕНО: Пропорции реальной иконки (примерно 2.5:1 - широкий овал)
    private static final int ICON_WIDTH = 100;   // Ширина (было 40)
    private static final int ICON_HEIGHT = 50;   // Высота (было 80)
    
    private BufferedImage onIcon;
    private BufferedImage offIcon;
    private volatile boolean state;

    /**
     * Конструктор тумблера с начальным состоянием.
     * 
     * @param initialState true = включен (зеленый), false = выключен (красный)
     */
    public ToggleSwitch(boolean initialState) {
        this.state = initialState;
        
        setPreferredSize(new Dimension(ICON_WIDTH, ICON_HEIGHT));
        setMinimumSize(new Dimension(ICON_WIDTH, ICON_HEIGHT));
        setMaximumSize(new Dimension(ICON_WIDTH, ICON_HEIGHT));
        setOpaque(false);
        
        loadIcons();
    }

    /**
     * Загрузка иконок из resources.
     */
    private void loadIcons() {
        try {
            onIcon = ImageIO.read(
                getClass().getResourceAsStream("/icons/switch-on-icon_34343.png")
            );
            
            offIcon = ImageIO.read(
                getClass().getResourceAsStream("/icons/switch-off-icon_34344.png")
            );
            
            System.out.println("[ToggleSwitch] Иконки успешно загружены");
            
        } catch (IOException | IllegalArgumentException e) {
            System.err.println("[ToggleSwitch] Ошибка загрузки иконок: " + e.getMessage());
            e.printStackTrace();
            
            onIcon = createFallbackIcon(new Color(76, 175, 80));
            offIcon = createFallbackIcon(new Color(244, 67, 54));
        }
    }

    /**
     * Создание запасной иконки если файлы не загрузились.
     * Рисует простой горизонтальный тумблер.
     */
    private BufferedImage createFallbackIcon(Color color) {
        BufferedImage img = new BufferedImage(ICON_WIDTH, ICON_HEIGHT, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2d = img.createGraphics();
        
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        
        // Фон тумблера (серый горизонтальный овал)
        g2d.setColor(Color.LIGHT_GRAY);
        g2d.fillRoundRect(5, 5, ICON_WIDTH - 10, ICON_HEIGHT - 10, ICON_HEIGHT - 10, ICON_HEIGHT - 10);
        
        // Ручка тумблера (цветной круг)
        g2d.setColor(color);
        int circleSize = ICON_HEIGHT - 16;
        int circleX = state ? ICON_WIDTH - circleSize - 12 : 8;  // Слева/справа
        int circleY = 8;
        g2d.fillOval(circleX, circleY, circleSize, circleSize);
        
        // Обводка
        g2d.setColor(Color.DARK_GRAY);
        g2d.setStroke(new BasicStroke(2));
        g2d.drawRoundRect(5, 5, ICON_WIDTH - 10, ICON_HEIGHT - 10, ICON_HEIGHT - 10, ICON_HEIGHT - 10);
        
        g2d.dispose();
        return img;
    }

    /**
     * Установка состояния тумблера.
     * 
     * @param state true = включен, false = выключен
     */
    public void setState(boolean state) {
        if (this.state != state) {
            this.state = state;
            repaint();
        }
    }

    public boolean getState() {
        return state;
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        
        if (onIcon == null || offIcon == null) {
            return;
        }
        
        Graphics2D g2d = (Graphics2D) g.create();
        g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        
        BufferedImage currentIcon = state ? onIcon : offIcon;
        
        // ИСПРАВЛЕНО: Пропорциональное масштабирование без искажений
        int imgWidth = currentIcon.getWidth();
        int imgHeight = currentIcon.getHeight();
        double imgAspect = (double) imgWidth / imgHeight;
        
        int panelWidth = getWidth();
        int panelHeight = getHeight();
        double panelAspect = (double) panelWidth / panelHeight;
        
        int drawX, drawY, drawWidth, drawHeight;
        
        if (imgAspect > panelAspect) {
            // Картинка шире - масштабируем по ширине
            drawWidth = panelWidth;
            drawHeight = (int) (panelWidth / imgAspect);
            drawX = 0;
            drawY = (panelHeight - drawHeight) / 2;
        } else {
            // Картинка выше - масштабируем по высоте
            drawHeight = panelHeight;
            drawWidth = (int) (panelHeight * imgAspect);
            drawX = (panelWidth - drawWidth) / 2;
            drawY = 0;
        }
        
        g2d.drawImage(currentIcon, drawX, drawY, drawWidth, drawHeight, null);
        
        g2d.dispose();
    }

    @Override
    public Dimension getPreferredSize() {
        return new Dimension(ICON_WIDTH, ICON_HEIGHT);
    }

    @Override
    public Dimension getMinimumSize() {
        return new Dimension(ICON_WIDTH, ICON_HEIGHT);
    }

    @Override
    public Dimension getMaximumSize() {
        return new Dimension(ICON_WIDTH, ICON_HEIGHT);
    }
}