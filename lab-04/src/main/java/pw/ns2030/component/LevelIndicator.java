package pw.ns2030.component;

import pw.ns2030.listener.LevelChangeEvent;
import pw.ns2030.listener.LevelDataListener;
import pw.ns2030.model.LevelData;
import pw.ns2030.model.LevelDataStorage;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.Rectangle2D;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Компонент визуализации уровня с трехзонной индикацией.
 * Поддерживает throttling для оптимизации при частых обновлениях.
 */
public class LevelIndicator extends JPanel {
    private static final int DEFAULT_WIDTH = 80;
    private static final int DEFAULT_HEIGHT = 400;
    private static final int MIN_HEIGHT = 200;
    private static final int PADDING = 10;
    private static final int SCALE_WIDTH = 40;
    
    private static final Font VALUE_FONT = new Font("Arial", Font.BOLD, 16);
    private static final Font SCALE_FONT = new Font("Arial", Font.PLAIN, 10);
    private static final Color LEVEL_OVERLAY = new Color(0, 0, 0, 100);
    private static final BasicStroke BORDER_STROKE = new BasicStroke(2);
    private static final BasicStroke ZONE_BORDER_STROKE = new BasicStroke(1, BasicStroke.CAP_BUTT, 
                                                                           BasicStroke.JOIN_BEVEL, 0, new float[]{5}, 0);
    
    private static final long REPAINT_THROTTLE_MS = 16;
    private static final long STORAGE_THROTTLE_MS = 100;
    private static final long LISTENER_THROTTLE_MS = 50;

    private volatile LevelIndicatorConfig config;
    private final LevelDataStorage storage;
    private final List<LevelDataListener> listeners;
    
    private final AtomicLong lastRepaintTime = new AtomicLong(0);
    private final AtomicLong lastStorageTime = new AtomicLong(0);
    private final AtomicLong lastListenerTime = new AtomicLong(0);
    private final AtomicBoolean repaintScheduled = new AtomicBoolean(false);

    private volatile double currentValue;
    private volatile LevelZone currentZone;
    private volatile double pendingValue;
    private volatile boolean hasPendingValue = false;
    
    private boolean showValue = true;
    private boolean showScale = true;
    private boolean showZoneBorders = true;

    public LevelIndicator(LevelIndicatorConfig config) {
        if (config == null) {
            throw new IllegalArgumentException("Конфигурация не может быть null");
        }

        this.config = config;
        this.storage = new LevelDataStorage();
        this.listeners = new CopyOnWriteArrayList<>();
        this.currentValue = config.getMinValue();
        this.currentZone = config.determineZone(currentValue);

        setPreferredSize(new Dimension(DEFAULT_WIDTH + SCALE_WIDTH, DEFAULT_HEIGHT));
        setBackground(new Color(240, 240, 240));
        setOpaque(true);
        setDoubleBuffered(true);
    }

    public LevelIndicator() {
        this(LevelIndicatorConfig.createDefault());
    }

    public void updateConfig(LevelIndicatorConfig newConfig) {
        if (newConfig == null) {
            throw new IllegalArgumentException("Новая конфигурация не может быть null");
        }

        LevelIndicatorConfig oldConfig = this.config;
        this.config = newConfig;

        LevelZone oldZone = this.currentZone;
        this.currentZone = newConfig.determineZone(this.currentValue);

        if (!newConfig.isInRange(this.currentValue)) {
            double clampedValue = Math.max(newConfig.getMinValue(), 
                                          Math.min(newConfig.getMaxValue(), this.currentValue));
            setValue(clampedValue);
        } else if (oldZone != this.currentZone) {
            fireDataUpdateEvent(this.currentValue, this.currentValue, oldZone, this.currentZone);
        }

        storage.addData(new LevelData(this.currentValue, this.currentZone));
        SwingUtilities.invokeLater(this::repaint);
    }

    public void setValue(double value) {
        if (!config.isInRange(value)) {
            value = Math.max(config.getMinValue(), Math.min(config.getMaxValue(), value));
        }

        double oldValue = this.currentValue;
        LevelZone oldZone = this.currentZone;

        this.currentValue = value;
        this.currentZone = config.determineZone(value);
        this.pendingValue = value;
        this.hasPendingValue = true;

        long currentTime = System.currentTimeMillis();

        if (currentTime - lastStorageTime.get() >= STORAGE_THROTTLE_MS) {
            LevelData data = new LevelData(value, currentZone);
            storage.addData(data);
            lastStorageTime.set(currentTime);
        }

        if (currentTime - lastListenerTime.get() >= LISTENER_THROTTLE_MS) {
            fireDataUpdateEvent(oldValue, value, oldZone, currentZone);
            lastListenerTime.set(currentTime);
        }

        scheduleRepaint();
    }

