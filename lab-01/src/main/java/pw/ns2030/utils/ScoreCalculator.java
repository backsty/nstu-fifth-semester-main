package pw.ns2030.utils;

import pw.ns2030.model.Team;
import pw.ns2030.model.Tournament;
import pw.ns2030.model.GameResult;

import java.util.List;
import java.util.Map;
import java.util.HashMap;

/**
 * Утилита для расчёта очков и статистики турнирной таблицы.
 * Предоставляет методы для вычисления турнирных показателей и формирования статистики.
 */
public class ScoreCalculator {
    public static final int POINTS_FOR_WIN = 3; // Очки за победу согласно футбольным правилам
    public static final int POINTS_FOR_DRAW = 1; // Очки за ничью согласно футбольным правилам
    public static final int POINTS_FOR_LOSS = 0; // Очки за поражение согласно футбольным правилам
    
    // Вычисляет количество очков для команды на основе результата матча
    public static int calculatePoints(int goalsFor, int goalsAgainst) {
        if (goalsFor > goalsAgainst) return POINTS_FOR_WIN;
        if (goalsFor == goalsAgainst) return POINTS_FOR_DRAW;
        return POINTS_FOR_LOSS;
    }
    
    // Вычисляет разность голов команды
    public static int calculateGoalDifference(int goalsFor, int goalsAgainst) {
        return goalsFor - goalsAgainst;
    }
    
    // Вычисляет процент побед команды
    public static double calculateWinPercentage(int wins, int totalMatches) {
        if (totalMatches == 0) return 0.0;
        return (wins * 100.0) / totalMatches;
    }
    
    // Вычисляет среднее количество очков за матч
    public static double calculateAveragePoints(int totalPoints, int matchesPlayed) {
        if (matchesPlayed == 0) return 0.0;
        return (double) totalPoints / matchesPlayed;
    }
    
    // Вычисляет среднее количество голов за матч
    public static double calculateAverageGoals(int totalGoals, int matchesPlayed) {
        if (matchesPlayed == 0) return 0.0;
        return (double) totalGoals / matchesPlayed;
    }
    
    // Определяет место команды в турнирной таблице на основе сортировки
    public static int calculatePosition(Team team, List<Team> sortedTeams) {
        for (int i = 0; i < sortedTeams.size(); i++) {
            if (sortedTeams.get(i).getId() == team.getId()) {
                return i + 1;
            }
        }
        return sortedTeams.size(); // Если не найдена, возвращаем последнее место
    }
    
    // Вычисляет общую статистику турнира
    public static Map<String, Object> calculateTournamentStatistics(Tournament tournament) {
        Map<String, Object> stats = new HashMap<>();
        
        int totalMatches = tournament.getTotalMatches();
        int playedMatches = tournament.getTotalMatchesPlayed();
        double completion = tournament.getCompletionPercentage();
        
        stats.put("totalTeams", tournament.getTeamCount());
        stats.put("totalMatches", totalMatches);
        stats.put("playedMatches", playedMatches);
        stats.put("remainingMatches", totalMatches - playedMatches);
        stats.put("completionPercentage", completion);
        
        // Статистика команд
        List<Team> teams = tournament.getTeams();
        int totalGoals = teams.stream().mapToInt(Team::getGoalsFor).sum();
        int totalPoints = teams.stream().mapToInt(Team::getTotalPoints).sum();
        
        stats.put("totalGoals", totalGoals);
        stats.put("totalPoints", totalPoints);
        stats.put("averageGoalsPerMatch", playedMatches > 0 ? (double) totalGoals / playedMatches : 0.0);
        
        // Лидер турнира
        if (!teams.isEmpty()) {
            Team leader = tournament.getLeader();
            stats.put("leader", leader.getName());
            stats.put("leaderPoints", leader.getTotalPoints());
        }
        
        return stats;
    }
    
    // Вычисляет максимальное количество очков, которое команда может набрать
    public static int calculateMaxPossiblePoints(Tournament tournament, int teamIndex) {
        Team team = tournament.getTeam(teamIndex);
        int currentPoints = team.getTotalPoints();
        int remainingMatches = 0;
        
        // Подсчитываем количество оставшихся матчей
        for (int i = 0; i < tournament.getTeamCount(); i++) {
            if (i != teamIndex && !tournament.getMatchResult(teamIndex, i).isPlayed()) {
                remainingMatches++;
            }
        }
        
        return currentPoints + (remainingMatches * POINTS_FOR_WIN);
    }
    
    // Проверяет, может ли команда теоретически занять первое место
    public static boolean canBecomeLeader(Tournament tournament, int teamIndex) {
        int maxPoints = calculateMaxPossiblePoints(tournament, teamIndex);
        Team currentLeader = tournament.getLeader();
        
        // Если это уже лидер
        if (currentLeader.getId() == tournament.getTeam(teamIndex).getId()) {
            return true;
        }
        
        return maxPoints >= currentLeader.getTotalPoints();
    }
    
    // Форматирует статистику команды для отображения
    public static String formatTeamStatistics(Team team) {
        return String.format(
            "%s: %d очков (И:%d В:%d Н:%d П:%d, Голы:%d-%d, +/-%d)",
            team.getName(),
            team.getTotalPoints(),
            team.getMatchesPlayed(),
            team.getWins(),
            team.getDraws(),
            team.getLosses(),
            team.getGoalsFor(),
            team.getGoalsAgainst(),
            team.getGoalDifference()
        );
    }
    
    // Форматирует краткую статистику турнира
    public static String formatTournamentSummary(Tournament tournament) {
        return String.format(
            "Турнир: %d команд | Матчи: %d/%d (%.1f%%) | Лидер: %s (%d очков)",
            tournament.getTeamCount(),
            tournament.getTotalMatchesPlayed(),
            tournament.getTotalMatches(),
            tournament.getCompletionPercentage(),
            tournament.getLeader().getName(),
            tournament.getLeader().getTotalPoints()
        );
    }
    
    // Проверяет корректность результата матча
    public static boolean isValidResult(GameResult result) {
        if (!result.isPlayed()) return true;
        return result.getHomeScore() >= 0 && result.getAwayScore() >= 0;
    }
    
    // Вычисляет эффективность команды (очки на матч)
    public static double calculateEfficiency(Team team) {
        return calculateAveragePoints(team.getTotalPoints(), team.getMatchesPlayed());
    }
}