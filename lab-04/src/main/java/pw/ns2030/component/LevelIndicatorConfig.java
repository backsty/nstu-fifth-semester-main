package pw.ns2030.component;

/**
 * Immutable конфигурация границ зон индикатора.
 * Использует паттерн Builder с валидацией для гарантии корректности настроек.
 * 
 * Структура зон (снизу вверх):
 * - Критическая: [min, criticalLow] и [criticalHigh, max]
 * - Предупреждение: [criticalLow, warningLow] и [warningHigh, criticalHigh]
 * - Нормальная: [warningLow, warningHigh]
 */
public class LevelIndicatorConfig {
    private final double minValue;
    private final double maxValue;
    private final double criticalLow;
    private final double criticalHigh;
    private final double warningLow;
    private final double warningHigh;

    private LevelIndicatorConfig(Builder builder) {
        this.minValue = builder.minValue;
        this.maxValue = builder.maxValue;
        this.criticalLow = builder.criticalLow;
        this.criticalHigh = builder.criticalHigh;
        this.warningLow = builder.warningLow;
        this.warningHigh = builder.warningHigh;
    }

    public double getMinValue() {
        return minValue;
    }

    public double getMaxValue() {
        return maxValue;
    }

    public double getCriticalLow() {
        return criticalLow;
    }

    public double getCriticalHigh() {
        return criticalHigh;
    }

    public double getWarningLow() {
        return warningLow;
    }

    public double getWarningHigh() {
        return warningHigh;
    }

    /**
     * Вычисляет диапазон значений (max - min).
     */
    public double getRange() {
        return maxValue - minValue;
    }

    /**
     * Определяет зону для заданного значения.
     * 
     * @param value значение для проверки
     * @return зона индикатора
     */
    public LevelZone determineZone(double value) {
        if (value < criticalLow || value > criticalHigh) {
            return LevelZone.CRITICAL;
        }
        if (value < warningLow || value > warningHigh) {
            return LevelZone.WARNING;
        }
        return LevelZone.NORMAL;
    }

    /**
     * Проверяет, находится ли значение в допустимом диапазоне.
     */
    public boolean isInRange(double value) {
        return value >= minValue && value <= maxValue;
    }

    /**
     * Нормализует значение к диапазону [0, 1].
     */
    public double normalize(double value) {
        if (getRange() == 0) {
            return 0;
        }
        return (value - minValue) / getRange();
    }

    @Override
    public String toString() {
        return String.format("LevelIndicatorConfig{min=%.2f, max=%.2f, " +
                        "critical=[%.2f, %.2f], warning=[%.2f, %.2f]}",
                minValue, maxValue, criticalLow, criticalHigh, warningLow, warningHigh);
    }

    /**
     * Builder для создания объектов конфигурации с валидацией.
     */
    public static class Builder {
        private double minValue = 0.0;
        private double maxValue = 100.0;
        private double criticalLow = 10.0;
        private double criticalHigh = 90.0;
        private double warningLow = 20.0;
        private double warningHigh = 80.0;

        /**
         * Устанавливает минимальное значение диапазона.
         */
        public Builder setMinValue(double minValue) {
            this.minValue = minValue;
            return this;
        }

        /**
         * Устанавливает максимальное значение диапазона.
         */
        public Builder setMaxValue(double maxValue) {
            this.maxValue = maxValue;
            return this;
        }

        /**
         * Устанавливает нижнюю границу критической зоны.
         */
        public Builder setCriticalLow(double criticalLow) {
            this.criticalLow = criticalLow;
            return this;
        }

        /**
         * Устанавливает верхнюю границу критической зоны.
         */
        public Builder setCriticalHigh(double criticalHigh) {
            this.criticalHigh = criticalHigh;
            return this;
        }

        /**
         * Устанавливает нижнюю границу зоны предупреждения.
         */
        public Builder setWarningLow(double warningLow) {
            this.warningLow = warningLow;
            return this;
        }

