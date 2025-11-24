package pw.ns2030.controller;

import pw.ns2030.model.Kettle;
import pw.ns2030.model.PowerState;

/**
 * Контроллер чайника с моделью нагрева и остывания воды.
 * Обновляет температуру каждые 500ms и отслеживает автоотключение при 100°C.
 */
public class KettleController extends ApplianceController {
    private static final int DEFAULT_UPDATE_INTERVAL = 500;  // 500ms = 2 обновления в секунду
    
    private final Kettle kettle;
    private double accumulatedTime = 0.0;  // Накопленное время нагрева (секунды)

    /**
     * Конструктор с пользовательским интервалом обновления.
     */
    public KettleController(Kettle kettle, int updateInterval) {
        super(kettle, updateInterval);
        this.kettle = kettle;
    }

    /**
     * Конструктор с дефолтным интервалом (500ms).
     */
    public KettleController(Kettle kettle) {
        this(kettle, DEFAULT_UPDATE_INTERVAL);
    }

    @Override
    protected void updateState(double deltaTime) {
        PowerState state = kettle.getState();
        
        if (state == PowerState.HEATING || state == PowerState.COOLING) {
            // Обновляем температуру чайника
            accumulatedTime += deltaTime;
            kettle.updateTemperature(accumulatedTime);
            
            // Логирование каждые 5 секунд
            if (accumulatedTime >= 5.0) {
                System.out.printf("[Kettle] %s: temp=%.1f°C, state=%s%n",
                    kettle.getName(),
                    kettle.getTemperature(),
                    state.getDisplayName());
                accumulatedTime = 0.0;
            }
            
            // Проверка автоотключения при закипании
            if (kettle.isBoiling() && state == PowerState.HEATING) {
                System.out.println("[Kettle] " + kettle.getName() + " достиг 100°C - автоотключение!");
                notifyStateChanged();
            }
        } else {
            accumulatedTime = 0.0;
        }
    }

    @Override
    protected void onStarted() {
        System.out.println("[KettleController] Запущен контроллер для " + kettle.getName());
        System.out.printf("[KettleController] Начальная температура: %.1f°C%n", kettle.getTemperature());
    }

    @Override
    protected void onStopped() {
        System.out.printf("[KettleController] Остановлен. Финальная температура: %.1f°C%n", 
            kettle.getTemperature());
    }

    public Kettle getKettle() {
        return kettle;
    }
}