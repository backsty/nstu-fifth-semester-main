package pw.ns2030.task;

import pw.ns2030.controller.PowerSystemController;
import pw.ns2030.model.Appliance;
import pw.ns2030.task.TaskHelpers.*;

import java.util.ArrayList;
import java.util.List;

/**
 * Фоновая задача нагрузочного тестирования системы.
 * Мониторит потребление в течение заданного времени.
 */
public class LoadTestTask extends BackgroundTask<LoadTestTask.LoadTestResult> {
    private final PowerSystemController powerSystem;
    private final int iterations;
    private final int intervalMs;

    public LoadTestTask(PowerSystemController powerSystem, int iterations, int intervalMs) {
        this.powerSystem = powerSystem;
        this.iterations = iterations;
        this.intervalMs = intervalMs;
    }

    @Override
    protected LoadTestResult performTask() throws Exception {
        publishProgress(0, "Начало нагрузочного теста...");
        Thread.sleep(300);
        
        checkCancelled();

        LoadTestResult result = new LoadTestResult();
        result.samples = new ArrayList<>();
        
        for (int i = 0; i < iterations; i++) {
            checkCancelled();
            
            // Сбор данных
            double consumption = powerSystem.getTotalConsumption();
            int activeDevices = (int) powerSystem.getDevices().stream()
                .filter(d -> d.isOn())
                .count();
            
            LoadTestSample sample = new LoadTestSample(
                i,
                consumption,
                activeDevices,
                powerSystem.isPowerAvailable()
            );
            
            result.samples.add(sample);
            
            // Обновление статистики
            result.minConsumption = Math.min(result.minConsumption, consumption);
            result.maxConsumption = Math.max(result.maxConsumption, consumption);
            result.totalConsumption += consumption;
            
            // Обновление прогресса
            int percent = (int) ((i / (double) iterations) * 100);
            publishProgress(percent, 
                String.format("Итерация %d / %d (%.0f Вт)", i + 1, iterations, consumption));
            
            // Задержка между измерениями
            if (i < iterations - 1) {
                Thread.sleep(intervalMs);
            }
        }
        
        // Расчет финальной статистики
        result.averageConsumption = result.totalConsumption / iterations;
        result.testDuration = getElapsedTime();
        
        publishProgress(100, "Тест завершен!");
        
        return result;
    }

    /**
     * Класс для хранения одного замера.
     */
    public static class LoadTestSample {
        public final int iteration;
        public final double consumption;
        public final int activeDevices;
        public final boolean powerAvailable;
        public final long timestamp;

        public LoadTestSample(int iteration, double consumption, 
                             int activeDevices, boolean powerAvailable) {
            this.iteration = iteration;
            this.consumption = consumption;
            this.activeDevices = activeDevices;
            this.powerAvailable = powerAvailable;
            this.timestamp = System.currentTimeMillis();
        }
    }

    /**
     * Результат нагрузочного теста.
     */
    public static class LoadTestResult {
        public List<LoadTestSample> samples;
        public double minConsumption = Double.MAX_VALUE;
        public double maxConsumption = Double.MIN_VALUE;
        public double averageConsumption;
        public double totalConsumption;
        public long testDuration;

        @Override
        public String toString() {
            return String.format(
                "Нагрузочный тест:\n" +
                "  Измерений: %d\n" +
                "  Минимум: %.0f Вт\n" +
                "  Максимум: %.0f Вт\n" +
                "  Среднее: %.0f Вт\n" +
                "  Длительность: %d мс",
                samples.size(),
                minConsumption,
                maxConsumption,
                averageConsumption,
                testDuration
            );
        }
    }
}