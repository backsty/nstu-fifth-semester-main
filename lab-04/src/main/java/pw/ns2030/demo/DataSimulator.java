package pw.ns2030.demo;

import javax.swing.Timer;
import java.util.Random;

/**
 * Генератор тестовых данных для индикатора с различными режимами изменения.
 * Использует Swing Timer для периодической генерации значений в EDT потоке.
 * 
 * Поддерживает:
 * - Плавные случайные изменения (Brownian motion)
 * - Резкие скачки (spike simulation)
 * - Тенденцию к среднему значению (mean reversion)
 */
public class DataSimulator {
    private final Random random;
    private final Object lock = new Object();  // Синхронизация между EDT и методами управления
    private Timer timer;
    private double currentValue;
    private double minValue;
    private double maxValue;
    private double minChange;
    private double maxChange;
    private int updateInterval;
    private boolean smoothTransition;
    private double spikeChance;
    private DataUpdateListener listener;

    /**
     * Конструктор с настройками по умолчанию (диапазон 0-100, старт с 50).
     */
    public DataSimulator() {
        this.random = new Random();
        this.currentValue = 50.0;
        this.minValue = 0.0;
        this.maxValue = 100.0;
        this.minChange = -5.0;
        this.maxChange = 5.0;
        this.updateInterval = 1000;
        this.smoothTransition = true;
        this.spikeChance = 0.05;
    }

    /**
     * Конструктор с настраиваемым диапазоном и начальным значением.
     */
    public DataSimulator(double minValue, double maxValue, double initialValue) {
        this();
        this.minValue = minValue;
        this.maxValue = maxValue;
        this.currentValue = clamp(initialValue, minValue, maxValue);
    }

    public void setDataUpdateListener(DataUpdateListener listener) {
        synchronized (lock) {
            this.listener = listener;
        }
    }

    /**
     * Изменение частоты обновления с перезапуском таймера если симулятор активен.
     */
    public void setFrequency(int milliseconds) {
        if (milliseconds < 100) {
            throw new IllegalArgumentException("Частота должна быть не менее 100 мс");
        }
        
        synchronized (lock) {
            this.updateInterval = milliseconds;
            boolean wasRunning = isRunning();
            if (wasRunning) {
                stopTimer();
                startTimer();
            }
        }
    }

    public void setChangeRange(double minChange, double maxChange) {
        if (minChange > 0 || maxChange < 0) {
            throw new IllegalArgumentException("Минимальное изменение должно быть отрицательным, максимальное - положительным");
        }
        synchronized (lock) {
            this.minChange = minChange;
            this.maxChange = maxChange;
        }
    }

    public void setSmoothTransition(boolean smooth) {
        synchronized (lock) {
            this.smoothTransition = smooth;
        }
    }

    public void setSpikeChance(double chance) {
        if (chance < 0 || chance > 1) {
            throw new IllegalArgumentException("Вероятность должна быть от 0.0 до 1.0");
        }
        synchronized (lock) {
            this.spikeChance = chance;
        }
    }

    public void start() {
        synchronized (lock) {
            if (timer != null && timer.isRunning()) {
                return;
            }
            startTimer();
        }
    }

    public void stop() {
        synchronized (lock) {
            stopTimer();
        }
    }

    private void startTimer() {
        timer = new Timer(updateInterval, e -> generateNextValue());
        timer.start();
    }

    private void stopTimer() {
        if (timer != null) {
            timer.stop();
            timer = null;
        }
    }

    public boolean isRunning() {
        synchronized (lock) {
            return timer != null && timer.isRunning();
        }
    }

    public double getCurrentValue() {
        synchronized (lock) {
            return currentValue;
        }
    }

    public void setCurrentValue(double value) {
        synchronized (lock) {
            this.currentValue = clamp(value, minValue, maxValue);
        }
        notifyListener();
    }

    /**
     * Генерация следующего значения с применением трех стратегий:
     * 1. Spike (редкие резкие скачки)
     * 2. Smooth (плавное случайное изменение)
     * 3. Mean reversion (притяжение к середине диапазона)
     * 
     * пример генерации:
     * 
     * Время    Значение   Изменение   Объяснение
     * ────────────────────────────────────────────
     *  0:00     50.0       -            Старт
     *  0:01     53.2       +3.2         Плавное ↑
     *  0:02     51.8       -1.4         Плавное ↓
     *  0:03     48.6       -3.2         Mean reversion к 50
     *  0:04     65.3       +16.7        SPIKE! (редкий скачок)
     *  0:05     62.1       -3.2         Возврат к середине
     *  0:06     58.9       -3.2         Продолжаем к 50
     */
    private void generateNextValue() {
        double newValue;
        
        synchronized (lock) {
            double change;

            // 1. Редкие резкие скачки (имитация аварийных ситуаций)
            if (random.nextDouble() < spikeChance) {
                change = (maxValue - minValue) * (random.nextDouble() - 0.5) * 0.3;
            } else {
                // 2. Обычные изменения: плавные или резкие
                if (smoothTransition) {
                    change = minChange + (maxChange - minChange) * random.nextDouble();
                } else {
                    change = random.nextBoolean() ? maxChange : minChange;
                }
            }

            // 3. Mean reversion: притяжение к среднему значению
            double tendency = (maxValue + minValue) / 2.0;
            if (currentValue > tendency) {
                change *= 0.7;  // Замедление роста выше середины
            } else if (currentValue < tendency) {
                change *= 1.3;  // Ускорение роста ниже середины
            }

            currentValue = clamp(currentValue + change, minValue, maxValue);
            newValue = currentValue;
        }
        
        notifyListener(newValue);
    }

    /**
     * Ограничение значения границами диапазона.
     */
    private double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }

    private void notifyListener() {
        notifyListener(getCurrentValue());
    }

    /**
     * Уведомление слушателя вне synchronized блока (избегаем deadlock).
     */
    private void notifyListener(double value) {
        DataUpdateListener currentListener;
        synchronized (lock) {
            currentListener = this.listener;
        }
        
        if (currentListener != null) {
            currentListener.onDataUpdate(value);
        }
    }

    /**
     * Возврат к начальному состоянию (середина диапазона).
     */
    public void reset() {
        synchronized (lock) {
            currentValue = (minValue + maxValue) / 2.0;
        }
        notifyListener();
    }

    /**
     * Callback для получения сгенерированных значений.
     */
    @FunctionalInterface
    public interface DataUpdateListener {
        void onDataUpdate(double value);
    }
}