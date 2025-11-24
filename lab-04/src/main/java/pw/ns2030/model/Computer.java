package pw.ns2030.model;

/**
 * Модель компьютера с источником бесперебойного питания (ИБП).
 * Автоматически переключается между сетью и батареей.
 */
public class Computer extends Appliance {
    private static final double BATTERY_CAPACITY_MINUTES = 2.0;
    private static final double BATTERY_CHARGE_MINUTES = 4.0;
    private volatile double batteryLevel;
    private volatile boolean isCharging;

    public Computer(String id, String name) {
        super(id, name, 300.0);
        this.batteryLevel = 100.0;
        this.isCharging = false;
    }

    @Override
    public void turnOn() {
        if (state == PowerState.OFF) {
            if (powerAvailable) {
                state = PowerState.ON_GRID;
                System.out.println(name + ": Включен (работа от сети)");
            } else if (batteryLevel > 0) {
                state = PowerState.ON_BATTERY;
                System.out.println(name + ": Включен (работа от батареи)");
            } else {
                System.out.println(name + ": Невозможно включить - нет питания и батарея разряжена");
            }
        }
    }

    @Override
    public void turnOff() {
        if (state != PowerState.OFF) {
            state = PowerState.OFF;
            isCharging = false;
            System.out.println(name + ": Выключен");
        }
    }

    @Override
    public double getCurrentPower() {
        return (state == PowerState.ON_GRID && powerAvailable) ? ratedPower : 0.0;
    }

    @Override
    public void onPowerLoss() {
        if (state == PowerState.ON_GRID) {
            if (batteryLevel > 0) {
                state = PowerState.ON_BATTERY;
                isCharging = false;
                System.out.println(name + ": Переключен на батарею (" + 
                                 String.format("%.0f%%", batteryLevel) + ")");
            } else {
                state = PowerState.OFF;
                System.out.println(name + ": Батарея разряжена - автовыключение");
            }
        }
    }

    @Override
    public void onPowerRestored() {
        if (state == PowerState.ON_BATTERY) {
            state = PowerState.ON_GRID;
            isCharging = true;
            System.out.println(name + ": Переключен на сеть (начинается зарядка)");
        } else if (state == PowerState.OFF && batteryLevel < 100.0) {
            isCharging = true;
            System.out.println(name + ": Зарядка батареи");
        }
    }

    /**
     * Обновление уровня батареи.
     * 
     * @param deltaTime время с последнего обновления (секунды)
     */
    public void updateBattery(double deltaTime) {
        if (state == PowerState.ON_BATTERY) {
            // Разряд батареи: 100% → 0% за BATTERY_CAPACITY_MINUTES
            double drainRate = 100.0 / (BATTERY_CAPACITY_MINUTES * 60.0);
            batteryLevel -= drainRate * deltaTime;

            if (batteryLevel <= 0) {
                batteryLevel = 0;
                state = PowerState.OFF;
                System.out.println(name + ": Батарея разряжена - автовыключение");
            }

        } else if (isCharging && powerAvailable && batteryLevel < 100.0) {
            // Зарядка батареи: 0% → 100% за BATTERY_CHARGE_MINUTES
            double chargeRate = 100.0 / (BATTERY_CHARGE_MINUTES * 60.0);
            batteryLevel += chargeRate * deltaTime;

            if (batteryLevel >= 100.0) {
                batteryLevel = 100.0;
                isCharging = false;
                System.out.println(name + ": Батарея полностью заряжена");
            }
        }
    }

    public double getBatteryLevel() {
        return batteryLevel;
    }

    public boolean isCharging() {
        return isCharging;
    }

    public boolean isOnBattery() {
        return state == PowerState.ON_BATTERY;
    }

    /**
     * Оставшееся время работы от батареи (минуты).
     */
    public double getRemainingBatteryMinutes() {
        if (state != PowerState.ON_BATTERY) {
            return 0;
        }
        return (batteryLevel / 100.0) * BATTERY_CAPACITY_MINUTES;
    }

    @Override
    public String toString() {
        return String.format("Computer{id='%s', name='%s', state=%s, battery=%.0f%%, power=%.0fW}",
                id, name, state.getDisplayName(), batteryLevel, getCurrentPower());
    }
}