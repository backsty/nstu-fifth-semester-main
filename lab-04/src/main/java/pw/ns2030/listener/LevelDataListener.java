package pw.ns2030.listener;

/**
 * Callback интерфейс для паттерна Observer (аналог PropertyChangeListener).
 * @FunctionalInterface позволяет использовать лямбды для подписки.
 * 
 * Thread-safety: вызывается из EDT (Swing), но может быть вызван из любого потока
 * если источник событий - не GUI компонент.
 */
@FunctionalInterface
public interface LevelDataListener {
    /**
     * Вызывается при любом изменении индикатора.
     * @param event snapshot изменения с контекстом
     */
    void onLevelDataUpdate(LevelChangeEvent event);
}