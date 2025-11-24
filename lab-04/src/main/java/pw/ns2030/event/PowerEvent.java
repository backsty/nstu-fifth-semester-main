package pw.ns2030.event;

import pw.ns2030.model.PowerState;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.EventObject;

/**
 * Базовый абстрактный класс для всех событий системы энергопотребления.
 * Расширяет EventObject для совместимости с Java Beans conventions.
 * 
 * Содержит общие поля:
 * - Временная метка события
 * - Состояние системы питания
 */
public abstract class PowerEvent extends EventObject {
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS");
    
    private final LocalDateTime timestamp;
    private final PowerState powerState;

    /**
     * Конструктор базового события.
     * 
     * @param source источник события (обычно контроллер)
     * @param powerState текущее состояние питания
     * @param timestamp временная метка события
     */
    protected PowerEvent(Object source, PowerState powerState, LocalDateTime timestamp) {
        super(source);
        if (powerState == null) {
            throw new IllegalArgumentException("PowerState не может быть null");
        }
        if (timestamp == null) {
            throw new IllegalArgumentException("Timestamp не может быть null");
        }
        
        this.powerState = powerState;
        this.timestamp = timestamp;
    }

    /**
     * Конструктор с автоматической фиксацией текущего времени.
     */
    protected PowerEvent(Object source, PowerState powerState) {
        this(source, powerState, LocalDateTime.now());
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public PowerState getPowerState() {
        return powerState;
    }

    /**
     * Форматированная временная метка для отображения.
     */
    public String getFormattedTimestamp() {
        return timestamp.format(FORMATTER);
    }

    /**
     * Тип события (для логирования и фильтрации).
     */
    public abstract String getEventType();

    @Override
    public String toString() {
        return String.format("%s{timestamp=%s, state=%s, source=%s}",
                getEventType(),
                getFormattedTimestamp(),
                powerState.getDisplayName(),
                getSource().getClass().getSimpleName());
    }
}