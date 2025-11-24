package pw.ns2030.controller;

import pw.ns2030.model.Appliance;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Абстрактный базовый контроллер для электроприборов.
 * Реализует Template Method pattern для управления жизненным циклом устройства в отдельном потоке.
 * 
 * Каждое устройство работает в собственном потоке с периодическими обновлениями состояния.
 * Наследники определяют специфическую логику через hook-методы.
 */
public abstract class ApplianceController implements Runnable {
    protected final Appliance appliance;
    protected final int updateInterval;  // Интервал обновления (миллисекунды)
    protected final AtomicBoolean running = new AtomicBoolean(false);
    protected volatile Thread controllerThread;
    protected final List<ApplianceStateListener> listeners = new CopyOnWriteArrayList<>();
    
    private long lastUpdateTime;

    /**
     * Конструктор базового контроллера.
     * 
     * @param appliance управляемое устройство
     * @param updateInterval интервал обновления в миллисекундах
     */
    protected ApplianceController(Appliance appliance, int updateInterval) {
        if (appliance == null) {
            throw new IllegalArgumentException("Appliance не может быть null");
        }
        if (updateInterval < 100) {
            throw new IllegalArgumentException("Интервал обновления не может быть меньше 100ms");
        }
        
        this.appliance = appliance;
        this.updateInterval = updateInterval;
        this.lastUpdateTime = System.currentTimeMillis();
    }

    /**
     * Запуск контроллера в отдельном потоке.
     */
    public void start() {
        if (running.compareAndSet(false, true)) {
            controllerThread = new Thread(this, "Controller-" + appliance.getName());
            controllerThread.setDaemon(true);
            controllerThread.start();
            System.out.println("[Controller] " + appliance.getName() + " запущен");
        }
    }

    /**
     * Остановка контроллера и ожидание завершения потока.
     */
    public void stop() {
        if (running.compareAndSet(true, false)) {
            System.out.println("[Controller] " + appliance.getName() + " остановка...");
            
            if (controllerThread != null) {
                controllerThread.interrupt();
                try {
                    controllerThread.join(1000);  // Ждем максимум 1 секунду
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
            
            onStopped();
            System.out.println("[Controller] " + appliance.getName() + " остановлен");
        }
    }

    /**
     * Главный цикл контроллера (Template Method).
     * Определяет общий алгоритм работы, вызывая hook-методы наследников.
     */
    @Override
    public final void run() {
        onStarted();
        
        while (running.get() && !Thread.currentThread().isInterrupted()) {
            long startTime = System.currentTimeMillis();
            long deltaTime = startTime - lastUpdateTime;
            
            try {
                // Hook-метод для обновления состояния устройства
                updateState(deltaTime / 1000.0);  // Передаем дельту в секундах
                
                // Уведомление слушателей о новом состоянии
                notifyStateChanged();
                
                lastUpdateTime = startTime;
                
                // Расчет времени сна до следующего обновления
                long elapsed = System.currentTimeMillis() - startTime;
                long sleepTime = Math.max(0, updateInterval - elapsed);
                
                if (sleepTime > 0) {
                    Thread.sleep(sleepTime);
                }
                
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            } catch (Exception e) {
                System.err.println("[Controller] Ошибка в " + appliance.getName() + ": " + e.getMessage());
                e.printStackTrace();
            }
        }
        
        onStopped();
    }

    /**
     * Hook-метод: обновление состояния устройства.
     * Вызывается каждый updateInterval миллисекунд.
     * 
     * @param deltaTime время с последнего обновления (секунды)
     */
    protected abstract void updateState(double deltaTime);

    /**
     * Hook-метод: обработка отключения питания в сети.
     */
    public void handlePowerLoss() {
        appliance.onPowerLoss();
        notifyStateChanged();
    }

    /**
     * Hook-метод: обработка восстановления питания.
     */
    public void handlePowerRestored() {
        appliance.onPowerRestored();
        notifyStateChanged();
    }

    /**
     * Hook-метод: вызывается при запуске контроллера (до цикла).
     */
    protected void onStarted() {
        // По умолчанию ничего не делаем
    }

    /**
     * Hook-метод: вызывается при остановке контроллера (после цикла).
     */
    protected void onStopped() {
        // По умолчанию ничего не делаем
    }

    /**
     * Уведомление слушателей об изменении состояния.
     */
    protected void notifyStateChanged() {
        ApplianceStateEvent event = new ApplianceStateEvent(
            appliance.getId(),
            appliance.getName(),
            appliance.getState(),
            appliance.getCurrentPower()
        );
        
        for (ApplianceStateListener listener : listeners) {
            try {
                listener.onStateChanged(event);
            } catch (Exception e) {
                System.err.println("[Controller] Ошибка в слушателе: " + e.getMessage());
            }
        }
    }

    // Управление слушателями
    public void addListener(ApplianceStateListener listener) {
        if (listener != null) {
            listeners.add(listener);
        }
    }

    public void removeListener(ApplianceStateListener listener) {
        listeners.remove(listener);
    }

    // Геттеры
    public Appliance getAppliance() {
        return appliance;
    }

    public boolean isRunning() {
        return running.get();
    }

    public int getUpdateInterval() {
        return updateInterval;
    }

    /**
     * Event объект для уведомления о состоянии устройства.
     */
    public static class ApplianceStateEvent {
        private final String deviceId;
        private final String deviceName;
        private final pw.ns2030.model.PowerState state;
        private final double currentPower;
        private final long timestamp;

        public ApplianceStateEvent(String deviceId, String deviceName, 
                                  pw.ns2030.model.PowerState state, double currentPower) {
            this.deviceId = deviceId;
            this.deviceName = deviceName;
            this.state = state;
            this.currentPower = currentPower;
            this.timestamp = System.currentTimeMillis();
        }

        public String getDeviceId() { return deviceId; }
        public String getDeviceName() { return deviceName; }
        public pw.ns2030.model.PowerState getState() { return state; }
        public double getCurrentPower() { return currentPower; }
        public long getTimestamp() { return timestamp; }
    }

    /**
     * Интерфейс слушателя состояния устройства.
     */
    @FunctionalInterface
    public interface ApplianceStateListener {
        void onStateChanged(ApplianceStateEvent event);
    }
}