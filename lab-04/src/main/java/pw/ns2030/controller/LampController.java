package pw.ns2030.controller;

import pw.ns2030.model.Lamp;

/**
 * Контроллер лампочки - простейший контроллер без модели поведения.
 * Лампа не требует периодического обновления состояния, только реагирует на команды.
 */
public class LampController extends ApplianceController {
    private static final int DEFAULT_UPDATE_INTERVAL = 1000;  // 1 секунда (для отслеживания состояния)
    
    private final Lamp lamp;

    /**
     * Конструктор с пользовательским интервалом обновления.
     */
    public LampController(Lamp lamp, int updateInterval) {
        super(lamp, updateInterval);
        this.lamp = lamp;
    }

    /**
     * Конструктор с дефолтным интервалом (1000ms).
     */
    public LampController(Lamp lamp) {
        this(lamp, DEFAULT_UPDATE_INTERVAL);
    }

    @Override
    protected void updateState(double deltaTime) {
        // Лампа не имеет изменяющегося состояния - просто мониторим
        // В будущем можно добавить счетчик наработки часов
    }

    @Override
    protected void onStarted() {
        System.out.println("[LampController] Запущен контроллер для " + lamp.getName());
    }

    @Override
    protected void onStopped() {
        System.out.println("[LampController] Остановлен контроллер для " + lamp.getName());
    }

    public Lamp getLamp() {
        return lamp;
    }
}