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
 * Компонент визуализации уровня жидкости/давления с трехзонной индикацией.
 * Расширяет JPanel для встраивания в любой Swing-контейнер.
 * 
 * Архитектурные решения:
 * - Throttling перерисовки (16ms = 60 FPS) для плавности при частых обновлениях
 * - CopyOnWriteArrayList для слушателей (lock-free чтение в paintComponent)
 * - Отложенное сохранение в storage (100ms) для снижения нагрузки на память
 * - Atomic переменные для thread-safe работы с симулятором
 * 
 * Визуальные элементы:
 * - Цветовые зоны (норма/предупреждение/авария)
 * - Текущий уровень с затемнением
 * - Боковая шкала значений
 * - Числовое отображение внизу
 */
public class LevelIndicator extends JPanel {
    // Константы размеров и отступов
    private static final int DEFAULT_WIDTH = 80;
    private static final int DEFAULT_HEIGHT = 400;
    private static final int PADDING = 10;
    private static final int SCALE_WIDTH = 40;
    
    // Кэширование графических объектов для производительности
    private static final Font VALUE_FONT = new Font("Arial", Font.BOLD, 16);
    private static final Font SCALE_FONT = new Font("Arial", Font.PLAIN, 10);
    private static final Color LEVEL_OVERLAY = new Color(0, 0, 0, 100);  // Полупрозрачное затемнение
    private static final BasicStroke BORDER_STROKE = new BasicStroke(2);
    private static final BasicStroke ZONE_BORDER_STROKE = new BasicStroke(1, BasicStroke.CAP_BUTT, 
                                                                           BasicStroke.JOIN_BEVEL, 0, new float[]{5}, 0);
    
    // Throttling для оптимизации производительности
    private static final long REPAINT_THROTTLE_MS = 16;      // ~60 FPS
    private static final long STORAGE_THROTTLE_MS = 100;     // 10 записей/сек
    private static final long LISTENER_THROTTLE_MS = 50;     // 20 уведомлений/сек

    private final LevelIndicatorConfig config;
    private final LevelDataStorage storage;
    private final List<LevelDataListener> listeners;  // Thread-safe через CopyOnWriteArrayList
    
    // Throttling механизм
    private final AtomicLong lastRepaintTime = new AtomicLong(0);
    private final AtomicLong lastStorageTime = new AtomicLong(0);
    private final AtomicLong lastListenerTime = new AtomicLong(0);
    private final AtomicBoolean repaintScheduled = new AtomicBoolean(false);

    // Текущее состояние (volatile для видимости между потоками)
    private volatile double currentValue;
    private volatile LevelZone currentZone;
    private volatile double pendingValue;
    private volatile boolean hasPendingValue = false;
    
    // Флаги отображения элементов
    private boolean showValue = true;
    private boolean showScale = true;
    private boolean showZoneBorders = true;

    /**
     * Конструктор с явной конфигурацией.
     */
    public LevelIndicator(LevelIndicatorConfig config) {
        if (config == null) {
            throw new IllegalArgumentException("Конфигурация не может быть null");
        }

        this.config = config;
        this.storage = new LevelDataStorage();
        this.listeners = new CopyOnWriteArrayList<>();  // Lock-free для paintComponent - Список слушателей
        this.currentValue = config.getMinValue();
        this.currentZone = config.determineZone(currentValue);

        setPreferredSize(new Dimension(DEFAULT_WIDTH + SCALE_WIDTH, DEFAULT_HEIGHT));
        setBackground(new Color(240, 240, 240));
        setOpaque(true);
        setDoubleBuffered(true);  // Swing автоматически включает, но явно указываем
    }

    /**
     * Конструктор с настройками по умолчанию.
     */
    public LevelIndicator() {
        this(LevelIndicatorConfig.createDefault());
    }

