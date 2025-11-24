package pw.ns2030.model;

/**
 * Модель чайника с автоматическим выключением при закипании.
 * Моделирует нагрев и остывание воды.
 */
public class Kettle extends Appliance {
    private static final double ROOM_TEMPERATURE = 20.0;
    private static final double BOILING_POINT = 100.0;
    
    // Скорости изменения температуры (градусов в секунду)
    private static final double HEATING_RATE = 8.0;    // Нагрев: 20°C → 100°C за ~10 секунд
    private static final double COOLING_RATE = 2.0;    // Остывание: 100°C → 20°C за ~40 секунд
    
    private volatile double temperature;

    public Kettle(String id, String name) {
        super(id, name, 2000.0);
        this.temperature = ROOM_TEMPERATURE;
    }

    @Override
    public void turnOn() {
        if (!powerAvailable) {
            System.out.println(name + ": Невозможно включить - нет питания");
            return;
        }

        if (state == PowerState.OFF || state == PowerState.COOLING) {
            state = PowerState.HEATING;
            System.out.println(name + ": Включен (нагрев)");
        }
    }

    @Override
    public void turnOff() {
        if (state == PowerState.HEATING) {
            state = PowerState.COOLING;
            System.out.println(name + ": Выключен (остывание)");
        } else if (state == PowerState.COOLING) {
            state = PowerState.OFF;
            System.out.println(name + ": Полностью выключен");
        }
    }

    @Override
    public double getCurrentPower() {
        return (state == PowerState.HEATING && powerAvailable) ? ratedPower : 0.0;
    }

    @Override
    public void onPowerLoss() {
        if (state == PowerState.HEATING) {
            state = PowerState.COOLING;
            System.out.println(name + ": Отключение питания - прекращен нагрев");
        }
    }

    @Override
    public void onPowerRestored() {
        System.out.println(name + ": Питание восстановлено");
    }

    /**
     * Обновление температуры с линейной моделью нагрева/остывания.
     * 
     * @param deltaTime время с последнего обновления (секунды)
     */
    public void updateTemperature(double deltaTime) {
        if (state == PowerState.HEATING && powerAvailable) {
            // Линейный нагрев с фиксированной скоростью
            temperature += HEATING_RATE * deltaTime;
            
            // Ограничение температурой кипения
            if (temperature >= BOILING_POINT) {
                temperature = BOILING_POINT;
                turnOff();
                System.out.println(name + ": Вода закипела! Автоотключение");
            }
            
        } else if (state == PowerState.COOLING) {
            // Линейное остывание
            temperature -= COOLING_RATE * deltaTime;
            
            // Ограничение комнатной температурой
            if (temperature <= ROOM_TEMPERATURE) {
                temperature = ROOM_TEMPERATURE;
                state = PowerState.OFF;
            }
        }
    }

    public double getTemperature() {
        return temperature;
    }

    public boolean isBoiling() {
        return temperature >= BOILING_POINT;
    }

    @Override
    public String toString() {
        return String.format("Kettle{id='%s', name='%s', temp=%.1f°C, state=%s, power=%.0fW}",
                id, name, temperature, state.getDisplayName(), getCurrentPower());
    }
}