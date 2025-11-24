package pw.ns2030.model;

/**
 * Модель лампочки - простейшее устройство без модели поведения.
 * Мгновенное включение/выключение.
 */
public class Lamp extends Appliance {

    public Lamp(String id, String name) {
        super(id, name, 60.0); // 60 Вт
    }

    @Override
    public void turnOn() {
        if (!powerAvailable) {
            System.out.println(name + ": Невозможно включить - нет питания");
            return;
        }

        if (state == PowerState.OFF) {
            state = PowerState.ON_GRID;
            System.out.println(name + ": Включена");
        }
    }

    @Override
    public void turnOff() {
        if (state == PowerState.ON_GRID) {
            state = PowerState.OFF;
            System.out.println(name + ": Выключена");
        }
    }

    @Override
    public double getCurrentPower() {
        // Потребляет энергию только когда включена и есть питание
        return (state == PowerState.ON_GRID && powerAvailable) ? ratedPower : 0.0;
    }

    @Override
    public void onPowerLoss() {
        if (state == PowerState.ON_GRID) {
            state = PowerState.OFF;
            System.out.println(name + ": Отключение питания - выключена");
        }
    }

    @Override
    public void onPowerRestored() {
        // Лампа НЕ включается автоматически (безопасность)
        System.out.println(name + ": Питание восстановлено");
    }

    @Override
    public String toString() {
        return String.format("Lamp{id='%s', name='%s', state=%s, power=%.0fW}",
                id, name, state.getDisplayName(), getCurrentPower());
    }
}