package pw.ns2030.ui;

import pw.ns2030.Main;
import pw.ns2030.model.Tournament;
import pw.ns2030.model.Team;
import pw.ns2030.utils.ScoreCalculator;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

/**
 * Контекстное меню для турнирной таблицы.
 * Предоставляет операции очистки, редактирования и просмотра статистики команд.
 */
public class TournamentContextMenu extends JPopupMenu {
    
    private final Tournament tournament; // Турнир для работы с данными
    private int homeTeamIndex = -1; // Индекс домашней команды
    private int awayTeamIndex = -1; // Индекс гостевой команды
    private boolean isTeamHeader = false; // Флаг клика по заголовку команды
    
    private JMenuItem clearCellItem; // Пункт очистки ячейки
    private JMenuItem clearTeamItem; // Пункт очистки результатов команды
    private JMenuItem clearAllItem; // Пункт очистки всей таблицы
    private JMenuItem teamStatsItem; // Пункт статистики команды
    private JMenuItem editTeamNameItem; // Пункт редактирования названия
    private JSeparator separator; // Разделитель меню
    
    public TournamentContextMenu(Tournament tournament) {
        this.tournament = tournament;
        initializeMenuItems();
        setupEventHandlers();
    }
    
    // Инициализирует элементы меню
    private void initializeMenuItems() {
        clearCellItem = new JMenuItem("Очистить ячейку", Main.createIcon("clear-circle", 16));
        clearCellItem.setToolTipText("Очистить результат матча в выбранной ячейке");
        
        clearTeamItem = new JMenuItem("Очистить результаты команды", Main.createIcon("clear-circle", 16));
        clearTeamItem.setToolTipText("Очистить все результаты выбранной команды");
        
        separator = new JSeparator();
        
        teamStatsItem = new JMenuItem("Статистика команды", Main.createIcon("statistics", 16));
        teamStatsItem.setToolTipText("Показать подробную статистику команды");
        
        editTeamNameItem = new JMenuItem("Изменить название", Main.createIcon("football", 16));
        editTeamNameItem.setToolTipText("Изменить название команды");
        
        clearAllItem = new JMenuItem("Очистить всю таблицу", Main.createIcon("clear-circle", 16));
        clearAllItem.setToolTipText("Очистить все результаты турнира");
        
        add(clearCellItem);
        add(clearTeamItem);
        add(separator);
        add(teamStatsItem);
        add(editTeamNameItem);
        addSeparator();
        add(clearAllItem);
    }
    
