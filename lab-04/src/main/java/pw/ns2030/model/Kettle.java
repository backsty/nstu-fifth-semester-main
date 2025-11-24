package pw.ns2030.model;

/**
 * Модель чайника с автоматическим выключением при закипании.
 * Моделирует нагрев и остывание воды.
 */
public class Kettle extends Appliance {
    private static final double ROOM_TEMPERATURE = 20.0; // °C
    private static final double BOILING_POINT = 100.0;   // °C
    
    private volatile double temperature; // Текущая температура воды (°C)

    public Kettle(String id, String name) {
        super(id, name, 2000.0); // 2000 Вт
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
        // Потребляет энергию только при нагреве
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
        // Чайник НЕ включается автоматически (безопасность)
        System.out.println(name + ": Питание восстановлено");
    }

    /**
     * Обновление температуры (вызывается контроллером).
     * 
     * @param deltaTime время с последнего обновления (секунды)
     */
    public void updateTemperature(double deltaTime) {
        if (state == PowerState.HEATING && powerAvailable) {
            // Нагрев: экспоненциальное приближение к 100°C
            // T(t) = T_room + (T_boil - T_room) * (1 - e^(-k*t))
            double k = 0.01; // Коэффициент теплопередачи
            temperature = ROOM_TEMPERATURE + 
                         (BOILING_POINT - ROOM_TEMPERATURE) * 
                         (1 - Math.exp(-k * deltaTime));

            // Автоматическое отключение при закипании
            if (temperature >= BOILING_POINT) {
                temperature = BOILING_POINT;
                turnOff(); // Автоотключение
                System.out.println(name + ": Вода закипела! Автоотключение");
            }

        } else if (state == PowerState.COOLING) {
            // Остывание: экспоненциальное приближение к комнатной температуре
            temperature = ROOM_TEMPERATURE + 
                         (temperature - ROOM_TEMPERATURE) * 0.95;

            // Полное остывание
            if (Math.abs(temperature - ROOM_TEMPERATURE) < 0.5) {
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