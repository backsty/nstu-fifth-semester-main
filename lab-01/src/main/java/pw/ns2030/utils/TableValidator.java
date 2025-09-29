package pw.ns2030.utils;

import pw.ns2030.model.Tournament;
import pw.ns2030.model.Team;
import pw.ns2030.model.GameResult;

import java.util.regex.Pattern;
import java.util.List;
import java.util.ArrayList;


/**
 * Утилита для валидации данных турнирной таблицы.
 * Проверяет корректность результатов матчей, названий команд и целостность данных.
 */
public class TableValidator {
    private static final Pattern SCORE_PATTERN = Pattern.compile("^\\d+:\\d+$"); // Паттерн для валидации результата матча (формат "X:Y")
    private static final Pattern TEAM_NAME_PATTERN = Pattern.compile("^[\\p{L}\\p{N}\\s\\-_]{1,50}$"); // Паттерн для валидации названия команды
    
    public static final int MAX_TEAM_NAME_LENGTH = 50; // Максимальная длина названия команды
    public static final int MIN_TEAM_NAME_LENGTH = 1; // Минимальная длина названия команды
    public static final int MAX_SCORE = 50; // Максимальный счёт в одном матче
    
    // Результат валидации с сообщением об ошибке
    public static class ValidationResult {
        private final boolean valid; // Флаг успешности валидации
        private final String errorMessage; // Сообщение об ошибке (null если ошибки нет)
        
        public ValidationResult(boolean valid, String errorMessage) {
            this.valid = valid;
            this.errorMessage = errorMessage;
        }
        
        public ValidationResult(boolean valid) {
            this(valid, null);
        }
        
        public boolean isValid() { return valid; }
        public String getErrorMessage() { return errorMessage; }
        
        public static ValidationResult success() {
            return new ValidationResult(true);
        }
        
        public static ValidationResult error(String message) {
            return new ValidationResult(false, message);
        }
    }
    
    // Валидирует строку результата матча
    public static ValidationResult validateScoreString(String scoreString) {
        if (scoreString == null) {
            return ValidationResult.error("Результат не может быть null");
        }
        
        String trimmed = scoreString.trim();
        if (trimmed.isEmpty()) {
            return ValidationResult.success(); // Пустой результат допустим (очистка)
        }
        
        if (!SCORE_PATTERN.matcher(trimmed).matches()) {
            return ValidationResult.error("Результат должен быть в формате 'X:Y' (например: '2:1')");
        }
        
        try {
            String[] parts = trimmed.split(":");
            int homeScore = Integer.parseInt(parts[0]);
            int awayScore = Integer.parseInt(parts[1]);
            
            if (homeScore < 0 || awayScore < 0) {
                return ValidationResult.error("Счёт не может быть отрицательным");
            }
            
            if (homeScore > MAX_SCORE || awayScore > MAX_SCORE) {
                return ValidationResult.error("Счёт не может быть больше " + MAX_SCORE);
            }
            
            return ValidationResult.success();
            
        } catch (NumberFormatException e) {
            return ValidationResult.error("Некорректный формат числа в результате");
        }
    }
    
    // Валидирует название команды
    public static ValidationResult validateTeamName(String teamName) {
        if (teamName == null) {
            return ValidationResult.error("Название команды не может быть null");
        }
        
        String trimmed = teamName.trim();
        
        if (trimmed.isEmpty()) {
            return ValidationResult.error("Название команды не может быть пустым");
        }
        
        if (trimmed.length() < MIN_TEAM_NAME_LENGTH) {
            return ValidationResult.error("Название команды слишком короткое");
        }
        
        if (trimmed.length() > MAX_TEAM_NAME_LENGTH) {
            return ValidationResult.error("Название команды слишком длинное (максимум " + 
                                        MAX_TEAM_NAME_LENGTH + " символов)");
        }
        
        if (!TEAM_NAME_PATTERN.matcher(trimmed).matches()) {
            return ValidationResult.error("Название команды содержит недопустимые символы");
        }
        
        return ValidationResult.success();
    }
    
    // Валидирует уникальность названия команды в турнире
    public static ValidationResult validateTeamNameUniqueness(Tournament tournament, 
                                                            String teamName, int excludeIndex) {
        if (teamName == null || teamName.trim().isEmpty()) {
            return ValidationResult.error("Название команды не может быть пустым");
        }
        
        String trimmed = teamName.trim();
        
        for (int i = 0; i < tournament.getTeamCount(); i++) {
            if (i != excludeIndex && tournament.getTeamName(i).equalsIgnoreCase(trimmed)) {
                return ValidationResult.error("Команда с таким названием уже существует");
            }
        }
        
        return ValidationResult.success();
    }
    
