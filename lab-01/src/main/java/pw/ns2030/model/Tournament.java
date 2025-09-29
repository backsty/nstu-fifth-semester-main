package pw.ns2030.model;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Класс для управления турниром и турнирной таблицей.
 * Поддерживает симметричное заполнение результатов и автоматический пересчёт очков.
 */
public class Tournament {
    public static final int MIN_TEAMS = 3; // Минимальное количество команд в турнире
    public static final int MAX_TEAMS = 20; // Максимальное количество команд в турнире
    
    private final int teamCount; // Количество команд в турнире
    private final List<Team> teams; // Список команд-участников турнира
    private final GameResult[][] results; // Двумерная матрица результатов матчей [домашняя][гостевая]
    private final List<TournamentListener> listeners; // Список слушателей изменений турнира для уведомления GUI
    private boolean needsResort; // Флаг, указывающий, что турнирная таблица требует пересортировки
    
    // Интерфейс для уведомления о изменениях в турнире
    public interface TournamentListener {
        void onMatchResultChanged(int homeTeam, int awayTeam, GameResult result);
        void onTeamNameChanged(int teamIndex, String newName);
        void onTableResorted(List<Team> sortedTeams);
    }
    
    /**
     * Создает новый турнир с указанным количеством команд.
     * Команды создаются с именами по умолчанию ("Команда 1", "Команда 2", и т.д.).
     */
    public Tournament(int teamCount) {
        if (teamCount < MIN_TEAMS || teamCount > MAX_TEAMS) {
            throw new IllegalArgumentException(
                    String.format("Количество команд должно быть от %d до %d", MIN_TEAMS, MAX_TEAMS));
        }
        
        this.teamCount = teamCount;
        this.teams = new ArrayList<>(teamCount);
        this.results = new GameResult[teamCount][teamCount];
        this.listeners = new ArrayList<>();
        this.needsResort = true;
        
        initializeTeams();
        initializeResults();
    }

    // Инициализирует команды с именами по умолчанию.
    private void initializeTeams() {
        for (int i = 0; i < teamCount; i++) {
            teams.add(new Team(i + 1, "Команда " + (i + 1)));
        }
    }

    // Инициализирует матрицу результатов пустыми значениями.
    private void initializeResults() {
        for (int i = 0; i < teamCount; i++) {
            for (int j = 0; j < teamCount; j++) {
                if (i != j) {
                    results[i][j] = new GameResult();
                }
            }
        }
    }

    public int getTeamCount() {
        return teamCount;
    }
    
    public Team getTeam(int index) {
        validateTeamIndex(index);
        return teams.get(index);
    }

    public List<Team> getTeams() {
        return Collections.unmodifiableList(teams);
    }

    public String getTeamName(int index) {
        validateTeamIndex(index);
        return teams.get(index).getName();
    }

    public void setTeamName(int index, String name) {
        validateTeamIndex(index);
        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("Название команды не может быть пустым");
        }
        
        String oldName = teams.get(index).getName();
        teams.get(index).setName(name.trim());
        
        notifyTeamNameChanged(index, name.trim());
        
