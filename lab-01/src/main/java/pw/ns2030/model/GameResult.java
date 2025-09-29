package pw.ns2030.model;

/**
 * Класс для представления результата матча между двумя командами.
 * Поддерживает симметричное заполнение и автоматический расчёт очков.
 */
public class GameResult {
    private int homeScore; // Количество голов домашней команды
    private int awayScore; // Количество голов гостевой команды
    private boolean isPlayed; // Флаг, указывающий был ли проведен матч
    
    /**
     * Создает новый объект результата матча без указания счёта.
     * Матч считается не проведенным.
     */
    public GameResult() {
        this.homeScore = 0;
        this.awayScore = 0;
        this.isPlayed = false;
    }
    
    /**
     * Создает новый объект результата матча с указанным счётом.
     * Матч автоматически помечается как проведенный.
     * 
     * @param homeScore количество голов домашней команды (не может быть отрицательным)
     * @param awayScore количество голов гостевой команды (не может быть отрицательным)
     */
    public GameResult(int homeScore, int awayScore) {
        if (homeScore < 0 || awayScore < 0) {
            throw new IllegalArgumentException("Счёт не может быть отрицательным");
        }
        this.homeScore = homeScore;
        this.awayScore = awayScore;
        this.isPlayed = true;
    }
    
    /**
     * Возвращает количество голов домашней команды.
     */
    public int getHomeScore() {
        return homeScore;
    }
    
    /**
     * Устанавливает количество голов домашней команды.
     * Автоматически помечает матч как проведенный.
     * 
     * @param homeScore количество голов домашней команды (не может быть отрицательным)
     */
    public void setHomeScore(int homeScore) {
        if (homeScore < 0) {
            throw new IllegalArgumentException("Счёт не может быть отрицательным");
        }
        this.homeScore = homeScore;
        this.isPlayed = true;
    }
    
    /**
     * Возвращает количество голов гостевой команды.
     */
    public int getAwayScore() {
        return awayScore;
    }
    
    /**
     * Устанавливает количество голов гостевой команды.
     * Автоматически помечает матч как проведенный.
     * 
     * @param awayScore количество голов гостевой команды (не может быть отрицательным)
     */
    public void setAwayScore(int awayScore) {
        if (awayScore < 0) {
            throw new IllegalArgumentException("Счёт не может быть отрицательным");
        }
        this.awayScore = awayScore;
        this.isPlayed = true;
    }
    
    /**
     * Проверяет, был ли проведен матч.
     */
    public boolean isPlayed() {
        return isPlayed;
    }
    
    /**
     * Устанавливает статус проведения матча.
     */
    public void setPlayed(boolean played) {
        this.isPlayed = played;
    }
    
    /**
     * Определяет количество очков для домашней команды согласно футбольным правилам.
     */
    public int getHomePoints() {
        if (!isPlayed) return 0;
        
        if (homeScore > awayScore) return 3; // Победа
        if (homeScore == awayScore) return 1; // Ничья
        return 0; // Поражение
    }
    
    /**
     * Определяет количество очков для гостевой команды согласно футбольным правилам.
     */
    public int getAwayPoints() {
        if (!isPlayed) return 0;
        
        if (awayScore > homeScore) return 3; // Победа
        if (homeScore == awayScore) return 1; // Ничья
        return 0; // Поражение
    }
    
    /**
     * Возвращает форматированную строку результата для отображения в GUI.
     * 
     * @return строка в формате "X:Y"
     */
    public String getFormattedResult() {
        if (!isPlayed) return "";
        return homeScore + ":" + awayScore;
    }
    
    /**
     * Создает обратный результат для симметричного заполнения.
     * Используется для автоматического заполнения ячейки B vs A при вводе A vs B.
     */
    public GameResult createReverseResult() {
        if (!isPlayed) {
            return new GameResult();
        }
        return new GameResult(awayScore, homeScore);
    }
    
    /**
     * Очищает результат матча, возвращая его к исходному состоянию.
     * После очистки матч считается не проведенным.
     */
    public void clear() {
        this.homeScore = 0;
        this.awayScore = 0;
        this.isPlayed = false;
    }
    
    /**
     * Устанавливает результат матча одновременно для обеих команд.
     * 
     * @param homeScore количество голов домашней команды
     * @param awayScore количество голов гостевой команды
     * @throws IllegalArgumentException если любой из параметров отрицательный
     */
    public void setResult(int homeScore, int awayScore) {
        if (homeScore < 0 || awayScore < 0) {
            throw new IllegalArgumentException("Счёт не может быть отрицательным");
        }
        this.homeScore = homeScore;
        this.awayScore = awayScore;
        this.isPlayed = true;
    }
    
    /**
     * Проверяет, является ли результат ничьей.
     */
    public boolean isDraw() {
        return isPlayed && homeScore == awayScore;
    }
    
    /**
     * Проверяет, выиграла ли домашняя команда.
     */
    public boolean isHomeWin() {
        return isPlayed && homeScore > awayScore;
    }
    
    /**
     * Проверяет, выиграла ли гостевая команда.
     */
    public boolean isAwayWin() {
        return isPlayed && awayScore > homeScore;
    }
    
    @Override
    public String toString() {
        return isPlayed ? getFormattedResult() : "Не сыгран";
    }
    
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        
        GameResult that = (GameResult) obj;
        return homeScore == that.homeScore && 
               awayScore == that.awayScore && 
               isPlayed == that.isPlayed;
    }
    
    @Override
    public int hashCode() {
        return java.util.Objects.hash(homeScore, awayScore, isPlayed);
    }
}