    /**
     * Установка нового значения с автоматическим определением зоны.
     * Применяется throttling для оптимизации при быстрых изменениях.
     */
    public void setValue(double value) {
        // Clamp в допустимый диапазон
        // Шаг 1: Ограничиваем значение диапазоном [0, 100]
        if (!config.isInRange(value)) {
            value = Math.max(config.getMinValue(), Math.min(config.getMaxValue(), value));
        }

        // Шаг 2: Сохраняем старые значения для события
        double oldValue = this.currentValue;
        LevelZone oldZone = this.currentZone;

        // Шаг 3: Обновляем текущие значения
        this.currentValue = value;
        this.currentZone = config.determineZone(value);
        this.pendingValue = value;
        this.hasPendingValue = true;

        long currentTime = System.currentTimeMillis();

        // Шаг 4: THROTTLED сохранение в storage (не чаще раза в 100ms)
        if (currentTime - lastStorageTime.get() >= STORAGE_THROTTLE_MS) {
            LevelData data = new LevelData(value, currentZone);
            storage.addData(data);
            lastStorageTime.set(currentTime);
        }

        // Шаг 5: THROTTLED уведомление слушателей (не чаще раза в 50ms)
        if (currentTime - lastListenerTime.get() >= LISTENER_THROTTLE_MS) {
            fireDataUpdateEvent(oldValue, value, oldZone, currentZone);
            lastListenerTime.set(currentTime);
        }

        // Шаг 6: THROTTLED перерисовка (не чаще 60 FPS = 16ms)
        scheduleRepaint();
    }

