package pw.ns2030.model;

import pw.ns2030.component.LevelZone;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Collectors;

/**
 * Thread-safe ring buffer для истории измерений индикатора.
 * Реализует throttling записи и автоматическую ротацию устаревших данных.
 * 
 * Ключевые решения:
 * - CopyOnWriteArrayList для lock-free чтения (GUI читает чаще чем пишет)
 * - Минимальный интервал записи предотвращает переполнение при быстром изменении
 * - Ring buffer через autoCleanup экономит память
 */
public class LevelDataStorage {
    private final List<LevelData> dataHistory;
    private final int maxSize;              // Лимит записей (ring buffer)
    private final boolean autoCleanup;      // Авто-удаление старых при переполнении
    private final long minIntervalMs;       // Throttling: минимальный интервал между записями
    private LocalDateTime lastAddedTime;    // Для проверки throttling

    /**
     * Конструктор с полным контролем поведения.
     */
    public LevelDataStorage(int maxSize, boolean autoCleanup, long minIntervalMs) {
        this.dataHistory = new CopyOnWriteArrayList<>();
        this.maxSize = maxSize;
        this.autoCleanup = autoCleanup;
        this.minIntervalMs = minIntervalMs;
        this.lastAddedTime = null;
    }

    /**
     * Дефолтная конфигурация: 1000 записей, авто-очистка, throttling 100ms.
     */
    public LevelDataStorage() {
        this(1000, true, 100);
    }

    /**
     * Добавление с защитой от спама (throttling).
     * @return true если добавлено, false если заблокировано throttling
     */
    public boolean addData(LevelData data) {
        if (data == null) {
            throw new IllegalArgumentException("Данные не могут быть null");
        }

        // Throttling: пропускаем слишком частые записи
        if (lastAddedTime != null && minIntervalMs > 0) {
            long timeSinceLastAdd = java.time.Duration.between(lastAddedTime, LocalDateTime.now()).toMillis();
            if (timeSinceLastAdd < minIntervalMs) {
                return false;
            }
        }

        dataHistory.add(data);
        lastAddedTime = LocalDateTime.now();

        // Ring buffer: удаляем самую старую запись при переполнении
        if (autoCleanup && maxSize > 0 && dataHistory.size() > maxSize) {
            dataHistory.remove(0);
        }

        return true;
    }

    /**
     * Весь snapshot истории (копия для безопасности).
     */
    public List<LevelData> getData() {
        return new ArrayList<>(dataHistory);
    }

    /**
     * Временной срез данных (для графиков/анализа трендов).
     */
    public List<LevelData> getData(LocalDateTime from, LocalDateTime to) {
        if (from == null || to == null) {
            throw new IllegalArgumentException("Временные метки не могут быть null");
        }
        if (from.isAfter(to)) {
            throw new IllegalArgumentException("Начальная метка должна быть раньше конечной");
        }

        return dataHistory.stream()
                .filter(data -> !data.getTimestamp().isBefore(from) && !data.getTimestamp().isAfter(to))
                .collect(Collectors.toList());
    }

    /**
     * Tail последних N записей (для отображения недавней истории).
     */
    public List<LevelData> getLastN(int count) {
        if (count <= 0) {
            throw new IllegalArgumentException("Количество должно быть положительным");
        }

        int size = dataHistory.size();
        if (count >= size) {
            return getData();
        }

        return new ArrayList<>(dataHistory.subList(size - count, size));
    }

    /**
     * Последнее измерение (текущее состояние).
     */
    public LevelData getLatest() {
        if (dataHistory.isEmpty()) {
            return null;
        }
        return dataHistory.get(dataHistory.size() - 1);
    }

    /**
     * Сброс всей истории.
     */
    public void clear() {
        dataHistory.clear();
        lastAddedTime = null;
    }

    public int size() {
        return dataHistory.size();
    }

