package pw.ns2030.controller;

import pw.ns2030.event.ConsumptionEvent;
import pw.ns2030.event.OverloadEvent;
import pw.ns2030.event.PowerEvent;
import pw.ns2030.listener.PowerSystemListener;
import pw.ns2030.model.Appliance;
import pw.ns2030.model.PowerState;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

/**
 * Центральный контроллер системы энергопотребления.
 * Управляет всеми устройствами, мониторит общее потребление и обрабатывает перегрузки.
 * 
 * Работает в отдельном потоке с проверкой состояния системы каждые 100ms.
 */
public class PowerSystemController implements Runnable {
    private static final double MAX_POWER = 5000.0;  // Лимит системы (Вт)
    private static final int CHECK_INTERVAL = 100;   // Интервал проверки (ms)
    
    private final List<ApplianceController> controllers = new CopyOnWriteArrayList<>();
    private final List<PowerSystemListener> listeners = new CopyOnWriteArrayList<>();
    private final AtomicBoolean running = new AtomicBoolean(false);
    private volatile Thread systemThread;
    
    private volatile boolean powerAvailable = true;
    private volatile double totalConsumption = 0.0;
    private volatile double previousConsumption = 0.0;
    
    private final Object powerLock = new Object();

    /**
     * Добавление устройства в систему и запуск его контроллера.
     */
    public void addDevice(ApplianceController controller) {
        if (controller == null) {
            throw new IllegalArgumentException("Controller не может быть null");
        }
        
        synchronized (powerLock) {
            controllers.add(controller);
            
            // Подписываемся на изменения состояния устройства
            controller.addListener(event -> updateConsumption());
            
            // Запускаем контроллер устройства
            controller.start();
            
            // Уведомляем слушателей
            String deviceId = controller.getAppliance().getId();
            String deviceName = controller.getAppliance().getName();
            notifyDeviceAdded(deviceId, deviceName);
        }
        
        System.out.println("[PowerSystem] Добавлено устройство: " + controller.getAppliance().getName());
        // ✅ ИСПРАВЛЕНИЕ #5: Убран дублирующий вызов updateConsumption()
        // Система сама обнаружит изменение через checkSystemState()
    }

    /**
     * Удаление устройства из системы.
     */
    public void removeDevice(ApplianceController controller) {
        if (controller == null) {
            return;
        }
        
        synchronized (powerLock) {
            controller.stop();
            controllers.remove(controller);
            
            String deviceId = controller.getAppliance().getId();
            String deviceName = controller.getAppliance().getName();
            notifyDeviceRemoved(deviceId, deviceName);
        }
        
        System.out.println("[PowerSystem] Удалено устройство: " + controller.getAppliance().getName());
        updateConsumption();
    }

    /**
     * Запуск мониторинга системы.
     */
    public void start() {
        if (running.compareAndSet(false, true)) {
            systemThread = new Thread(this, "PowerSystemController");
            systemThread.setDaemon(true);
            systemThread.start();
            System.out.println("[PowerSystem] Система запущена");
        }
    }

