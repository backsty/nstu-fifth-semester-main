package pw.ns2030.controller;

import pw.ns2030.event.BatteryLevelEvent;
import pw.ns2030.listener.PowerSystemListener;
import pw.ns2030.model.Computer;
import pw.ns2030.model.PowerState;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Контроллер компьютера с ИБП - управляет моделью батареи.
 * Обновляет уровень заряда/разряда каждые 1000ms и генерирует события батареи.
 */
public class ComputerController extends ApplianceController {
    private static final int DEFAULT_UPDATE_INTERVAL = 1000;  // 1 секунда
    
    private final Computer computer;
    private final List<PowerSystemListener> batteryListeners = new CopyOnWriteArrayList<>();
    private double lastBatteryLevel = 100.0;

    /**
     * Конструктор с пользовательским интервалом обновления.
     */
    public ComputerController(Computer computer, int updateInterval) {
        super(computer, updateInterval);
        this.computer = computer;
        this.lastBatteryLevel = computer.getBatteryLevel();
    }

    /**
     * Конструктор с дефолтным интервалом (1000ms).
     */
    public ComputerController(Computer computer) {
        this(computer, DEFAULT_UPDATE_INTERVAL);
    }

    @Override
    protected void updateState(double deltaTime) {
        PowerState state = computer.getState();
        
        if (state != PowerState.OFF) {
            // Обновляем уровень батареи
            computer.updateBattery(deltaTime);
            
            double currentLevel = computer.getBatteryLevel();
            
            // Генерируем событие батареи при значительном изменении (> 1%)
            if (Math.abs(currentLevel - lastBatteryLevel) >= 1.0) {
                notifyBatteryLevelChanged(currentLevel);
                lastBatteryLevel = currentLevel;
            }
            
            // Логирование состояния батареи
            if (state == PowerState.ON_BATTERY) {
                System.out.printf("[Computer] %s: battery=%.0f%%, remaining=%.1f min%n",
                    computer.getName(),
                    currentLevel,
                    computer.getRemainingBatteryMinutes());
            } else if (computer.isCharging() && currentLevel < 100.0) {
                System.out.printf("[Computer] %s: charging=%.0f%%%n",
                    computer.getName(),
                    currentLevel);
            }
        }
    }

    @Override
    public void handlePowerLoss() {
        super.handlePowerLoss();
        
        if (computer.getState() == PowerState.ON_BATTERY) {
            System.out.println("[Computer] " + computer.getName() + 
                             " переключен на батарею (" + 
                             String.format("%.0f%%", computer.getBatteryLevel()) + ")");
            notifyBatteryLevelChanged(computer.getBatteryLevel());
        }
    }

    @Override
    public void handlePowerRestored() {
        super.handlePowerRestored();
        
        if (computer.getState() == PowerState.ON_GRID) {
            System.out.println("[Computer] " + computer.getName() + " переключен на сеть (зарядка батареи)");
            notifyBatteryLevelChanged(computer.getBatteryLevel());
        }
    }

    /**
     * Уведомление слушателей об изменении уровня батареи.
     */
    private void notifyBatteryLevelChanged(double level) {
        BatteryLevelEvent event = new BatteryLevelEvent(
            this,
            computer.getId(),
            level,
            computer.isCharging()
        );
        
        for (PowerSystemListener listener : batteryListeners) {
            try {
                listener.onBatteryLevelChanged(event);
            } catch (Exception e) {
                System.err.println("[ComputerController] Ошибка в слушателе батареи: " + e.getMessage());
            }
        }
    }

    /**
     * Добавление слушателя событий батареи.
     */
    public void addBatteryListener(PowerSystemListener listener) {
        if (listener != null) {
            batteryListeners.add(listener);
        }
    }

    public void removeBatteryListener(PowerSystemListener listener) {
        batteryListeners.remove(listener);
    }

    @Override
    protected void onStarted() {
        System.out.println("[ComputerController] Запущен контроллер для " + computer.getName());
        System.out.printf("[ComputerController] Уровень батареи: %.0f%%%n", computer.getBatteryLevel());
    }

    @Override
    protected void onStopped() {
        System.out.printf("[ComputerController] Остановлен. Уровень батареи: %.0f%%%n", 
            computer.getBatteryLevel());
    }

    public Computer getComputer() {
        return computer;
    }
}