        /**
         * Устанавливает верхнюю границу зоны предупреждения.
         */
        public Builder setWarningHigh(double warningHigh) {
            this.warningHigh = warningHigh;
            return this;
        }

        /**
         * Устанавливает границы критической зоны (нижняя и верхняя).
         */
        public Builder setCriticalRange(double low, double high) {
            this.criticalLow = low;
            this.criticalHigh = high;
            return this;
        }

        /**
         * Устанавливает границы зоны предупреждения (нижняя и верхняя).
         */
        public Builder setWarningRange(double low, double high) {
            this.warningLow = low;
            this.warningHigh = high;
            return this;
        }

        /**
         * Создает объект конфигурации с валидацией всех параметров.
         * 
         * @return объект конфигурации
         * @throws IllegalStateException если параметры некорректны
         */
        public LevelIndicatorConfig build() {
            validateConfiguration();
            return new LevelIndicatorConfig(this);
        }

        private void validateConfiguration() {
            if (Double.isNaN(minValue) || Double.isInfinite(minValue)) {
                throw new IllegalStateException("Минимальное значение должно быть конечным числом");
            }
            if (Double.isNaN(maxValue) || Double.isInfinite(maxValue)) {
                throw new IllegalStateException("Максимальное значение должно быть конечным числом");
            }
            if (minValue >= maxValue) {
                throw new IllegalStateException(
                        String.format("Минимальное значение (%.2f) должно быть меньше максимального (%.2f)",
                                minValue, maxValue));
            }
            if (criticalLow < minValue) {
                throw new IllegalStateException(
                        String.format("Нижняя граница критической зоны (%.2f) не может быть меньше минимума (%.2f)",
                                criticalLow, minValue));
            }
            if (criticalHigh > maxValue) {
                throw new IllegalStateException(
                        String.format("Верхняя граница критической зоны (%.2f) не может быть больше максимума (%.2f)",
                                criticalHigh, maxValue));
            }
            if (criticalLow >= warningLow) {
                throw new IllegalStateException(
                        String.format("Нижняя граница критической зоны (%.2f) должна быть меньше нижней границы предупреждения (%.2f)",
                                criticalLow, warningLow));
            }
            if (warningHigh >= criticalHigh) {
                throw new IllegalStateException(
                        String.format("Верхняя граница предупреждения (%.2f) должна быть меньше верхней границы критической зоны (%.2f)",
                                warningHigh, criticalHigh));
            }
            if (warningLow >= warningHigh) {
                throw new IllegalStateException(
                        String.format("Нижняя граница предупреждения (%.2f) должна быть меньше верхней (%.2f)",
                                warningLow, warningHigh));
            }

            double criticalLowRange = warningLow - criticalLow;
            double criticalHighRange = criticalHigh - warningHigh;
            double warningRange = warningHigh - warningLow;
            
            if (criticalLowRange <= 0 || criticalHighRange <= 0 || warningRange <= 0) {
                throw new IllegalStateException("Все зоны должны иметь ненулевую ширину");
            }
        }
    }

    /**
     * Создает конфигурацию по умолчанию с предустановленными значениями.
     */
    public static LevelIndicatorConfig createDefault() {
        return new Builder().build();
    }

    /**
     * Создает конфигурацию из диапазона с автоматическим расчетом зон.
     * Критические зоны - 10% от краев, предупреждения - 20% от краев.
     * 
     * @param min минимальное значение
     * @param max максимальное значение
     */
    public static LevelIndicatorConfig fromRange(double min, double max) {
        if (min >= max) {
            throw new IllegalArgumentException("Минимум должен быть меньше максимума");
        }
        
        double range = max - min;
        double criticalMargin = range * 0.1;
        double warningMargin = range * 0.2;
        
        return new Builder()
                .setMinValue(min)
                .setMaxValue(max)
                .setCriticalLow(min + criticalMargin)
                .setCriticalHigh(max - criticalMargin)
                .setWarningLow(min + warningMargin)
                .setWarningHigh(max - warningMargin)
                .build();
    }
}