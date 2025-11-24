package pw.ns2030.event;

import pw.ns2030.model.PowerState;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

/**
 * Событие перегрузки электросети - критическое событие системы.
 * Генерируется когда общее потребление превышает максимально допустимое.
 * 
 * Содержит:
 * - Текущую и максимальную мощность
 * - Процент перегрузки
 * - Список устройств, вызвавших перегрузку
 */
public class OverloadEvent extends PowerEvent {
    private final double currentPower;     // Текущее потребление (Вт)
    private final double maxPower;         // Лимит системы (Вт)
    private final List<String> culprits;   // ID устройств-виновников

    /**
     * Полный конструктор с указанием виновников перегрузки.
     */
    public OverloadEvent(Object source, PowerState powerState, LocalDateTime timestamp,
                        double currentPower, double maxPower, List<String> culprits) {
        super(source, powerState, timestamp);
        
        if (currentPower <= 0) {
            throw new IllegalArgumentException("Текущая мощность должна быть положительной");
        }
        if (maxPower <= 0) {
            throw new IllegalArgumentException("Максимальная мощность должна быть положительной");
        }
        if (currentPower <= maxPower) {
            throw new IllegalArgumentException("Для перегрузки текущая мощность должна превышать лимит");
        }
        
        this.currentPower = currentPower;
        this.maxPower = maxPower;
        this.culprits = culprits != null ? List.copyOf(culprits) : Collections.emptyList();
    }

    /**
     * Конструктор без указания конкретных виновников.
     */
    public OverloadEvent(Object source, double currentPower, double maxPower) {
        this(source, PowerState.OFF, LocalDateTime.now(), currentPower, maxPower, Collections.emptyList());
    }

    public double getCurrentPower() {
        return currentPower;
    }

    public double getMaxPower() {
        return maxPower;
    }

    /**
     * Превышение лимита в ваттах.
     */
    public double getOverloadAmount() {
        return currentPower - maxPower;
    }

    /**
     * Процент перегрузки относительно лимита.
     */
    public double getOverloadPercent() {
        return ((currentPower - maxPower) / maxPower) * 100.0;
    }

    /**
     * Коэффициент перегрузки (1.0 = на лимите, 1.5 = перегрузка на 50%).
     */
    public double getOverloadFactor() {
        return currentPower / maxPower;
    }

    /**
     * Список ID устройств, вызвавших перегрузку (immutable).
     */
    public List<String> getCulprits() {
        return culprits;
    }

    /**
     * Проверка наличия информации о виновниках.
     */
    public boolean hasCulprits() {
        return !culprits.isEmpty();
    }

    /**
     * Уровень критичности перегрузки.
     */
    public OverloadSeverity getSeverity() {
        double percent = getOverloadPercent();
        if (percent >= 50) return OverloadSeverity.SEVERE;
        if (percent >= 20) return OverloadSeverity.HIGH;
        if (percent >= 10) return OverloadSeverity.MODERATE;
        return OverloadSeverity.LOW;
    }

    @Override
    public String getEventType() {
        return "OverloadEvent";
    }

    @Override
    public String toString() {
        return String.format("OverloadEvent{timestamp=%s, current=%.0fW, max=%.0fW, overload=+%.0fW (+%.1f%%), severity=%s, culprits=%s}",
                getFormattedTimestamp(),
                currentPower,
                maxPower,
                getOverloadAmount(),
                getOverloadPercent(),
                getSeverity(),
                culprits.isEmpty() ? "unknown" : String.join(", ", culprits));
    }

    /**
     * Enum уровня критичности перегрузки.
     */
    public enum OverloadSeverity {
        LOW("Низкая"),         // 0-10% перегрузка
        MODERATE("Умеренная"), // 10-20%
        HIGH("Высокая"),       // 20-50%
        SEVERE("Критическая"); // >50%

        private final String displayName;

        OverloadSeverity(String displayName) {
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