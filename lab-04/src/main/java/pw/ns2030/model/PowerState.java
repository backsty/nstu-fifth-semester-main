package pw.ns2030.model;

/**
 * Состояния электроприборов в системе потребителей энергии.
 */
public enum PowerState {
    /** Выключено */
    OFF("Выключено"),
    
    /** Работа от сети */
    ON_GRID("Работа от сети"),
    
    /** Работа от батареи (для компьютера с ИБП) */
    ON_BATTERY("Работа от батареи"),
    
    /** Нагрев (для чайника) */
    HEATING("Нагрев"),
    
    /** Остывание (для чайника после выключения) */
    COOLING("Остывание");

    private final String displayName;

    PowerState(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }

    @Override
    public String toString() {
        return displayName;
    }
}