    /**
     * Остановка системы и всех контроллеров устройств.
     */
    public void shutdown() {
        if (running.compareAndSet(true, false)) {
            System.out.println("[PowerSystem] Остановка системы...");
            
            // Останавливаем все контроллеры устройств
            for (ApplianceController controller : controllers) {
                controller.stop();
            }
            
            // Останавливаем поток системы
            if (systemThread != null) {
                systemThread.interrupt();
                try {
                    systemThread.join(2000);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
            
            System.out.println("[PowerSystem] Система остановлена");
        }
    }

    /**
     * Главный цикл мониторинга системы.
     */
    @Override
    public void run() {
        while (running.get() && !Thread.currentThread().isInterrupted()) {
            try {
                checkSystemState();
                Thread.sleep(CHECK_INTERVAL);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            } catch (Exception e) {
                System.err.println("[PowerSystem] Ошибка мониторинга: " + e.getMessage());
                e.printStackTrace();
            }
        }
    }

    /**
     * Проверка состояния системы (перегрузка, потребление).
     */
    private void checkSystemState() {
        double currentPower = calculateTotalPower();
        
        synchronized (powerLock) {
            previousConsumption = totalConsumption;
            totalConsumption = currentPower;
        }
        
        // Проверка перегрузки
        if (currentPower > MAX_POWER && powerAvailable) {
            handleOverload(currentPower);
        }
        
        // Уведомление об изменении потребления
        if (Math.abs(currentPower - previousConsumption) > 0.1) {
            notifyConsumptionChanged(currentPower);
        }
    }

    /**
     * Расчет общего потребления энергии.
     */
    private double calculateTotalPower() {
        return controllers.stream()
            .mapToDouble(c -> c.getAppliance().getCurrentPower())
            .sum();
    }

    /**
     * Обработка перегрузки сети.
     */
    private void handleOverload(double currentPower) {
        System.err.println("[PowerSystem] ⚠️ ПЕРЕГРУЗКА! " + 
                         String.format("%.0f Вт / %.0f Вт", currentPower, MAX_POWER));
        
        // Отключаем питание
        setPowerAvailable(false);
        
        // Определяем виновников перегрузки (устройства с максимальной мощностью)
        List<String> culprits = controllers.stream()
            .filter(c -> c.getAppliance().getCurrentPower() > 0)
            .sorted((c1, c2) -> Double.compare(
                c2.getAppliance().getCurrentPower(),
                c1.getAppliance().getCurrentPower()))
            .limit(3)
            .map(c -> c.getAppliance().getName())
            .collect(Collectors.toList());
        
        // Уведомляем слушателей
        notifyOverload(currentPower, culprits);
    }

    /**
     * Установка доступности питания в сети.
     */
    public void setPowerAvailable(boolean available) {
        boolean oldValue;
        
        synchronized (powerLock) {
            oldValue = this.powerAvailable;
            this.powerAvailable = available;
        }
        
        if (oldValue != available) {
            // Уведомляем все устройства
            for (ApplianceController controller : controllers) {
                controller.getAppliance().setPowerAvailable(available);
                
                if (available) {
                    controller.handlePowerRestored();
                } else {
                    controller.handlePowerLoss();
                }
            }
            
            // Уведомляем слушателей
            PowerState newState = available ? PowerState.ON_GRID : PowerState.OFF;
            notifyPowerStateChanged(newState);
            
            System.out.println("[PowerSystem] Питание: " + (available ? "✅ Восстановлено" : "❌ Отключено"));
        }
    }

    /**
     * Восстановление питания вручную.
     */
    public void restorePower() {
        setPowerAvailable(true);
    }

    /**
     * Обновление общего потребления.
     */
    private void updateConsumption() {
        double currentPower = calculateTotalPower();
        
        synchronized (powerLock) {
            previousConsumption = totalConsumption;
            totalConsumption = currentPower;
        }
        
        notifyConsumptionChanged(currentPower);
    }

    // Методы уведомления слушателей
    private void notifyConsumptionChanged(double currentPower) {
        int activeDevices = (int) controllers.stream()
            .filter(c -> c.getAppliance().isOn())
            .count();
        
        ConsumptionEvent event = new ConsumptionEvent(
            this,
            PowerState.ON_GRID,
            currentPower,
            previousConsumption,
            activeDevices
        );
        
        for (PowerSystemListener listener : listeners) {
            try {
                listener.onConsumptionChanged(event);
            } catch (Exception e) {
                System.err.println("[PowerSystem] Ошибка в слушателе: " + e.getMessage());
            }
        }
    }

    private void notifyOverload(double currentPower, List<String> culprits) {
        OverloadEvent event = new OverloadEvent(this, currentPower, MAX_POWER);
        
        for (PowerSystemListener listener : listeners) {
            try {
                listener.onOverload(event);
            } catch (Exception e) {
                System.err.println("[PowerSystem] Ошибка в слушателе перегрузки: " + e.getMessage());
            }
        }
    }

    private void notifyPowerStateChanged(PowerState newState) {
        PowerEvent event = new PowerEvent(this, newState) {
            @Override
            public String getEventType() {
                return "PowerStateChanged";
            }
        };
        
        for (PowerSystemListener listener : listeners) {
            try {
                listener.onPowerStateChanged(event);
            } catch (Exception e) {
                System.err.println("[PowerSystem] Ошибка в слушателе состояния: " + e.getMessage());
            }
        }
    }

    private void notifyDeviceAdded(String deviceId, String deviceName) {
        for (PowerSystemListener listener : listeners) {
            try {
                listener.onDeviceAdded(deviceId, deviceName);
            } catch (Exception e) {
                // Игнорируем ошибки в default методах
            }
        }
    }

    private void notifyDeviceRemoved(String deviceId, String deviceName) {
        for (PowerSystemListener listener : listeners) {
            try {
                listener.onDeviceRemoved(deviceId, deviceName);
            } catch (Exception e) {
                // Игнорируем ошибки в default методах
            }
        }
    }

    // Управление слушателями
    public void addListener(PowerSystemListener listener) {
        if (listener != null) {
            listeners.add(listener);
        }
    }

    public void removeListener(PowerSystemListener listener) {
        listeners.remove(listener);
    }

    // Геттеры
    public boolean isPowerAvailable() {
        return powerAvailable;
    }

    public double getTotalConsumption() {
        return totalConsumption;
    }

    public double getMaxPower() {
        return MAX_POWER;
    }

    public List<Appliance> getDevices() {
        return controllers.stream()
            .map(ApplianceController::getAppliance)
            .collect(Collectors.toList());
    }

    public int getDeviceCount() {
        return controllers.size();
    }

    public boolean isRunning() {
        return running.get();
    }
}