package pw.ns2030.model;

/**
 * Класс для представления команды в турнирной таблице.
 * Хранит статистику команды и обеспечивает сортировку по турнирным показателям.
 */
public class Team implements Comparable<Team> {
    
    private final int id; // Уникальный идентификатор команды в турнире
    private String name; // Название команды
    private int totalPoints; // Общее количество очков команды в турнире
    private int matchesPlayed; // Количество проведенных матчей
    private int wins; // Количество побед
    private int draws; // Количество ничьих
    private int losses; // Количество поражений
    private int goalsFor; // Количество забитых голов
    private int goalsAgainst; // Количество пропущенных голов
    private int position; // Текущее место команды в турнирной таблице
    
    public Team(int id, String name) {
        if (id <= 0) {
            throw new IllegalArgumentException("ID команды должен быть положительным числом");
        }
        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("Название команды не может быть пустым");
        }
        
        this.id = id;
        this.name = name.trim();
        this.totalPoints = 0;
        this.matchesPlayed = 0;
        this.wins = 0;
        this.draws = 0;
        this.losses = 0;
        this.goalsFor = 0;
        this.goalsAgainst = 0;
        this.position = 0;
    }
    
    public int getId() {
        return id;
    }
    
    public String getName() {
        return name;
    }
    
    public void setName(String name) {
        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("Название команды не может быть пустым");
        }
        this.name = name.trim();
    }
    
    public int getTotalPoints() {
        return totalPoints;
    }
    
    public int getMatchesPlayed() {
        return matchesPlayed;
    }
    
    public int getWins() {
        return wins;
    }
    
    public int getDraws() {
        return draws;
    }
    
    public int getLosses() {
        return losses;
    }
    
    public int getGoalsFor() {
        return goalsFor;
    }
    
    public int getGoalsAgainst() {
        return goalsAgainst;
    }
    
    // Возвращает разность забитых и пропущенных голов
    public int getGoalDifference() {
        return goalsFor - goalsAgainst;
    }
    
    public int getPosition() {
        return position;
    }
    
    public void setPosition(int position) {
        if (position <= 0) {
            throw new IllegalArgumentException("Позиция должна быть положительным числом");
        }
        this.position = position;
    }
    
    // Добавляет результат матча к статистике команды
    public void addMatchResult(int points, int goalsFor, int goalsAgainst) {
        if (points < 0 || points > 3 || (points != 0 && points != 1 && points != 3)) {
            throw new IllegalArgumentException("Количество очков может быть только 0, 1 или 3");
        }
        if (goalsFor < 0 || goalsAgainst < 0) {
            throw new IllegalArgumentException("Количество голов не может быть отрицательным");
        }
        
        this.totalPoints += points;
        this.matchesPlayed++;
        this.goalsFor += goalsFor;
        this.goalsAgainst += goalsAgainst;
        
        if (points == 3) {
            this.wins++;
        } else if (points == 1) {
            this.draws++;
        } else {
            this.losses++;
        }
    }
    
    // Удаляет результат матча из статистики команды
    public void removeMatchResult(int points, int goalsFor, int goalsAgainst) {
        if (matchesPlayed == 0) {
            throw new IllegalArgumentException("Нет матчей для удаления");
        }
        if (this.totalPoints < points || this.goalsFor < goalsFor || this.goalsAgainst < goalsAgainst) {
            throw new IllegalArgumentException("Некорректные данные для удаления");
        }
        
        this.totalPoints -= points;
        this.matchesPlayed--;
        this.goalsFor -= goalsFor;
        this.goalsAgainst -= goalsAgainst;
        
        if (points == 3) {
            this.wins--;
        } else if (points == 1) {
            this.draws--;
        } else {
            this.losses--;
        }
    }
    
    // Сбрасывает всю статистику команды к нулевым значениям
    public void resetStatistics() {
        this.totalPoints = 0;
        this.matchesPlayed = 0;
        this.wins = 0;
        this.draws = 0;
        this.losses = 0;
        this.goalsFor = 0;
        this.goalsAgainst = 0;
        this.position = 0;
    }
    
    // Проверяет, является ли команда лидером турнира (1-е место)
    public boolean isLeader() {
        return position == 1;
    }
    
    // Возвращает процент побед команды
    public double getWinPercentage() {
        if (matchesPlayed == 0) return 0.0;
        return (wins * 100.0) / matchesPlayed;
    }
    
    // Возвращает среднее количество очков за матч
    public double getAveragePointsPerMatch() {
        if (matchesPlayed == 0) return 0.0;
        return (double) totalPoints / matchesPlayed;
    }
    
    // Возвращает краткую статистику команды для отображения
    public String getShortStatistics() {
        return String.format("И:%d В:%d Н:%d П:%d О:%d", 
                matchesPlayed, wins, draws, losses, totalPoints);
    }
    
    // Возвращает полную статистику команды для отображения
    public String getFullStatistics() {
        return String.format(
                "Место: %d | Очки: %d | Матчи: %d | Победы: %d | Ничьи: %d | Поражения: %d | " +
                "Голы: %d-%d | Разность: %s%d | Процент побед: %.1f%%",
                position, totalPoints, matchesPlayed, wins, draws, losses,
                goalsFor, goalsAgainst, 
                getGoalDifference() >= 0 ? "+" : "", getGoalDifference(),
                getWinPercentage()
        );
    }
    
    // Сравнивает команды для сортировки в турнирной таблице
    @Override
    public int compareTo(Team other) {
        // 1. Сравниваем по очкам (больше очков = выше в таблице)
        int pointsComparison = Integer.compare(other.totalPoints, this.totalPoints);
        if (pointsComparison != 0) {
            return pointsComparison;
        }
        
        // 2. Сравниваем по разности голов
        int goalDiffComparison = Integer.compare(other.getGoalDifference(), this.getGoalDifference());
        if (goalDiffComparison != 0) {
            return goalDiffComparison;
        }
        
        // 3. Сравниваем по количеству забитых голов
        int goalsForComparison = Integer.compare(other.goalsFor, this.goalsFor);
        if (goalsForComparison != 0) {
            return goalsForComparison;
        }
        
        // 4. Сравниваем по количеству побед
        int winsComparison = Integer.compare(other.wins, this.wins);
        if (winsComparison != 0) {
            return winsComparison;
        }
        
        // 5. Сравниваем по названию (алфавитный порядок)
        return this.name.compareToIgnoreCase(other.name);
    }
    
    @Override
    public String toString() {
        return String.format("%s (%d очков)", name, totalPoints);
    }
    
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        
        Team team = (Team) obj;
        return id == team.id;
    }
    
    @Override
    public int hashCode() {
        return Integer.hashCode(id);
    }
}