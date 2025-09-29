package pw.ns2030.ui;

import pw.ns2030.Main;
import pw.ns2030.model.Tournament;
import pw.ns2030.model.Team;
import pw.ns2030.utils.ScoreCalculator;
import pw.ns2030.utils.TableValidator;

import com.formdev.flatlaf.extras.FlatSVGIcon;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import javax.swing.plaf.basic.BasicSplitPaneDivider;
import javax.swing.plaf.basic.BasicSplitPaneUI;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.List;
import java.util.Map;
import java.util.Properties;

/**
 * Главное окно приложения "Турнирная таблица".
 * Содержит элементы управления турниром и отображение статистики.
 */
public class MainFrame extends JFrame {
    private final Properties config; // Конфигурация приложения
    private Tournament tournament; // Текущий турнир
    private TournamentTablePanel tablePanel; // Панель с турнирной таблицей
    private JPanel controlPanel; // Панель управления
    private JPanel statisticsPanel; // Панель статистики
    private JLabel statusBar; // Строка состояния
    private JSpinner teamCountSpinner; // Поле ввода количества команд
    private JButton newTournamentButton; // Кнопка создания нового турнира
    private JButton clearAllButton; // Кнопка очистки всех результатов
    private JButton showStatisticsButton; // Кнопка показа статистики
    private JTextArea resultsArea; // Текстовая область для отображения турнирной таблицы
    private JProgressBar progressBar; // Прогресс-бар завершенности турнира
    private JLabel leaderLabel; // Метка с информацией о лидере
    private JSplitPane splitPane; // Разделяемая панель для изменения размеров

    public MainFrame(Properties config) {
        this.config = config;
        
        initializeComponents();
        setupLayout();
        setupMenuBar();
        setupEventHandlers();
        
        // Создаем турнир по умолчанию
        int defaultTeams = Integer.parseInt(config.getProperty("tournament.default.teams", "6"));
        createNewTournament(defaultTeams);
        
        updateUI();
    }
    