    private void scheduleRepaint() {
        if (repaintScheduled.compareAndSet(false, true)) {
            long currentTime = System.currentTimeMillis();
            long timeSinceLastRepaint = currentTime - lastRepaintTime.get();
            
            if (timeSinceLastRepaint >= REPAINT_THROTTLE_MS) {
                lastRepaintTime.set(currentTime);
                repaintScheduled.set(false);
                repaint();
            } else {
                Timer timer = new Timer((int)(REPAINT_THROTTLE_MS - timeSinceLastRepaint), e -> {
                    lastRepaintTime.set(System.currentTimeMillis());
                    repaintScheduled.set(false);
                    repaint();
                });
                timer.setRepeats(false);
                timer.start();
            }
        }
    }

    public double getValue() {
        return currentValue;
    }

    public LevelZone getCurrentZone() {
        return currentZone;
    }

    public LevelIndicatorConfig getConfig() {
        return config;
    }

    public List<LevelData> getAccumulatedData() {
        return storage.getData();
    }

    public LevelDataStorage.StorageStatistics getStatistics() {
        if (hasPendingValue) {
            LevelData data = new LevelData(pendingValue, config.determineZone(pendingValue));
            storage.addData(data);
            hasPendingValue = false;
        }
        return storage.getStatistics();
    }

    public void clearData() {
        storage.clear();
    }

    public void addLevelDataListener(LevelDataListener listener) {
        if (listener != null) {
            listeners.add(listener);
        }
    }

    public void removeLevelDataListener(LevelDataListener listener) {
        listeners.remove(listener);
    }

    public void removeAllListeners() {
        listeners.clear();
    }

    public int getListenerCount() {
        return listeners.size();
    }

    public void setShowValue(boolean showValue) {
        this.showValue = showValue;
        repaint();
    }

    public void setShowScale(boolean showScale) {
        this.showScale = showScale;
        repaint();
    }

    public void setShowZoneBorders(boolean showZoneBorders) {
        this.showZoneBorders = showZoneBorders;
        repaint();
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2d = (Graphics2D) g.create();

        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_SPEED);
        g2d.setRenderingHint(RenderingHints.KEY_COLOR_RENDERING, RenderingHints.VALUE_COLOR_RENDER_SPEED);

        int width = getWidth();
        int height = getHeight();

        int indicatorX = PADDING + (showScale ? SCALE_WIDTH : 0);
        int indicatorY = PADDING;
        int indicatorWidth = width - indicatorX - PADDING;
        int indicatorHeight = height - 2 * PADDING;

        drawZones(g2d, indicatorX, indicatorY, indicatorWidth, indicatorHeight);
        drawLevel(g2d, indicatorX, indicatorY, indicatorWidth, indicatorHeight);

        if (showZoneBorders) {
            drawZoneBorders(g2d, indicatorX, indicatorY, indicatorWidth, indicatorHeight);
        }

        if (showScale) {
            drawScale(g2d, PADDING, indicatorY, SCALE_WIDTH - 5, indicatorHeight);
        }

        if (showValue) {
            drawValueText(g2d, indicatorX, indicatorY, indicatorWidth, indicatorHeight);
        }

        drawBorder(g2d, indicatorX, indicatorY, indicatorWidth, indicatorHeight);