    // Валидирует количество команд в турнире
    public static ValidationResult validateTeamCount(int teamCount) {
        if (teamCount < Tournament.MIN_TEAMS) {
            return ValidationResult.error("Минимальное количество команд: " + Tournament.MIN_TEAMS);
        }
        
        if (teamCount > Tournament.MAX_TEAMS) {
            return ValidationResult.error("Максимальное количество команд: " + Tournament.MAX_TEAMS);
        }
        
        return ValidationResult.success();
    }
    
    // Валидирует индексы команд для матча
    public static ValidationResult validateMatchIndices(Tournament tournament, 
                                                       int homeTeam, int awayTeam) {
        int teamCount = tournament.getTeamCount();
        
        if (homeTeam < 0 || homeTeam >= teamCount) {
            return ValidationResult.error("Некорректный индекс домашней команды");
        }
        
        if (awayTeam < 0 || awayTeam >= teamCount) {
            return ValidationResult.error("Некорректный индекс гостевой команды");
        }
        
        if (homeTeam == awayTeam) {
            return ValidationResult.error("Команда не может играть сама с собой");
        }
        
        return ValidationResult.success();
    }
    
    // Проверяет целостность данных турнира
    public static List<String> validateTournamentIntegrity(Tournament tournament) {
        List<String> issues = new ArrayList<>();
        
        // Проверяем названия команд
        for (int i = 0; i < tournament.getTeamCount(); i++) {
            ValidationResult nameResult = validateTeamName(tournament.getTeamName(i));
            if (!nameResult.isValid()) {
                issues.add("Команда " + (i + 1) + ": " + nameResult.getErrorMessage());
            }
        }
        
        // Проверяем уникальность названий
        for (int i = 0; i < tournament.getTeamCount(); i++) {
            for (int j = i + 1; j < tournament.getTeamCount(); j++) {
                if (tournament.getTeamName(i).equalsIgnoreCase(tournament.getTeamName(j))) {
                    issues.add("Дублирующиеся названия команд: '" + tournament.getTeamName(i) + "'");
                }
            }
        }
        
        // Проверяем симметричность результатов
        for (int i = 0; i < tournament.getTeamCount(); i++) {
            for (int j = 0; j < tournament.getTeamCount(); j++) {
                if (i != j) {
                    GameResult result1 = tournament.getMatchResult(i, j);
                    GameResult result2 = tournament.getMatchResult(j, i);
                    
                    if (result1.isPlayed() && result2.isPlayed()) {
                        if (result1.getHomeScore() != result2.getAwayScore() ||
                            result1.getAwayScore() != result2.getHomeScore()) {
                            issues.add(String.format("Несимметричные результаты: %s vs %s",
                                tournament.getTeamName(i), tournament.getTeamName(j)));
                        }
                    } else if (result1.isPlayed() != result2.isPlayed()) {
                        issues.add(String.format("Несимметричные статусы матчей: %s vs %s",
                            tournament.getTeamName(i), tournament.getTeamName(j)));
                    }
                }
            }
        }
        
        return issues;
    }
    
    // Проверяет, является ли строка числом
    public static boolean isNumeric(String str) {
        if (str == null || str.trim().isEmpty()) {
            return false;
        }
        try {
            Integer.parseInt(str.trim());
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }
    
    // Очищает и нормализует название команды
    public static String sanitizeTeamName(String teamName) {
        if (teamName == null) return "";
        
        return teamName.trim()
                       .replaceAll("\\s+", " ")  // Заменяем множественные пробелы одним
                       .replaceAll("[\\r\\n\\t]", ""); // Удаляем переносы строк и табы
    }
    
    // Очищает и нормализует строку результата
    public static String sanitizeScoreString(String scoreString) {
        if (scoreString == null) return "";
        
        return scoreString.trim()
                         .replaceAll("\\s", "")  // Удаляем все пробелы
                         .replaceAll("[^\\d:]", ""); // Оставляем только цифры и двоеточие
    }
    
    // Проверяет, может ли быть установлен результат матча
    public static ValidationResult canSetMatchResult(Tournament tournament, 
                                                   int homeTeam, int awayTeam) {
        ValidationResult indicesResult = validateMatchIndices(tournament, homeTeam, awayTeam);
        if (!indicesResult.isValid()) {
            return indicesResult;
        }
        
        // Todo: сюда можно добавлять доп. проверки
        
        return ValidationResult.success();
    }
}