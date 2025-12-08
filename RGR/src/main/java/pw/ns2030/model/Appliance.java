package pw.ns2030.model;

/**
 * Абстрактный базовый класс для всех электроприборов в системе.
 * Определяет общий интерфейс управления и получения данных.
 */
public abstract class Appliance {
    protected final String id;
    protected final String name;
    protected final double ratedPower; // Номинальная мощность (Вт)
    protected volatile PowerState state;
    protected volatile boolean powerAvailable; // Наличие напряжения в сети

    /**
     * Конструктор базового устройства.
     * 
     * @param id уникальный идентификатор
     * @param name отображаемое имя
     * @param ratedPower номинальная мощность в ваттах
     */
    public Appliance(String id, String name, double ratedPower) {
        if (id == null || id.trim().isEmpty()) {
            throw new IllegalArgumentException("ID не может быть пустым");
        }
        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("Имя не может быть пустым");
        }
        if (ratedPower < 0) {
            throw new IllegalArgumentException("Мощность не может быть отрицательной");
        }

        this.id = id;
        this.name = name;
        this.ratedPower = ratedPower;
        this.state = PowerState.OFF;
        this.powerAvailable = true;
    }

    /**
     * Включить устройство (если есть питание).
     */
    public abstract void turnOn();

    /**
     * Выключить устройство.
     */
    public abstract void turnOff();

    /**
     * Получить текущее потребление энергии (Вт).
     * Зависит от состояния устройства.
     */
    public abstract double getCurrentPower();

    /**
     * Обработка отключения питания в сети.
     * Каждое устройство реагирует по-своему.
     */
    public abstract void onPowerLoss();

    /**
     * Обработка восстановления питания в сети.
     */
    public abstract void onPowerRestored();

    // Геттеры
    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public double getRatedPower() {
        return ratedPower;
    }

    public PowerState getState() {
        return state;
    }

    public boolean isPowerAvailable() {
        return powerAvailable;
    }

    /**
     * Установка доступности питания (вызывается контроллером системы).
     */
    public void setPowerAvailable(boolean powerAvailable) {
        boolean oldValue = this.powerAvailable;
        this.powerAvailable = powerAvailable;

        // Автоматическая реакция на изменение питания
        if (oldValue && !powerAvailable) {
            onPowerLoss();
        } else if (!oldValue && powerAvailable) {
            onPowerRestored();
        }
    }

    public boolean isOn() {
        return state != PowerState.OFF;
    }

    @Override
    public String toString() {
        return String.format("%s{id='%s', name='%s', power=%.0fW, state=%s}",
                getClass().getSimpleName(), id, name, getCurrentPower(), state.getDisplayName());
    }
}