    /**
     * Отложенная перерисовка с ограничением частоты (60 FPS).
     * Использует Timer для плавности анимации.
     */
    private void scheduleRepaint() {
        if (repaintScheduled.compareAndSet(false, true)) {
            long currentTime = System.currentTimeMillis();
            long timeSinceLastRepaint = currentTime - lastRepaintTime.get();
            
            if (timeSinceLastRepaint >= REPAINT_THROTTLE_MS) {
                // Достаточно времени прошло - рисуем сразу
                lastRepaintTime.set(currentTime);
                repaintScheduled.set(false);
                repaint();
            } else {
                // Откладываем до следующего кадра
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

    /**
     * Получение статистики с гарантией записи последнего значения.
     */
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

    // Методы управления отображением элементов
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

    /**
     * Основной метод отрисовки - вызывается Swing при необходимости.
     * Оптимизирован для скорости (RENDER_SPEED вместо RENDER_QUALITY).
     */
    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2d = (Graphics2D) g.create();

        // Настройка рендеринга: приоритет скорости для плавных обновлений
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_SPEED);
        g2d.setRenderingHint(RenderingHints.KEY_COLOR_RENDERING, RenderingHints.VALUE_COLOR_RENDER_SPEED);

        int width = getWidth();
        int height = getHeight();

        // Расчет области индикатора с учетом шкалы
        int indicatorX = PADDING + (showScale ? SCALE_WIDTH : 0);
        int indicatorY = PADDING;
        int indicatorWidth = width - indicatorX - PADDING;
        int indicatorHeight = height - 2 * PADDING;

        // Отрисовка элементов в порядке наложения (от фона к переднему плану)
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

    /**
     * Отрисовка цветовых зон (фон индикатора).
     * Заполняет прямоугольники снизу вверх согласно границам зон.
     */
    private void drawZones(Graphics2D g2d, int x, int y, int width, int height) {
        double range = config.getRange();

        // Преобразование границ зон в пиксельные координаты
        double criticalLowHeight = ((config.getCriticalLow() - config.getMinValue()) / range) * height;
        double warningLowHeight = ((config.getWarningLow() - config.getMinValue()) / range) * height;
        double warningHighHeight = ((config.getWarningHigh() - config.getMinValue()) / range) * height;
        double criticalHighHeight = ((config.getCriticalHigh() - config.getMinValue()) / range) * height;

        // Отрисовка зон снизу вверх (критическая -> предупреждение -> норма -> предупреждение -> критическая)
        g2d.setColor(LevelZone.CRITICAL.getColor());
        g2d.fillRect(x, y, width, (int) criticalLowHeight);

        g2d.setColor(LevelZone.WARNING.getColor());
        g2d.fillRect(x, y + (int) criticalLowHeight, width, (int) (warningLowHeight - criticalLowHeight));

        g2d.setColor(LevelZone.NORMAL.getColor());
        g2d.fillRect(x, y + (int) warningLowHeight, width, (int) (warningHighHeight - warningLowHeight));

        g2d.setColor(LevelZone.WARNING.getColor());
        g2d.fillRect(x, y + (int) warningHighHeight, width, (int) (criticalHighHeight - warningHighHeight));

        g2d.setColor(LevelZone.CRITICAL.getColor());
        g2d.fillRect(x, y + (int) criticalHighHeight, width, height - (int) criticalHighHeight);
    }

    /**
     * Отрисовка текущего уровня - затемненная область снизу + линия границы.
     */
    private void drawLevel(Graphics2D g2d, int x, int y, int width, int height) {
        double normalized = config.normalize(currentValue);
        int levelHeight = (int) (normalized * height);
        int levelY = y + height - levelHeight;

        // Затемнение заполненной области
        g2d.setColor(LEVEL_OVERLAY);
        g2d.fillRect(x, levelY, width, levelHeight);

        // Линия текущего уровня (акцентная)
        g2d.setColor(currentZone.getDarkerColor());
        g2d.setStroke(BORDER_STROKE);
        g2d.drawLine(x, levelY, x + width, levelY);
    }

    /**
     * Отрисовка границ зон пунктирными линиями.
     */
    private void drawZoneBorders(Graphics2D g2d, int x, int y, int width, int height) {
        g2d.setColor(Color.DARK_GRAY);
        g2d.setStroke(ZONE_BORDER_STROKE);

        double range = config.getRange();
        
        // Вычисление Y-координат границ (от низа)
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
     * Отрисовка боковой шкалы значений с метками.
     */
    private void drawScale(Graphics2D g2d, int x, int y, int width, int height) {
        g2d.setColor(Color.DARK_GRAY);
        g2d.setFont(SCALE_FONT);

        int divisions = 10;
        double step = config.getRange() / divisions;

        for (int i = 0; i <= divisions; i++) {
            double value = config.getMinValue() + (i * step);
            int scaleY = y + height - (int) ((i / (double) divisions) * height);

            // Короткая черточка шкалы
            g2d.drawLine(x + width - 5, scaleY, x + width, scaleY);

            // Числовая метка (выравнивание справа)
            String label = String.format("%.0f", value);
            FontMetrics fm = g2d.getFontMetrics();
            int labelWidth = fm.stringWidth(label);
            g2d.drawString(label, x + width - labelWidth - 8, scaleY + 4);
        }
    }

    /**
     * Отрисовка числового значения под индикатором в рамке цвета зоны.
     */
    private void drawValueText(Graphics2D g2d, int x, int y, int width, int height) {
        g2d.setFont(VALUE_FONT);
        String valueText = String.format("%.1f", currentValue);

        FontMetrics fm = g2d.getFontMetrics();
        int textWidth = fm.stringWidth(valueText);
        int textHeight = fm.getHeight();

        int textX = x + (width - textWidth) / 2;
        int textY = y + height + textHeight + 5;

        // Белый фон под текстом
        g2d.setColor(Color.WHITE);
        Rectangle2D rect = new Rectangle2D.Double(textX - 5, textY - textHeight + 5, textWidth + 10, textHeight);
        g2d.fill(rect);

        // Рамка цвета текущей зоны
        g2d.setColor(currentZone.getColor());
        g2d.draw(rect);

        // Сам текст
        g2d.setColor(Color.BLACK);
        g2d.drawString(valueText, textX, textY);
    }

    /**
     * Отрисовка рамки индикатора.
     */
    private void drawBorder(Graphics2D g2d, int x, int y, int width, int height) {
        g2d.setColor(Color.DARK_GRAY);
        g2d.setStroke(BORDER_STROKE);
        g2d.drawRect(x, y, width, height);
    }

    /**
     * Уведомление слушателей об изменении состояния.
     * Обработка ошибок предотвращает сбой при ошибке в одном слушателе.
     */
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
        return new Dimension(DEFAULT_WIDTH + (showScale ? SCALE_WIDTH : 0), DEFAULT_HEIGHT);
    }

    @Override
    public Dimension getMinimumSize() {
        return new Dimension(60, 200);
    }

    @Override
    public String toString() {
        return String.format("LevelIndicator{value=%.2f, zone=%s, listeners=%d, dataPoints=%d}",
                currentValue, currentZone.getDisplayName(), listeners.size(), storage.size());
    }
}