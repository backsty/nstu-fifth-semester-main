package pw.ns2030.model;

import pw.ns2030.component.LevelZone;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Objects;

/**
 * Класс для хранения данных одного измерения уровня.
 * Содержит значение, зону и временную метку. Является неизменяемым (immutable).
 */
public final class LevelData {
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS");
    
    private final LocalDateTime timestamp;
    private final double value;
    private final LevelZone zone;

    /**
     * Создает объект данных уровня с указанной временной меткой.
     * 
     * @param value значение уровня
     * @param zone зона индикатора
     * @param timestamp временная метка измерения
     */
    public LevelData(double value, LevelZone zone, LocalDateTime timestamp) {
        if (zone == null) {
            throw new IllegalArgumentException("Зона не может быть null");
        }
        if (timestamp == null) {
            throw new IllegalArgumentException("Временная метка не может быть null");
        }
        if (Double.isNaN(value) || Double.isInfinite(value)) {
            throw new IllegalArgumentException("Значение должно быть конечным числом");
        }
        
        this.value = value;
        this.zone = zone;
        this.timestamp = timestamp;
    }

    /**
     * Создает объект данных уровня с текущей временной меткой.
     * 
     * @param value значение уровня
     * @param zone зона индикатора
     */
    public LevelData(double value, LevelZone zone) {
        this(value, zone, LocalDateTime.now());
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public double getValue() {
        return value;
    }

    public LevelZone getZone() {
        return zone;
    }

    /**
     * Возвращает форматированную временную метку.
     */
    public String getFormattedTimestamp() {
        return timestamp.format(FORMATTER);
    }

    public boolean isCritical() {
        return zone.isCritical();
    }

    public boolean isWarning() {
        return zone.isWarning();
    }

    public boolean isNormal() {
        return zone.isNormal();
    }

    /**
     * Вычисляет разницу во времени с другим измерением в миллисекундах.
     * 
     * @param other другое измерение для сравнения
     * @return разница в миллисекундах
     */
    public long getTimeDifferenceMillis(LevelData other) {
        if (other == null) {
            throw new IllegalArgumentException("Другое измерение не может быть null");
        }
        return java.time.Duration.between(other.timestamp, this.timestamp).toMillis();
    }

    /**
     * Вычисляет разницу значений с другим измерением.
     * 
     * @param other другое измерение для сравнения
     * @return разница значений
     */
    public double getValueDifference(LevelData other) {
        if (other == null) {
            throw new IllegalArgumentException("Другое измерение не может быть null");
        }
        return this.value - other.value;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        LevelData levelData = (LevelData) o;
        return Double.compare(levelData.value, value) == 0 &&
               zone == levelData.zone &&
               Objects.equals(timestamp, levelData.timestamp);
    }

    @Override
    public int hashCode() {
        return Objects.hash(timestamp, value, zone);
    }

    @Override
    public String toString() {
        return String.format("LevelData{timestamp=%s, value=%.2f, zone=%s}",
                getFormattedTimestamp(), value, zone.getDisplayName());
    }

    /**
     * Создает копию с новым значением и зоной.
     */
    public LevelData withValue(double newValue, LevelZone newZone) {
        return new LevelData(newValue, newZone, this.timestamp);
    }

    /**
     * Создает копию с текущей временной меткой.
     */
    public LevelData withCurrentTimestamp() {
        return new LevelData(this.value, this.zone, LocalDateTime.now());
    }
}