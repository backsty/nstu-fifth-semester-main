package pw.ns2030.event;

import pw.ns2030.model.PowerState;

import java.time.LocalDateTime;

/**
 * Событие изменения общего энергопотребления системы.
 * Генерируется PowerSystemController при изменении суммарной мощности.
 * 
 * Содержит:
 * - Текущее и предыдущее значение потребления
 * - Дельту изменения
 * - Количество активных устройств
 */
public class ConsumptionEvent extends PowerEvent {
    private final double totalPower;       // Текущее потребление (Вт)
    private final double previousPower;    // Предыдущее потребление (Вт)
    private final int activeDevices;       // Количество активных устройств

    /**
     * Полный конструктор.
     */
    public ConsumptionEvent(Object source, PowerState powerState, LocalDateTime timestamp,
                           double totalPower, double previousPower, int activeDevices) {
        super(source, powerState, timestamp);
        
        if (totalPower < 0 || previousPower < 0) {
            throw new IllegalArgumentException("Мощность не может быть отрицательной");
        }
        if (activeDevices < 0) {
            throw new IllegalArgumentException("Количество устройств не может быть отрицательным");
        }
        
        this.totalPower = totalPower;
        this.previousPower = previousPower;
        this.activeDevices = activeDevices;
    }

    /**
     * Конструктор с автоматической временной меткой.
     */
    public ConsumptionEvent(Object source, PowerState powerState,
                           double totalPower, double previousPower, int activeDevices) {
        this(source, powerState, LocalDateTime.now(), totalPower, previousPower, activeDevices);
    }

    /**
     * Упрощенный конструктор без предыдущего значения.
     */
    public ConsumptionEvent(Object source, double totalPower, int activeDevices) {
        this(source, PowerState.ON_GRID, totalPower, totalPower, activeDevices);
    }

    public double getTotalPower() {
        return totalPower;
    }

    public double getPreviousPower() {
        return previousPower;
    }

    public int getActiveDevices() {
        return activeDevices;
    }

    /**
     * Изменение потребления (положительное = рост, отрицательное = падение).
     */
    public double getPowerChange() {
        return totalPower - previousPower;
    }

    /**
     * Относительное изменение в процентах.
     */
    public double getPercentageChange() {
        if (previousPower == 0) {
            return totalPower > 0 ? Double.POSITIVE_INFINITY : 0;
        }
        return ((totalPower - previousPower) / previousPower) * 100.0;
    }

    /**
     * Проверка увеличения потребления.
     */
    public boolean isIncreasing() {
        return totalPower > previousPower;
    }

    /**
     * Проверка уменьшения потребления.
     */
    public boolean isDecreasing() {
        return totalPower < previousPower;
    }

    /**
     * Проверка стабильности потребления.
     */
    public boolean isStable() {
        return Math.abs(totalPower - previousPower) < 0.01; // Погрешность 0.01 Вт
    }

    @Override
    public String getEventType() {
        return "ConsumptionEvent";
    }

    @Override
    public String toString() {
        return String.format("ConsumptionEvent{timestamp=%s, total=%.0fW, change=%+.0fW (%.1f%%), devices=%d}",
                getFormattedTimestamp(),
                totalPower,
                getPowerChange(),
                getPercentageChange(),
                activeDevices);
    }
}