        needsResort = true;
    }
    
    // Возвращает результат матча между двумя командами.
    public GameResult getMatchResult(int homeTeam, int awayTeam) {
        validateMatchIndices(homeTeam, awayTeam);
        return results[homeTeam][awayTeam];
    }
    
    /**
     * Устанавливает результат матча между двумя командами.
     * Автоматически выполняет симметричное заполнение.
     */
    public void setMatchResult(int homeTeam, int awayTeam, String resultString) {
        validateMatchIndices(homeTeam, awayTeam);
        
        if (resultString == null || resultString.trim().isEmpty()) {
            clearMatchResult(homeTeam, awayTeam);
            return;
        }
        
        // Парсим результат
        String[] parts = resultString.trim().split(":");
        if (parts.length != 2) {
            throw new IllegalArgumentException("Результат должен быть в формате 'X:Y'");
        }
        
        try {
            int homeScore = Integer.parseInt(parts[0].trim());
            int awayScore = Integer.parseInt(parts[1].trim());
            
            if (homeScore < 0 || awayScore < 0) {
                throw new IllegalArgumentException("Счёт не может быть отрицательным");
            }
            
            setMatchResult(homeTeam, awayTeam, homeScore, awayScore);
            
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Некорректный формат счёта: " + resultString);
        }
    }
    
    // Устанавливает результат матча с числовыми значениями счёта.
    public void setMatchResult(int homeTeam, int awayTeam, int homeScore, int awayScore) {
        validateMatchIndices(homeTeam, awayTeam);
        
        // Сохраняем старый результат для отката статистики
        GameResult oldResult = results[homeTeam][awayTeam];
        boolean wasPlayed = oldResult.isPlayed();
        
        // Откатываем статистику старого результата
        if (wasPlayed) {
            teams.get(homeTeam).removeMatchResult(
                    oldResult.getHomePoints(), 
                    oldResult.getHomeScore(), 
                    oldResult.getAwayScore()
            );
            teams.get(awayTeam).removeMatchResult(
                    oldResult.getAwayPoints(), 
                    oldResult.getAwayScore(), 
                    oldResult.getHomeScore()
            );
        }
        
        // Устанавливаем новый результат
        GameResult newResult = new GameResult(homeScore, awayScore);
        results[homeTeam][awayTeam] = newResult;
        
        // Симметричное заполнение
        results[awayTeam][homeTeam] = newResult.createReverseResult();
        
        // Обновляем статистику команд
        teams.get(homeTeam).addMatchResult(
                newResult.getHomePoints(), homeScore, awayScore);
        teams.get(awayTeam).addMatchResult(
                newResult.getAwayPoints(), awayScore, homeScore);
        
        // Уведомляем слушателей
        notifyMatchResultChanged(homeTeam, awayTeam, newResult);
        notifyMatchResultChanged(awayTeam, homeTeam, results[awayTeam][homeTeam]);
        
        needsResort = true;
    }
    
    // Очищает результат матча между двумя командами.
    public void clearMatchResult(int homeTeam, int awayTeam) {
        validateMatchIndices(homeTeam, awayTeam);
        
        GameResult oldResult = results[homeTeam][awayTeam];
        if (oldResult.isPlayed()) {
            // Откатываем статистику
            teams.get(homeTeam).removeMatchResult(
                    oldResult.getHomePoints(), 
                    oldResult.getHomeScore(), 
                    oldResult.getAwayScore()
            );
            teams.get(awayTeam).removeMatchResult(
                    oldResult.getAwayPoints(), 
                    oldResult.getAwayScore(), 
                    oldResult.getHomeScore()
            );
        }
        
        // Очищаем результаты
        results[homeTeam][awayTeam].clear();
        results[awayTeam][homeTeam].clear();
        
        // Уведомляем слушателей
        notifyMatchResultChanged(homeTeam, awayTeam, results[homeTeam][awayTeam]);
        notifyMatchResultChanged(awayTeam, homeTeam, results[awayTeam][homeTeam]);
        
        needsResort = true;
    }
    
    // Очищает все результаты турнира.
    public void clearAllResults() {
        for (Team team : teams) {
            team.resetStatistics();
        }

        for (int i = 0; i < teamCount; i++) {
            for (int j = 0; j < teamCount; j++) {
                if (i != j) {
                    results[i][j].clear();
                }
            }
        }

        for (int i = 0; i < teamCount; i++) {
            for (int j = 0; j < teamCount; j++) {
                if (i != j) {
                    notifyMatchResultChanged(i, j, results[i][j]);
                }
            }
        }
        
        needsResort = true;
    }
    
    // Очищает все результаты указанной команды.
    public void clearTeamResults(int teamIndex) {
        validateTeamIndex(teamIndex);
        
        for (int j = 0; j < teamCount; j++) {
            if (j != teamIndex) {
                clearMatchResult(teamIndex, j);
            }
        }
    }
    
    /**
     * Возвращает отсортированную турнирную таблицу.
     * Команды сортируются по турнирным показателям.
     */
    public List<Team> getSortedTable() {
        if (needsResort) {
            resortTable();
        }
        
        return teams.stream()
                .sorted()
                .collect(Collectors.toList());
    }
    
    // Пересортировывает турнирную таблицу и обновляет места команд.
    private void resortTable() {
        List<Team> sortedTeams = teams.stream()
                .sorted()
                .collect(Collectors.toList());
        
        for (int i = 0; i < sortedTeams.size(); i++) {
            sortedTeams.get(i).setPosition(i + 1);
        }

        notifyTableResorted(sortedTeams);
        
        needsResort = false;
    }

    public Team getLeader() {
        return getSortedTable().get(0);
    }

    // Возвращает общее количество проведенных матчей в турнире.
    public int getTotalMatchesPlayed() {
        int count = 0;
        for (int i = 0; i < teamCount; i++) {
            for (int j = i + 1; j < teamCount; j++) {
                if (results[i][j].isPlayed()) {
                    count++;
                }
            }
        }
        return count;
    }
    
    public int getTotalMatches() {
        return (teamCount * (teamCount - 1)) / 2;
    }

    public boolean isComplete() {
        return getTotalMatchesPlayed() == getTotalMatches();
    }

    // Возвращает процент завершенности турнира.
    public double getCompletionPercentage() {
        return (getTotalMatchesPlayed() * 100.0) / getTotalMatches();
    }

    // Добавляет слушателя изменений турнира.
    public void addTournamentListener(TournamentListener listener) {
        if (listener != null && !listeners.contains(listener)) {
            listeners.add(listener);
        }
    }

    // Удаляет слушателя изменений турнира.
    public void removeTournamentListener(TournamentListener listener) {
        listeners.remove(listener);
    }
    
    // Проверяет корректность индекса команды.
    private void validateTeamIndex(int index) {
        if (index < 0 || index >= teamCount) {
            throw new IndexOutOfBoundsException(
                    String.format("Индекс команды должен быть от 0 до %d", teamCount - 1));
        }
    }

    // Проверяет корректность индексов для матча.
    private void validateMatchIndices(int homeTeam, int awayTeam) {
        validateTeamIndex(homeTeam);
        validateTeamIndex(awayTeam);
        
        if (homeTeam == awayTeam) {
            throw new IllegalArgumentException("Команда не может играть сама с собой");
        }
    }

    // Уведомляет слушателей об изменении результата матча.
    private void notifyMatchResultChanged(int homeTeam, int awayTeam, GameResult result) {
        for (TournamentListener listener : listeners) {
            try {
                listener.onMatchResultChanged(homeTeam, awayTeam, result);
            } catch (Exception e) {
                System.err.println("Ошибка в слушателе изменений матча: " + e.getMessage());
            }
        }
    }

    // Уведомляет слушателей об изменении названия команды.
    private void notifyTeamNameChanged(int teamIndex, String newName) {
        for (TournamentListener listener : listeners) {
            try {
                listener.onTeamNameChanged(teamIndex, newName);
            } catch (Exception e) {
                System.err.println("Ошибка в слушателе изменений названия: " + e.getMessage());
            }
        }
    }

    // Уведомляет слушателей о пересортировке таблицы.
    private void notifyTableResorted(List<Team> sortedTeams) {
        for (TournamentListener listener : listeners) {
            try {
                listener.onTableResorted(sortedTeams);
            } catch (Exception e) {
                System.err.println("Ошибка в слушателе пересортировки: " + e.getMessage());
            }
        }
    }
    
    @Override
    public String toString() {
        return String.format("Турнир: %d команд, %d/%d матчей проведено (%.1f%%)",
                teamCount, getTotalMatchesPlayed(), getTotalMatches(), getCompletionPercentage());
    }
}
