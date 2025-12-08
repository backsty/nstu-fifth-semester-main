package pw.ns2030.event;

import pw.ns2030.model.PowerState;

import java.time.LocalDateTime;

/**
 * Событие изменения уровня батареи ИБП компьютера.
 * Генерируется ComputerController при разряде/зарядке батареи.
 * 
 * Содержит:
 * - Уровень заряда (0-100%)
 * - Режим (зарядка/разрядка)
 * - Оставшееся время работы
 * - ID устройства
 */
public class BatteryLevelEvent extends PowerEvent {
    private final String deviceId;              // ID компьютера
    private final double batteryLevel;          // Уровень заряда (0-100%)
    private final boolean isCharging;           // Режим зарядки
    private final double remainingMinutes;      // Оставшееся время работы (минуты)

    /**
     * Полный конструктор.
     */
    public BatteryLevelEvent(Object source, PowerState powerState, LocalDateTime timestamp,
                            String deviceId, double batteryLevel, boolean isCharging, 
                            double remainingMinutes) {
        super(source, powerState, timestamp);
        
        if (deviceId == null || deviceId.trim().isEmpty()) {
            throw new IllegalArgumentException("Device ID не может быть пустым");
        }
        if (batteryLevel < 0 || batteryLevel > 100) {
            throw new IllegalArgumentException("Уровень батареи должен быть в диапазоне 0-100%");
        }
        if (remainingMinutes < 0) {
            throw new IllegalArgumentException("Оставшееся время не может быть отрицательным");
        }
        
        this.deviceId = deviceId;
        this.batteryLevel = batteryLevel;
        this.isCharging = isCharging;
        this.remainingMinutes = remainingMinutes;
    }

    /**
     * Конструктор с автоматической временной меткой.
     */
    public BatteryLevelEvent(Object source, PowerState powerState, String deviceId,
                            double batteryLevel, boolean isCharging, double remainingMinutes) {
        this(source, powerState, LocalDateTime.now(), deviceId, batteryLevel, isCharging, remainingMinutes);
    }

    /**
     * Упрощенный конструктор без оставшегося времени.
     */
    public BatteryLevelEvent(Object source, String deviceId, double batteryLevel, boolean isCharging) {
        this(source, isCharging ? PowerState.ON_GRID : PowerState.ON_BATTERY,
             deviceId, batteryLevel, isCharging, 0);
    }

    public String getDeviceId() {
        return deviceId;
    }

    public double getBatteryLevel() {
        return batteryLevel;
    }

    public boolean isCharging() {
        return isCharging;
    }

    public double getRemainingMinutes() {
        return remainingMinutes;
    }

    /**
     * Проверка низкого заряда батареи (< 20%).
     */
    public boolean isLowBattery() {
        return batteryLevel < 20.0;
    }

    /**
     * Проверка критически низкого заряда (< 10%).
     */
    public boolean isCriticalBattery() {
        return batteryLevel < 10.0;
    }

    /**
     * Проверка полной зарядки (>= 100%).
     */
    public boolean isFullyCharged() {
        return batteryLevel >= 100.0;
    }

    /**
     * Проверка разрядки батареи.
     */
    public boolean isDraining() {
        return !isCharging && getPowerState() == PowerState.ON_BATTERY;
    }

    /**
     * Уровень заряда в виде зоны (для использования с LevelIndicator).
     */
    public BatteryZone getBatteryZone() {
        if (batteryLevel >= 60) return BatteryZone.GOOD;
        if (batteryLevel >= 20) return BatteryZone.FAIR;
        if (batteryLevel >= 10) return BatteryZone.LOW;
        return BatteryZone.CRITICAL;
    }

    /**
     * Форматированное оставшееся время (для UI).
     */
    public String getFormattedRemainingTime() {
        if (isCharging || remainingMinutes <= 0) {
            return "N/A";
        }
        
        int hours = (int) (remainingMinutes / 60);
        int minutes = (int) (remainingMinutes % 60);
        
        if (hours > 0) {
            return String.format("%dч %02dмин", hours, minutes);
        } else {
            return String.format("%dмин", minutes);
        }
    }

    @Override
    public String getEventType() {
        return "BatteryLevelEvent";
    }

    @Override
    public String toString() {
        String mode = isCharging ? "зарядка" : "разрядка";
        String remaining = isCharging ? "" : String.format(", осталось %s", getFormattedRemainingTime());
        
        return String.format("BatteryLevelEvent{timestamp=%s, device=%s, level=%.0f%%, mode=%s, zone=%s%s}",
                getFormattedTimestamp(),
                deviceId,
                batteryLevel,
                mode,
                getBatteryZone(),
                remaining);
    }

    /**
     * Enum зон заряда батареи (для визуальной индикации).
     */
    public enum BatteryZone {
        GOOD("Хорошо", 60, 100),       // 60-100%
        FAIR("Средне", 20, 60),        // 20-60%
        LOW("Низко", 10, 20),          // 10-20%
        CRITICAL("Критично", 0, 10);   // 0-10%

        private final String displayName;
        private final double minLevel;
        private final double maxLevel;

        BatteryZone(String displayName, double minLevel, double maxLevel) {
            this.displayName = displayName;
            this.minLevel = minLevel;
            this.maxLevel = maxLevel;
        }

        public String getDisplayName() {
            return displayName;
        }

        public double getMinLevel() {
            return minLevel;
        }

        public double getMaxLevel() {
            return maxLevel;
        }

        public boolean contains(double level) {
            return level >= minLevel && level < maxLevel;
        }

        @Override
        public String toString() {
            return displayName;
        }
    }
}