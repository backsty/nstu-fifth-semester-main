package pw.ns2030.listener;

import pw.ns2030.component.LevelZone;
import java.time.LocalDateTime;
import java.util.EventObject;

/**
 * Event объект для паттерна Observer - фиксирует изменение состояния индикатора.
 * Расширяет EventObject из стандартной библиотеки (Java Beans conventions).
 * 
 * Содержит:
 * - Старое/новое значение и зону (для анализа дельты)
 * - Временную метку (для соотношения с внешними событиями)
 * - Вычисляемые предикаты (isWorsening, isCritical) для упрощения обработки
 */
public class LevelChangeEvent extends EventObject {
    private final double oldValue;
    private final double newValue;
    private final LevelZone oldZone;
    private final LevelZone newZone;
    private final LocalDateTime timestamp;

    /**
     * Полный конструктор (для восстановления событий из лога).
     */
    public LevelChangeEvent(Object source, double oldValue, double newValue, 
                           LevelZone oldZone, LevelZone newZone, LocalDateTime timestamp) {
        super(source);
        this.oldValue = oldValue;
        this.newValue = newValue;
        this.oldZone = oldZone;
        this.newZone = newZone;
        this.timestamp = timestamp;
    }

    /**
     * Основной конструктор - автоматическая фиксация времени события.
     */
    public LevelChangeEvent(Object source, double oldValue, double newValue, 
                           LevelZone oldZone, LevelZone newZone) {
        this(source, oldValue, newValue, oldZone, newZone, LocalDateTime.now());
    }

    public double getOldValue() {
        return oldValue;
    }

    public double getNewValue() {
        return newValue;
    }

    public LevelZone getOldZone() {
        return oldZone;
    }

    public LevelZone getNewZone() {
        return newZone;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    /**
     * Дельта значения (положительная = рост, отрицательная = падение).
     */
    public double getValueChange() {
        return newValue - oldValue;
    }

    /**
     * Проверка пересечения границы зоны (триггер для алертов).
     */
    public boolean hasZoneChanged() {
        return oldZone != newZone;
    }

    public boolean hasValueChanged() {
        return Double.compare(oldValue, newValue) != 0;
    }

    /**
     * Переход в более опасную зону (критический алерт).
     */
    public boolean isWorsening() {
        return newZone.getSeverityLevel() > oldZone.getSeverityLevel();
    }

    /**
     * Переход в менее опасную зону (recovery notification).
     */
    public boolean isImproving() {
        return newZone.getSeverityLevel() < oldZone.getSeverityLevel();
    }

    // Предикаты для удобной фильтрации событий
    public boolean isCritical() {
        return newZone.isCritical();
    }

    public boolean isWarning() {
        return newZone.isWarning();
    }

    public boolean isNormal() {
        return newZone.isNormal();
    }

    /**
     * Направление тренда (для стрелок индикации).
     */
    public ChangeDirection getDirection() {
        double delta = getValueChange();
        if (delta > 0) return ChangeDirection.INCREASING;
        if (delta < 0) return ChangeDirection.DECREASING;
        return ChangeDirection.STABLE;
    }

    @Override
    public String toString() {
        return String.format("LevelChangeEvent{oldValue=%.2f, newValue=%.2f, " +
                        "oldZone=%s, newZone=%s, change=%.2f, timestamp=%s}",
                oldValue, newValue, oldZone.getDisplayName(), newZone.getDisplayName(),
                getValueChange(), timestamp);
    }

    /**
     * Enum направления изменения (для визуальной индикации тренда).
     */
    public enum ChangeDirection {
        INCREASING("Возрастание"),
        DECREASING("Убывание"),
        STABLE("Стабильно");

        private final String displayName;

        ChangeDirection(String displayName) {
            this.displayName = displayName;
        }

        public String getDisplayName() {
            return displayName;
        }

        @Override
        public String toString() {
            return displayName;
        }
    }
}