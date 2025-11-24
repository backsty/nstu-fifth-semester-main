package pw.ns2030.listener;

import pw.ns2030.event.BatteryLevelEvent;
import pw.ns2030.event.ConsumptionEvent;
import pw.ns2030.event.OverloadEvent;
import pw.ns2030.event.PowerEvent;

/**
 * Интерфейс слушателя событий системы энергопотребления.
 * Реализует паттерн Observer для взаимодействия между контроллерами и GUI.
 * 
 * Используется для:
 * - Уведомления GUI об изменениях в системе
 * - Логирования событий
 * - Интеграции с внешними системами мониторинга
 * 
 * Thread-safety: методы могут вызываться из разных потоков (контроллеры устройств),
 * реализации должны быть потокобезопасными (например, через SwingUtilities.invokeLater).
 */
public interface PowerSystemListener {
    
    /**
     * Вызывается при изменении общего энергопотребления системы.
     * Генерируется PowerSystemController при включении/выключении устройств
     * или изменении их режима работы.
     * 
     * @param event событие с информацией о текущем и предыдущем потреблении
     */
    void onConsumptionChanged(ConsumptionEvent event);
    
    /**
     * Вызывается при превышении лимита мощности (перегрузка сети).
     * Критическое событие - система автоматически отключает питание.
     * 
     * @param event событие с информацией о превышении и виновниках
     */
    void onOverload(OverloadEvent event);
    
    /**
     * Вызывается при изменении состояния питания в сети.
     * Включает:
     * - Отключение питания (из-за перегрузки или вручную)
     * - Восстановление питания
     * 
     * @param event событие с новым состоянием системы
     */
    void onPowerStateChanged(PowerEvent event);
    
    /**
     * Вызывается при изменении уровня батареи компьютера с ИБП.
     * Позволяет отслеживать:
     * - Разряд при работе от батареи
     * - Зарядку при восстановлении питания
     * - Критически низкий заряд
     * 
     * @param event событие с информацией об уровне батареи и устройстве
     */
    void onBatteryLevelChanged(BatteryLevelEvent event);
    
    /**
     * Вызывается при добавлении нового устройства в систему.
     * Позволяет GUI обновить список подключенных устройств.
     * 
     * @param deviceId ID добавленного устройства
     * @param deviceName отображаемое имя устройства
     */
    default void onDeviceAdded(String deviceId, String deviceName) {
        // По умолчанию ничего не делаем (optional метод)
    }
    
    /**
     * Вызывается при удалении устройства из системы.
     * 
     * @param deviceId ID удаленного устройства
     * @param deviceName отображаемое имя устройства
     */
    default void onDeviceRemoved(String deviceId, String deviceName) {
        // По умолчанию ничего не делаем (optional метод)
    }
    
    /**
     * Вызывается при изменении состояния устройства (вкл/выкл/переключение режима).
     * 
     * @param deviceId ID устройства
     * @param oldState предыдущее состояние
     * @param newState новое состояние
     */
    default void onDeviceStateChanged(String deviceId, String oldState, String newState) {
        // По умолчанию ничего не делаем (optional метод)
    }
}