        g2d.dispose();
    }

    private void drawZones(Graphics2D g2d, int x, int y, int width, int height) {
        double range = config.getRange();
        
        class ZoneBoundary implements Comparable<ZoneBoundary> {
            double value;
            LevelZone zone;
            
            ZoneBoundary(double value, LevelZone zone) {
                this.value = value;
                this.zone = zone;
            }
            
            @Override
            public int compareTo(ZoneBoundary other) {
                return Double.compare(this.value, other.value);
            }
        }
        
        List<ZoneBoundary> boundaries = new java.util.ArrayList<>();
        boundaries.add(new ZoneBoundary(config.getMinValue(), null));
        boundaries.add(new ZoneBoundary(config.getCriticalLow(), null));
        boundaries.add(new ZoneBoundary(config.getWarningLow(), null));
        boundaries.add(new ZoneBoundary(config.getWarningHigh(), null));
        boundaries.add(new ZoneBoundary(config.getCriticalHigh(), null));
        boundaries.add(new ZoneBoundary(config.getMaxValue(), null));
        
        java.util.Collections.sort(boundaries);
        
        List<ZoneBoundary> uniqueBoundaries = new java.util.ArrayList<>();
        for (int i = 0; i < boundaries.size(); i++) {
            if (i == 0 || Math.abs(boundaries.get(i).value - boundaries.get(i-1).value) > 0.001) {
                uniqueBoundaries.add(boundaries.get(i));
            }
        }
        
        for (int i = 0; i < uniqueBoundaries.size() - 1; i++) {
            double bottomValue = uniqueBoundaries.get(i).value;
            double topValue = uniqueBoundaries.get(i + 1).value;
            double midValue = (bottomValue + topValue) / 2.0;
            
            LevelZone zone = config.determineZone(midValue);
            
            int bottomY = y + height - (int)(((bottomValue - config.getMinValue()) / range) * height);
            int topY = y + height - (int)(((topValue - config.getMinValue()) / range) * height);
            int segmentHeight = bottomY - topY;
            
            if (segmentHeight > 0) {
                g2d.setColor(zone.getColor());
                g2d.fillRect(x, topY, width, segmentHeight);
            }
        }
    }

    private void drawLevel(Graphics2D g2d, int x, int y, int width, int height) {
        double normalized = config.normalize(currentValue);
        int levelHeight = (int) (normalized * height);
        int levelY = y + height - levelHeight;

        g2d.setColor(LEVEL_OVERLAY);
        g2d.fillRect(x, levelY, width, levelHeight);

        g2d.setColor(currentZone.getDarkerColor());
        g2d.setStroke(BORDER_STROKE);
        g2d.drawLine(x, levelY, x + width, levelY);
    }

    private void drawZoneBorders(Graphics2D g2d, int x, int y, int width, int height) {
        g2d.setColor(Color.DARK_GRAY);
        g2d.setStroke(ZONE_BORDER_STROKE);

        double range = config.getRange();
        
        int criticalLowY = y + height - (int) (((config.getCriticalLow() - config.getMinValue()) / range) * height);
        int warningLowY = y + height - (int) (((config.getWarningLow() - config.getMinValue()) / range) * height);
        int warningHighY = y + height - (int) (((config.getWarningHigh() - config.getMinValue()) / range) * height);
        int criticalHighY = y + height - (int) (((config.getCriticalHigh() - config.getMinValue()) / range) * height);

        g2d.drawLine(x, criticalLowY, x + width, criticalLowY);
        g2d.drawLine(x, warningLowY, x + width, warningLowY);
        g2d.drawLine(x, warningHighY, x + width, warningHighY);
        g2d.drawLine(x, criticalHighY, x + width, criticalHighY);
    }

    /**
     * Отрисовка шкалы с автоматическим включением границ зон.
     * Отображает min, max, равномерные деления И границы критических/warning зон.
     */
    private void drawScale(Graphics2D g2d, int x, int y, int width, int height) {
        g2d.setColor(Color.DARK_GRAY);
        g2d.setFont(SCALE_FONT);
        FontMetrics fm = g2d.getFontMetrics();

        // Собираем все важные значения для отображения
        java.util.Set<Double> scaleValues = new java.util.TreeSet<>();
        
        // 1. Обязательные: минимум и максимум
        scaleValues.add(config.getMinValue());
        scaleValues.add(config.getMaxValue());
        
        // 2. Равномерные деления (стандартная сетка)
        int divisions = 5;
        double step = config.getRange() / divisions;
        for (int i = 1; i < divisions; i++) {
            scaleValues.add(config.getMinValue() + (step * i));
        }
        
        // 3. Границы зон (если они не совпадают с уже добавленными)
        scaleValues.add(config.getCriticalLow());
        scaleValues.add(config.getCriticalHigh());
        scaleValues.add(config.getWarningLow());
        scaleValues.add(config.getWarningHigh());
        
        // Отрисовка всех меток
        for (double value : scaleValues) {
            // Y-координата метки (от низа вверх)
            double valueRatio = (value - config.getMinValue()) / config.getRange();
            int scaleY = y + height - (int) (valueRatio * height);
            
            // Форматирование значения
            String label = String.format("%.0f", value);
            int labelWidth = fm.stringWidth(label);
            
            // Позиция текста: выравнивание по правому краю
            int labelX = x + width - labelWidth;
            
            // Вертикальное центрирование текста
            int labelY = scaleY + (fm.getAscent() - fm.getDescent()) / 2;
            
            // Проверка наложения меток (минимальный отступ 12 пикселей)
            boolean tooClose = false;
            for (double other : scaleValues) {
                if (other != value) {
                    double otherRatio = (other - config.getMinValue()) / config.getRange();
                    int otherY = y + height - (int) (otherRatio * height);
                    if (Math.abs(scaleY - otherY) < 12 && Math.abs(value - other) > 0.01) {
                        tooClose = true;
                        break;
                    }
                }
            }
            
            // Рисуем метку только если не накладывается
            if (!tooClose || value == config.getMinValue() || value == config.getMaxValue()) {
                g2d.drawString(label, labelX, labelY);
                
                // Засечка (для границ зон - длиннее)
                boolean isZoneBoundary = value == config.getCriticalLow() || 
                                        value == config.getCriticalHigh() ||
                                        value == config.getWarningLow() || 
                                        value == config.getWarningHigh();
                int tickLength = isZoneBoundary ? 8 : 5;
                g2d.drawLine(x + width, scaleY, x + width + tickLength, scaleY);
            }
        }
    }

    private void drawValueText(Graphics2D g2d, int x, int y, int width, int height) {
        g2d.setFont(VALUE_FONT);
        String valueText = String.format("%.1f", currentValue);

        FontMetrics fm = g2d.getFontMetrics();
        int textWidth = fm.stringWidth(valueText);
        int textHeight = fm.getHeight();

        int textX = x + (width - textWidth) / 2;
        int textY = y + height + textHeight + 5;

        g2d.setColor(Color.WHITE);
        Rectangle2D rect = new Rectangle2D.Double(textX - 5, textY - textHeight + 5, textWidth + 10, textHeight);
        g2d.fill(rect);

        g2d.setColor(currentZone.getColor());
        g2d.draw(rect);

        g2d.setColor(Color.BLACK);
        g2d.drawString(valueText, textX, textY);
    }

    private void drawBorder(Graphics2D g2d, int x, int y, int width, int height) {
        g2d.setColor(Color.DARK_GRAY);
        g2d.setStroke(BORDER_STROKE);
        g2d.drawRect(x, y, width, height);
    }

    private void fireDataUpdateEvent(double oldValue, double newValue, LevelZone oldZone, LevelZone newZone) {
        if (listeners.isEmpty()) {
            return;
        }

        LevelChangeEvent event = new LevelChangeEvent(this, oldValue, newValue, oldZone, newZone, LocalDateTime.now());

        for (LevelDataListener listener : listeners) {
            try {
                listener.onLevelDataUpdate(event);
            } catch (Exception e) {
                System.err.println("Ошибка при вызове слушателя: " + e.getMessage());
            }
        }
    }

    @Override
    public Dimension getPreferredSize() {
        Container parent = getParent();
        if (parent != null) {
            int parentHeight = parent.getHeight();
            if (parentHeight > MIN_HEIGHT) {
                int dynamicHeight = (int) (parentHeight * 0.9);
                dynamicHeight = Math.max(MIN_HEIGHT, Math.min(dynamicHeight, DEFAULT_HEIGHT));
                return new Dimension(DEFAULT_WIDTH + SCALE_WIDTH, dynamicHeight);
            }
        }
        return new Dimension(DEFAULT_WIDTH + SCALE_WIDTH, DEFAULT_HEIGHT);
    }

    @Override
    public Dimension getMinimumSize() {
        return new Dimension(DEFAULT_WIDTH + SCALE_WIDTH, MIN_HEIGHT);
    }

    @Override
    public Dimension getMaximumSize() {
        return new Dimension(DEFAULT_WIDTH + SCALE_WIDTH, 600);
    }

    @Override
    public String toString() {
        return String.format("LevelIndicator{value=%.2f, zone=%s, listeners=%d, dataPoints=%d}",
                currentValue, currentZone.getDisplayName(), listeners.size(), storage.size());
    }
}