    public boolean isEmpty() {
        return dataHistory.isEmpty();
    }

    /**
     * Агрегированная статистика по всей истории (для отчетов).
     * Однопроходный алгоритм O(n) вместо множественных stream().
     */
    public StorageStatistics getStatistics() {
        if (dataHistory.isEmpty()) {
            return new StorageStatistics(0, 0, 0, 0, 0, 0, 0);
        }

        double sum = 0;
        double min = Double.MAX_VALUE;
        double max = Double.MIN_VALUE;
        int normalCount = 0;
        int warningCount = 0;
        int criticalCount = 0;

        // Единственный проход по данным для всех метрик
        for (LevelData data : dataHistory) {
            double value = data.getValue();
            sum += value;
            min = Math.min(min, value);
            max = Math.max(max, value);

            if (data.isNormal()) normalCount++;
            else if (data.isWarning()) warningCount++;
            else if (data.isCritical()) criticalCount++;
        }

        double average = sum / dataHistory.size();

        return new StorageStatistics(
                dataHistory.size(),
                average,
                min,
                max,
                normalCount,
                warningCount,
                criticalCount
        );
    }

    /**
     * Фильтр по зоне (для анализа времени в критических состояниях).
     */
    public List<LevelData> getDataByZone(LevelZone zone) {
        if (zone == null) {
            throw new IllegalArgumentException("Зона не может быть null");
        }

        return dataHistory.stream()
                .filter(data -> data.getZone() == zone)
                .collect(Collectors.toList());
    }

    /**
     * Cleanup устаревших данных (для периодической очистки вместо ring buffer).
     * @return количество удаленных записей
     */
    public int removeOlderThan(LocalDateTime olderThan) {
        if (olderThan == null) {
            throw new IllegalArgumentException("Временная метка не может быть null");
        }

        int sizeBefore = dataHistory.size();
        dataHistory.removeIf(data -> data.getTimestamp().isBefore(olderThan));
        return sizeBefore - dataHistory.size();
    }

    /**
     * Value Object для результата статистического анализа.
     * Immutable DTO для передачи метрик.
     */
    public static class StorageStatistics {
        private final int totalCount;
        private final double average;
        private final double min;
        private final double max;
        private final int normalCount;
        private final int warningCount;
        private final int criticalCount;

        public StorageStatistics(int totalCount, double average, double min, double max,
                                int normalCount, int warningCount, int criticalCount) {
            this.totalCount = totalCount;
            this.average = average;
            this.min = min;
            this.max = max;
            this.normalCount = normalCount;
            this.warningCount = warningCount;
            this.criticalCount = criticalCount;
        }

        public int getTotalCount() {
            return totalCount;
        }

        public double getAverage() {
            return average;
        }

        public double getMin() {
            return min;
        }

        public double getMax() {
            return max;
        }

        public int getNormalCount() {
            return normalCount;
        }

        public int getWarningCount() {
            return warningCount;
        }

        public int getCriticalCount() {
            return criticalCount;
        }

        // Вычисляемые метрики (ленивое вычисление вместо хранения)
        public double getNormalPercentage() {
            return totalCount > 0 ? (normalCount * 100.0 / totalCount) : 0;
        }

        public double getWarningPercentage() {
            return totalCount > 0 ? (warningCount * 100.0 / totalCount) : 0;
        }

        public double getCriticalPercentage() {
            return totalCount > 0 ? (criticalCount * 100.0 / totalCount) : 0;
        }

        @Override
        public String toString() {
            return String.format("StorageStatistics{total=%d, avg=%.2f, min=%.2f, max=%.2f, " +
                            "normal=%d(%.1f%%), warning=%d(%.1f%%), critical=%d(%.1f%%)}",
                    totalCount, average, min, max,
                    normalCount, getNormalPercentage(),
                    warningCount, getWarningPercentage(),
                    criticalCount, getCriticalPercentage());
        }
    }
}