    private void initializeComponents() {
        // Панель управления
        controlPanel = new JPanel();
        controlPanel.setBorder(new TitledBorder("Управление турниром"));
        
        // Спиннер для выбора количества команд
        int minTeams = Tournament.MIN_TEAMS;
        int maxTeams = Tournament.MAX_TEAMS;
        int defaultTeams = Integer.parseInt(config.getProperty("tournament.default.teams", "6"));
        
        teamCountSpinner = new JSpinner(new SpinnerNumberModel(defaultTeams, minTeams, maxTeams, 1));
        teamCountSpinner.setToolTipText("Количество команд в турнире (" + minTeams + "-" + maxTeams + ")");
        
        // спиннер
        teamCountSpinner.setPreferredSize(new Dimension(80, 35));
        Font spinnerFont = teamCountSpinner.getFont();
        teamCountSpinner.setFont(spinnerFont.deriveFont(Font.PLAIN, 14f));
        
        // Кнопки управления
        newTournamentButton = new JButton("Создать", Main.createIcon("football", 18));
        newTournamentButton.setToolTipText("Создать новый турнир с указанным количеством команд (Ctrl+N)");
        newTournamentButton.setPreferredSize(new Dimension(140, 40));
        newTournamentButton.setFont(newTournamentButton.getFont().deriveFont(Font.PLAIN, 13f));
        
        clearAllButton = new JButton("Очистить", Main.createIcon("clear-circle", 18));
        clearAllButton.setToolTipText("Очистить все результаты матчей (Ctrl+R)");
        clearAllButton.setPreferredSize(new Dimension(130, 40));
        clearAllButton.setFont(clearAllButton.getFont().deriveFont(Font.PLAIN, 13f));
        
        showStatisticsButton = new JButton("Статистика", Main.createIcon("statistics", 18));
        showStatisticsButton.setToolTipText("Показать подробную статистику турнира (F2)");
        showStatisticsButton.setPreferredSize(new Dimension(140, 40));
        showStatisticsButton.setFont(showStatisticsButton.getFont().deriveFont(Font.PLAIN, 13f));
        
        // Панель статистики
        statisticsPanel = new JPanel();
        statisticsPanel.setBorder(new TitledBorder("Статистика турнира"));
        
        // Область результатов
        resultsArea = new JTextArea(8, 30);
        resultsArea.setEditable(false);
        resultsArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 14));
        resultsArea.setBackground(getBackground());
        
        // Прогресс-бар
        progressBar = new JProgressBar(0, 100);
        progressBar.setStringPainted(true);
        progressBar.setString("Турнир не начат");
        progressBar.setPreferredSize(new Dimension(0, 30));
        progressBar.setFont(progressBar.getFont().deriveFont(Font.PLAIN, 13f));
        
        // Метка лидера
        leaderLabel = new JLabel("Лидер: —");
        leaderLabel.setFont(leaderLabel.getFont().deriveFont(Font.BOLD, 15f));
        leaderLabel.setPreferredSize(new Dimension(0, 35));
        
        // Строка состояния
        statusBar = new JLabel("Готов к работе");
        statusBar.setBorder(BorderFactory.createLoweredBevelBorder());
        statusBar.setPreferredSize(new Dimension(0, 30));
        statusBar.setFont(statusBar.getFont().deriveFont(Font.PLAIN, 13f));
    }
    
    // Настраивает компоновку компонентов.
    private void setupLayout() {
        setLayout(new BorderLayout());
        
        // Верхняя панель с управлением
        setupControlPanel();
        add(controlPanel, BorderLayout.NORTH);
        
        // Создаем разделяемую панель JSplitPane
        splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
        splitPane.setResizeWeight(0.7);
        splitPane.setOneTouchExpandable(true);
        splitPane.setContinuousLayout(true);
        splitPane.setDividerSize(20); 
        
        // Настраиваем UI для увеличенных стрелочек
        setupSplitPaneUI();
        
        // Правая панель со статистикой
        setupStatisticsPanel();
        splitPane.setRightComponent(statisticsPanel);
        
        add(splitPane, BorderLayout.CENTER);
        
        // Строка состояния
        add(statusBar, BorderLayout.SOUTH);
    }
    
    // Настраивает UI разделителя для увеличенных стрелочек.
    private void setupSplitPaneUI() {
        splitPane.setUI(new BasicSplitPaneUI() {
            @Override
            public BasicSplitPaneDivider createDefaultDivider() {
                return new BasicSplitPaneDivider(this) {
                    @Override
                    protected JButton createLeftOneTouchButton() {
                        JButton button = super.createLeftOneTouchButton();
                        button.setPreferredSize(new Dimension(16, 30));
                        button.setMinimumSize(new Dimension(16, 30));
                        button.setMaximumSize(new Dimension(16, 30));
                        button.setToolTipText("Скрыть левую панель");
                        return button;
                    }
                    
                    @Override
                    protected JButton createRightOneTouchButton() {
                        JButton button = super.createRightOneTouchButton();
                        button.setPreferredSize(new Dimension(16, 30));
                        button.setMinimumSize(new Dimension(16, 30));
                        button.setMaximumSize(new Dimension(16, 30));
                        button.setToolTipText("Скрыть правую панель");
                        return button;
                    }
                    
                    @Override
                    public void paint(Graphics g) {
                        super.paint(g);
                        
                        // Рисуем стрелочки в центре
                        Graphics2D g2 = (Graphics2D) g.create();
                        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                        
                        int width = getWidth();
                        int height = getHeight();
                        
                        // Цвет стрелочек
                        g2.setColor(new Color(80, 80, 80));
                        g2.setStroke(new BasicStroke(2.5f));
                        
                        // Центральная позиция для стрелочек
                        int centerY = height / 2;
                        
                        // Левая стрелочка (скрыть левую панель) - в центре левой части
                        int leftX = width / 4;
                        g2.drawLine(leftX + 4, centerY - 6, leftX, centerY);
                        g2.drawLine(leftX, centerY, leftX + 4, centerY + 6);
                        
                        // Правая стрелочка (скрыть правую панель) - в центре правой части
                        int rightX = width * 3 / 4;
                        g2.drawLine(rightX - 4, centerY - 6, rightX, centerY);
                        g2.drawLine(rightX, centerY, rightX - 4, centerY + 6);
                        
                        g2.dispose();
                    }
                };
            }
        });
    }
    
    // Настраивает панель управления.
    private void setupControlPanel() {
        controlPanel.setLayout(new FlowLayout(FlowLayout.LEFT, 8, 10));
        
        JLabel teamLabel = new JLabel("Команд:");
        teamLabel.setFont(teamLabel.getFont().deriveFont(Font.PLAIN, 14f));
        
        controlPanel.add(teamLabel);
        controlPanel.add(teamCountSpinner);
        controlPanel.add(Box.createHorizontalStrut(8));
        controlPanel.add(newTournamentButton);
        controlPanel.add(Box.createHorizontalStrut(6));
        controlPanel.add(clearAllButton);
        controlPanel.add(Box.createHorizontalStrut(6));
        controlPanel.add(showStatisticsButton);
    }
    
    // Настраивает панель статистики.
    private void setupStatisticsPanel() {
        statisticsPanel.setLayout(new BorderLayout());
        statisticsPanel.setMinimumSize(new Dimension(450, 0));
        statisticsPanel.setPreferredSize(new Dimension(550, 0));
        
        // Верхняя часть - прогресс и лидер
        JPanel topStatsPanel = new JPanel(new BorderLayout(10, 10));
        topStatsPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        topStatsPanel.add(progressBar, BorderLayout.NORTH);
        topStatsPanel.add(leaderLabel, BorderLayout.CENTER);
        
        statisticsPanel.add(topStatsPanel, BorderLayout.NORTH);
        
        // Центральная часть - турнирная таблица
        JScrollPane scrollPane = new JScrollPane(resultsArea);
        scrollPane.setBorder(new TitledBorder("Турнирная таблица"));
        statisticsPanel.add(scrollPane, BorderLayout.CENTER);
    }
    
    // Настраивает меню приложения
    private void setupMenuBar() {
        JMenuBar menuBar = new JMenuBar();

        Font menuFont = menuBar.getFont().deriveFont(Font.PLAIN, 13f);
        
        // Меню "Турнир"
        JMenu tournamentMenu = new JMenu("Турнир");
        tournamentMenu.setMnemonic('Т');
        tournamentMenu.setFont(menuFont);
        
        JMenuItem newTournamentItem = new JMenuItem("Новый турнир", Main.createIcon("football", 16));
        newTournamentItem.setMnemonic('Н');
        newTournamentItem.setAccelerator(KeyStroke.getKeyStroke("ctrl N"));
        newTournamentItem.addActionListener(e -> createNewTournamentDialog());
        newTournamentItem.setFont(menuFont);
        
        JMenuItem clearAllItem = new JMenuItem("Очистить все результаты", Main.createIcon("clear-circle", 16));
        clearAllItem.setMnemonic('О');
        clearAllItem.setAccelerator(KeyStroke.getKeyStroke("ctrl R"));
        clearAllItem.addActionListener(e -> clearAllResults());
        clearAllItem.setFont(menuFont);
        
        tournamentMenu.add(newTournamentItem);
        tournamentMenu.addSeparator();
        tournamentMenu.add(clearAllItem);
        
        // Меню "Статистика"
        JMenu statisticsMenu = new JMenu("Статистика");
        statisticsMenu.setMnemonic('С');
        statisticsMenu.setFont(menuFont);
        
        JMenuItem showStatsItem = new JMenuItem("Показать статистику", Main.createIcon("statistics", 16));
        showStatsItem.setMnemonic('П');
        showStatsItem.setAccelerator(KeyStroke.getKeyStroke("F2"));
        showStatsItem.addActionListener(e -> showDetailedStatistics());
        showStatsItem.setFont(menuFont);
        
        JMenuItem exportStatsItem = new JMenuItem("Экспорт турнирной таблицы");
        exportStatsItem.setMnemonic('Э');
        exportStatsItem.setAccelerator(KeyStroke.getKeyStroke("ctrl E"));
        exportStatsItem.addActionListener(e -> exportTournamentTable());
        exportStatsItem.setFont(menuFont);
        
        statisticsMenu.add(showStatsItem);
        statisticsMenu.addSeparator();
        statisticsMenu.add(exportStatsItem);
        
        // Меню "Справка"
        JMenu helpMenu = new JMenu("Справка");
        helpMenu.setMnemonic('п');
        helpMenu.setFont(menuFont);
        
        JMenuItem aboutItem = new JMenuItem("О программе", Main.createIcon("help", 16));
        aboutItem.setMnemonic('О');
        aboutItem.setAccelerator(KeyStroke.getKeyStroke("F1"));
        aboutItem.addActionListener(e -> showAboutDialog());
        aboutItem.setFont(menuFont);
        
        JMenuItem helpItem = new JMenuItem("Помощь");
        helpItem.setMnemonic('П');
        helpItem.setAccelerator(KeyStroke.getKeyStroke("shift F1"));
        helpItem.addActionListener(e -> showHelpDialog());
        helpItem.setFont(menuFont);
        
        helpMenu.add(helpItem);
        helpMenu.addSeparator();
        helpMenu.add(aboutItem);
        
        menuBar.add(tournamentMenu);
        menuBar.add(statisticsMenu);
        menuBar.add(helpMenu);
        
        setJMenuBar(menuBar);
    }
    
    // Настраивает обработчики событий.
    private void setupEventHandlers() {
        newTournamentButton.addActionListener(e -> {
            int teamCount = (Integer) teamCountSpinner.getValue();
            createNewTournament(teamCount);
        });
        
        clearAllButton.addActionListener(e -> clearAllResults());
        
        showStatisticsButton.addActionListener(e -> showDetailedStatistics());
        
        // Обновляем кнопки при изменении количества команд
        teamCountSpinner.addChangeListener(e -> {
            int teamCount = (Integer) teamCountSpinner.getValue();
            newTournamentButton.setToolTipText("Создать новый турнир на " + teamCount + " команд (Ctrl+N)");
        });
    }
    
    // Создает новый турнир с указанным количеством команд.
    private void createNewTournament(int teamCount) {
        try {
            // Валидируем количество команд
            TableValidator.ValidationResult validation = TableValidator.validateTeamCount(teamCount);
            if (!validation.isValid()) {
                showError("Ошибка создания турнира", validation.getErrorMessage());
                return;
            }
            
            tournament = new Tournament(teamCount);
            
            // Удаляем старую таблицу из splitPane
            if (tablePanel != null && splitPane.getLeftComponent() == tablePanel) {
                splitPane.setLeftComponent(null);
            }
            
            // Создаем новую панель таблицы
            tablePanel = new TournamentTablePanel(tournament);
            
            // Добавляем в левую часть splitPane
            splitPane.setLeftComponent(tablePanel);
            
            // Добавляем слушателя изменений турнира
            tournament.addTournamentListener(new TournamentChangeListener());
            
            // Обновляем интерфейс
            updateUI();
            validate();
            repaint();
            
            updateStatus("Создан турнир на " + teamCount + " команд");
        } catch (Exception e) {
            showError("Ошибка создания турнира", e.getMessage());
        }
    }
    
    // Показывает диалог создания нового турнира.
    private void createNewTournamentDialog() {
        String input = JOptionPane.showInputDialog(
                this,
                "Введите количество команд (" + Tournament.MIN_TEAMS + "-" + Tournament.MAX_TEAMS + "):",
                "Новый турнир",
                JOptionPane.QUESTION_MESSAGE
        );
        
        if (input != null && !input.trim().isEmpty()) {
            try {
                int teamCount = Integer.parseInt(input.trim());
                teamCountSpinner.setValue(teamCount);
                createNewTournament(teamCount);
            } catch (NumberFormatException e) {
                showError("Ошибка ввода", "Введите корректное число");
            }
        }
    }
    
    // Очищает все результаты турнира.
    private void clearAllResults() {
        if (tournament == null) return;
        
        int result = JOptionPane.showConfirmDialog(
                this,
                "Очистить все результаты матчей?\nЭто действие нельзя отменить.",
                "Подтверждение",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.WARNING_MESSAGE
        );
        
        if (result == JOptionPane.YES_OPTION) {
            tournament.clearAllResults();
            updateUI();
            updateStatus("Все результаты очищены");
        }
    }
    
    // Показывает подробную статистику турнира.
    private void showDetailedStatistics() {
        if (tournament == null) return;
        
        Map<String, Object> stats = ScoreCalculator.calculateTournamentStatistics(tournament);
        
        StringBuilder message = new StringBuilder();
        message.append("СТАТИСТИКА ТУРНИРА\n\n");
        message.append(String.format("Команд: %d\n", stats.get("totalTeams")));
        message.append(String.format("Матчей проведено: %d из %d\n", 
                stats.get("playedMatches"), stats.get("totalMatches")));
        message.append(String.format("Завершенность: %.1f%%\n\n", stats.get("completionPercentage")));
        message.append(String.format("Всего голов: %d\n", stats.get("totalGoals")));
        message.append(String.format("Голов за матч: %.2f\n\n", stats.get("averageGoalsPerMatch")));
        
        if (stats.containsKey("leader")) {
            message.append(String.format("Лидер: %s (%d очков)\n", 
                    stats.get("leader"), stats.get("leaderPoints")));
        }
        
        JOptionPane.showMessageDialog(
                this,
                message.toString(),
                "Статистика турнира",
                JOptionPane.INFORMATION_MESSAGE,
                Main.createIcon("statistics", 32)
        );
    }
    
    // Экспортирует турнирную таблицу в текстовый формат.
    private void exportTournamentTable() {
        if (tournament == null) return;
        
        StringBuilder export = new StringBuilder();
        export.append("ТУРНИРНАЯ ТАБЛИЦА\n");
        export.append("=".repeat(50)).append("\n\n");
        
        List<Team> sortedTeams = tournament.getSortedTable();
        export.append(String.format("%-3s %-20s %-4s %-3s %-3s %-3s %-3s %-6s %-4s\n",
                "М", "КОМАНДА", "О", "И", "В", "Н", "П", "ГОЛЫ", "+/-"));
        export.append("-".repeat(50)).append("\n");
        
        for (Team team : sortedTeams) {
            export.append(String.format("%-3d %-20s %-4d %-3d %-3d %-3d %-3d %-6s %-4d\n",
                    team.getPosition(),
                    team.getName(),
                    team.getTotalPoints(),
                    team.getMatchesPlayed(),
                    team.getWins(),
                    team.getDraws(),
                    team.getLosses(),
                    team.getGoalsFor() + "-" + team.getGoalsAgainst(),
                    team.getGoalDifference()
            ));
        }
        
        export.append("\nСтатистика:\n");
        export.append(ScoreCalculator.formatTournamentSummary(tournament));
        
        // Показываем в диалоге
        JTextArea textArea = new JTextArea(export.toString());
        textArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 14));
        textArea.setEditable(false);
        
        JScrollPane scrollPane = new JScrollPane(textArea);
        scrollPane.setPreferredSize(new Dimension(700, 500));
        
        JOptionPane.showMessageDialog(
                this,
                scrollPane,
                "Экспорт турнирной таблицы",
                JOptionPane.PLAIN_MESSAGE
        );
    }
    
    // Показывает диалог "О программе".
    private void showAboutDialog() {
        String appName = config.getProperty("app.name", "Турнирная таблица");
        String version = config.getProperty("app.version", "1.0");
        
        String message = String.format(
                "%s v%s\n\n" +
                "Лабораторная работа №1\n" +
                "Вариант 4: Турнирная таблица\n\n" +
                "Автор: Шаламов А.Е., группа АВТ-343\n" +
                "НГТУ, 2025\n\n" +
                "Технологии:\n" +
                "• Java 23 + Swing\n" +
                "• FlatLaf Look&Feel\n" +
                "• Gradle 8.10\n\n" +
                "Функции:\n" +
                "• Симметричное заполнение результатов\n" +
                "• Автоматический подсчёт очков\n" +
                "• Сортировка по турнирной таблице\n" +
                "• Контекстное меню\n" +
                "• Статистика турнира",
                appName, version
        );
        
        JOptionPane.showMessageDialog(
                this,
                message,
                "О программе",
                JOptionPane.INFORMATION_MESSAGE,
                Main.createIcon("app-icon", 48)
        );
    }
    
    // Показывает диалог справки.
    private void showHelpDialog() {
        String helpText = 
                "ПОМОЩЬ ПО ИСПОЛЬЗОВАНИЮ\n\n" +
                "1. СОЗДАНИЕ ТУРНИРА:\n" +
                "   • Выберите количество команд (3-20)\n" +
                "   • Нажмите 'Создать' или Ctrl+N\n\n" +
                "2. ЗАПОЛНЕНИЕ РЕЗУЛЬТАТОВ:\n" +
                "   • Введите результат в формате 'X:Y'\n" +
                "   • Обратный результат заполнится автоматически\n" +
                "   • Для очистки оставьте поле пустым или нажмите Delete\n\n" +
                "3. КОНТЕКСТНОЕ МЕНЮ:\n" +
                "   • Правый клик по ячейке → операции\n" +
                "   • Очистка ячейки/строки/всей таблицы\n" +
                "   • Статистика команды\n\n" +
                "4. ТУРНИРНАЯ ТАБЛИЦА:\n" +
                "   • Автоматическая сортировка команд\n" +
                "   • 3 очка за победу, 1 за ничью\n" +
                "   • Сортировка: очки → разность → голы\n\n" +
                "5. ГОРЯЧИЕ КЛАВИШИ:\n" +
                "   • Ctrl+N: Новый турнир\n" +
                "   • Ctrl+R: Очистить результаты\n" +
                "   • Ctrl+E: Экспорт таблицы\n" +
                "   • F1: О программе\n" +
                "   • Shift+F1: Эта справка\n" +
                "   • F2: Статистика турнира\n" +
                "   • Delete: Очистить выделенную ячейку\n" +
                "   • Escape: Снять фокус";
        
        JTextArea textArea = new JTextArea(helpText);
        textArea.setEditable(false);
        textArea.setFont(textArea.getFont().deriveFont(14f));
        
        JScrollPane scrollPane = new JScrollPane(textArea);
        scrollPane.setPreferredSize(new Dimension(650, 550));
        
        JOptionPane.showMessageDialog(
                this,
                scrollPane,
                "Справка",
                JOptionPane.INFORMATION_MESSAGE,
                Main.createIcon("help", 32)
        );
    }
    
    // Обновляет весь пользовательский интерфейс.
    private void updateUI() {
        if (tournament == null) return;
        
        updateTournamentTable();
        updateProgressBar();
        updateLeaderInfo();
    }
    
    // Обновляет отображение турнирной таблицы.
    private void updateTournamentTable() {
        if (tournament == null) return;
        
        StringBuilder table = new StringBuilder();
        List<Team> sortedTeams = tournament.getSortedTable();
        
        table.append(String.format("%-3s %-20s %5s %3s %3s %3s %3s %8s %5s\n",
                "М", "КОМАНДА", "ОЧКИ", "И", "В", "Н", "П", "ГОЛЫ", "+/-"));
        table.append("=".repeat(70)).append("\n");
        
        // Команды
        for (Team team : sortedTeams) {
            String teamName = team.getName();
            
            if (teamName.length() > 20) {
                teamName = teamName.substring(0, 17) + "...";
            }

            String leaderMark = team.isLeader() ? "*" : " ";
            
            table.append(String.format("%-3s %-20s %5d %3d %3d %3d %3d %8s %5d\n",
                    leaderMark + (team.getPosition() + "."),
                    teamName,
                    team.getTotalPoints(),
                    team.getMatchesPlayed(),
                    team.getWins(),
                    team.getDraws(),
                    team.getLosses(),
                    team.getGoalsFor() + "-" + team.getGoalsAgainst(),
                    team.getGoalDifference()
            ));
        }
        
        resultsArea.setText(table.toString());
        resultsArea.setCaretPosition(0);
    }
    
    // Обновляет прогресс-бар завершенности турнира.
    private void updateProgressBar() {
        if (tournament == null) return;
        
        double completion = tournament.getCompletionPercentage();
        progressBar.setValue((int) completion);
        progressBar.setString(String.format("%.1f%% (%d/%d матчей)", 
                completion, tournament.getTotalMatchesPlayed(), tournament.getTotalMatches()));
    }
    
    // Обновляет информацию о лидере.
    private void updateLeaderInfo() {
        if (tournament == null) return;
        
        Team leader = tournament.getLeader();
        
        leaderLabel.setText(String.format("Лидер: %s (%d очков)", 
                leader.getName(), leader.getTotalPoints()));
        
        try {
            FlatSVGIcon trophyIcon = Main.createIcon("trophy", 18);
            if (trophyIcon != null) {
                leaderLabel.setIcon(trophyIcon);
            }
        } catch (Exception e) {
            leaderLabel.setIcon(null);
        }
    }
    
    // Обновляет строку состояния.
    private void updateStatus(String message) {
        statusBar.setText(message);
        
        // Автоматически очищаем статус через 5 секунд
        Timer timer = new Timer(5000, e -> {
            if (tournament != null) {
                statusBar.setText(ScoreCalculator.formatTournamentSummary(tournament));
            }
        });
        timer.setRepeats(false);
        timer.start();
    }
    
    // Показывает диалог ошибки.
    private void showError(String title, String message) {
        JOptionPane.showMessageDialog(
                this,
                message,
                title,
                JOptionPane.ERROR_MESSAGE
        );
    }
    
    // Слушатель изменений турнира для обновления GUI.
    private class TournamentChangeListener implements Tournament.TournamentListener {
        
        @Override
        public void onMatchResultChanged(int homeTeam, int awayTeam, pw.ns2030.model.GameResult result) {
            SwingUtilities.invokeLater(() -> {
                updateUI();
                String homeTeamName = tournament.getTeamName(homeTeam);
                String awayTeamName = tournament.getTeamName(awayTeam);
                
                if (result.isPlayed()) {
                    updateStatus(String.format("Результат: %s %s %s", 
                            homeTeamName, result.getFormattedResult(), awayTeamName));
                } else {
                    updateStatus(String.format("Очищен результат: %s vs %s", 
                            homeTeamName, awayTeamName));
                }
            });
        }
        
        @Override
        public void onTeamNameChanged(int teamIndex, String newName) {
            SwingUtilities.invokeLater(() -> {
                updateUI();
                updateStatus("Команда переименована: " + newName);
            });
        }
        
        @Override
        public void onTableResorted(List<Team> sortedTeams) {
            SwingUtilities.invokeLater(() -> {
                updateTournamentTable();
                updateLeaderInfo();
            });
        }
    }
}