    // Настраивает обработчики событий для пунктов меню
    private void setupEventHandlers() {
        clearCellItem.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                clearCell();
            }
        });
        
        clearTeamItem.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                clearTeamResults();
            }
        });
        
        teamStatsItem.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                showTeamStatistics();
            }
        });
        
        editTeamNameItem.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                editTeamName();
            }
        });
        
        clearAllItem.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                clearAllResults();
            }
        });
    }
    
    // Показывает меню для ячейки результата матча
    public void showForMatchCell(int homeTeam, int awayTeam, int x, int y, java.awt.Component invoker) {
        this.homeTeamIndex = homeTeam;
        this.awayTeamIndex = awayTeam;
        this.isTeamHeader = false;
        
        updateMenuForMatchCell();
        show(invoker, x, y);
    }
    
    // Показывает меню для заголовка команды
    public void showForTeamHeader(int teamIndex, int x, int y, java.awt.Component invoker) {
        this.homeTeamIndex = teamIndex;
        this.awayTeamIndex = -1;
        this.isTeamHeader = true;
        
        updateMenuForTeamHeader();
        show(invoker, x, y);
    }
    
    // Обновляет состояние меню для ячейки матча
    private void updateMenuForMatchCell() {
        String homeTeamName = tournament.getTeamName(homeTeamIndex);
        String awayTeamName = tournament.getTeamName(awayTeamIndex);
        
        clearCellItem.setText(String.format("Очистить результат: %s vs %s", 
                                          homeTeamName, awayTeamName));
        clearTeamItem.setText(String.format("Очистить результаты: %s", homeTeamName));
        teamStatsItem.setText(String.format("Статистика: %s", homeTeamName));
        editTeamNameItem.setText(String.format("Переименовать: %s", homeTeamName));
        
        clearCellItem.setVisible(true);
        clearTeamItem.setVisible(true);
        separator.setVisible(true);
        teamStatsItem.setVisible(true);
        editTeamNameItem.setVisible(true);
        
        boolean hasResult = tournament.getMatchResult(homeTeamIndex, awayTeamIndex).isPlayed();
        clearCellItem.setEnabled(hasResult);
        
        if (!hasResult) {
            clearCellItem.setToolTipText("Результат матча не задан");
        } else {
            clearCellItem.setToolTipText("Очистить результат матча");
        }
    }
    
    // Обновляет состояние меню для заголовка команды
    private void updateMenuForTeamHeader() {
        String teamName = tournament.getTeamName(homeTeamIndex);
        
        clearTeamItem.setText(String.format("Очистить результаты: %s", teamName));
        teamStatsItem.setText(String.format("Статистика: %s", teamName));
        editTeamNameItem.setText(String.format("Переименовать: %s", teamName));
        
        clearCellItem.setVisible(false);
        clearTeamItem.setVisible(true);
        separator.setVisible(true);
        teamStatsItem.setVisible(true);
        editTeamNameItem.setVisible(true);
        
        Team team = tournament.getTeam(homeTeamIndex);
        boolean hasResults = team.getMatchesPlayed() > 0;
        clearTeamItem.setEnabled(hasResults);
        
        if (!hasResults) {
            clearTeamItem.setToolTipText("У команды нет результатов матчей");
        } else {
            clearTeamItem.setToolTipText("Очистить все результаты команды");
        }
    }
    
    // Очищает результат в выбранной ячейке
    private void clearCell() {
        if (homeTeamIndex == -1 || awayTeamIndex == -1) return;
        
        try {
            String homeTeamName = tournament.getTeamName(homeTeamIndex);
            String awayTeamName = tournament.getTeamName(awayTeamIndex);
            
            int result = JOptionPane.showConfirmDialog(
                    this,
                    String.format("Очистить результат матча\n%s vs %s?", homeTeamName, awayTeamName),
                    "Подтверждение",
                    JOptionPane.YES_NO_OPTION,
                    JOptionPane.QUESTION_MESSAGE
            );
            
            if (result == JOptionPane.YES_OPTION) {
                tournament.clearMatchResult(homeTeamIndex, awayTeamIndex);
            }
            
        } catch (Exception e) {
            showError("Ошибка очистки ячейки", e.getMessage());
        }
    }
    
    // Очищает все результаты выбранной команды
    private void clearTeamResults() {
        if (homeTeamIndex == -1) return;
        
        try {
            String teamName = tournament.getTeamName(homeTeamIndex);
            Team team = tournament.getTeam(homeTeamIndex);
            
            if (team.getMatchesPlayed() == 0) {
                JOptionPane.showMessageDialog(
                        this,
                        "У команды \"" + teamName + "\" нет результатов для очистки.",
                        "Информация",
                        JOptionPane.INFORMATION_MESSAGE
                );
                return;
            }
            
            int result = JOptionPane.showConfirmDialog(
                    this,
                    String.format("Очистить все результаты команды \"%s\"?\n" +
                                "Будет очищено матчей: %d", teamName, team.getMatchesPlayed()),
                    "Подтверждение",
                    JOptionPane.YES_NO_OPTION,
                    JOptionPane.WARNING_MESSAGE
            );
            
            if (result == JOptionPane.YES_OPTION) {
                tournament.clearTeamResults(homeTeamIndex);
            }
            
        } catch (Exception e) {
            showError("Ошибка очистки результатов команды", e.getMessage());
        }
    }
    
    // Очищает все результаты турнира
    private void clearAllResults() {
        try {
            int totalMatches = tournament.getTotalMatchesPlayed();
            
            if (totalMatches == 0) {
                JOptionPane.showMessageDialog(
                        this,
                        "В турнире нет результатов для очистки.",
                        "Информация",
                        JOptionPane.INFORMATION_MESSAGE
                );
                return;
            }
            
            int result = JOptionPane.showConfirmDialog(
                    this,
                    String.format("Очистить все результаты турнира?\n" +
                                "Будет очищено матчей: %d\n" +
                                "Это действие нельзя отменить!", totalMatches),
                    "Подтверждение",
                    JOptionPane.YES_NO_OPTION,
                    JOptionPane.WARNING_MESSAGE
            );
            
            if (result == JOptionPane.YES_OPTION) {
                tournament.clearAllResults();
            }
            
        } catch (Exception e) {
            showError("Ошибка очистки турнира", e.getMessage());
        }
    }
    
    // Показывает подробную статистику команды
    private void showTeamStatistics() {
        if (homeTeamIndex == -1) return;
        
        try {
            Team team = tournament.getTeam(homeTeamIndex);
            
            StringBuilder stats = new StringBuilder();
            stats.append("📊 СТАТИСТИКА КОМАНДЫ\n\n");
            stats.append(String.format("Команда: %s\n", team.getName()));
            stats.append(String.format("Место в таблице: %d\n\n", team.getPosition()));
            
            stats.append("📈 ОСНОВНЫЕ ПОКАЗАТЕЛИ:\n");
            stats.append(String.format("Очки: %d\n", team.getTotalPoints()));
            stats.append(String.format("Матчи: %d\n", team.getMatchesPlayed()));
            stats.append(String.format("Победы: %d\n", team.getWins()));
            stats.append(String.format("Ничьи: %d\n", team.getDraws()));
            stats.append(String.format("Поражения: %d\n\n", team.getLosses()));
            
            stats.append("⚽ ГОЛЫ:\n");
            stats.append(String.format("Забито: %d\n", team.getGoalsFor()));
            stats.append(String.format("Пропущено: %d\n", team.getGoalsAgainst()));
            stats.append(String.format("Разность: %+d\n\n", team.getGoalDifference()));
            
            stats.append("📊 ДОПОЛНИТЕЛЬНО:\n");
            stats.append(String.format("Процент побед: %.1f%%\n", team.getWinPercentage()));
            stats.append(String.format("Очков за матч: %.2f\n", team.getAveragePointsPerMatch()));
            
            if (team.getMatchesPlayed() > 0) {
                stats.append(String.format("Голов за матч: %.2f\n", 
                        (double) team.getGoalsFor() / team.getMatchesPlayed()));
            }
            
            if (team.isLeader()) {
                stats.append("\n🏆 ЛИДЕР ТУРНИРА!");
            }
            
            if (ScoreCalculator.canBecomeLeader(tournament, homeTeamIndex)) {
                int maxPoints = ScoreCalculator.calculateMaxPossiblePoints(tournament, homeTeamIndex);
                stats.append(String.format("\n💪 Может набрать максимум: %d очков", maxPoints));
            }
            
            JOptionPane.showMessageDialog(
                    this,
                    stats.toString(),
                    "Статистика: " + team.getName(),
                    JOptionPane.INFORMATION_MESSAGE,
                    Main.createIcon("statistics", 32)
            );
            
        } catch (Exception e) {
            showError("Ошибка отображения статистики", e.getMessage());
        }
    }
    
    // Открывает диалог редактирования названия команды
    private void editTeamName() {
        if (homeTeamIndex == -1) return;
        
        try {
            String currentName = tournament.getTeamName(homeTeamIndex);
            
            String newName = (String) JOptionPane.showInputDialog(
                    this,
                    "Введите новое название команды:",
                    "Изменение названия",
                    JOptionPane.QUESTION_MESSAGE,
                    Main.createIcon("football", 32),
                    null,
                    currentName
            );
            
            if (newName != null && !newName.trim().isEmpty() && !newName.trim().equals(currentName)) {
                boolean nameExists = false;
                for (int i = 0; i < tournament.getTeamCount(); i++) {
                    if (i != homeTeamIndex && tournament.getTeamName(i).equalsIgnoreCase(newName.trim())) {
                        nameExists = true;
                        break;
                    }
                }
                
                if (nameExists) {
                    JOptionPane.showMessageDialog(
                            this,
                            "Команда с таким названием уже существует!",
                            "Ошибка",
                            JOptionPane.ERROR_MESSAGE
                    );
                    return;
                }
                
                tournament.setTeamName(homeTeamIndex, newName.trim());
                
                JOptionPane.showMessageDialog(
                        this,
                        String.format("Команда переименована:\n\"%s\" → \"%s\"", 
                                     currentName, newName.trim()),
                        "Успешно",
                        JOptionPane.INFORMATION_MESSAGE
                );
            }
            
        } catch (Exception e) {
            showError("Ошибка изменения названия", e.getMessage());
        }
    }
    
    // Показывает диалог ошибки
    private void showError(String title, String message) {
        JOptionPane.showMessageDialog(
                this,
                message,
                title,
                JOptionPane.ERROR_MESSAGE
        );
    }
}