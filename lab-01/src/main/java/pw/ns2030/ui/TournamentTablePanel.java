package pw.ns2030.ui;

import pw.ns2030.Main;
import pw.ns2030.model.Tournament;
import pw.ns2030.model.GameResult;
import pw.ns2030.utils.TableValidator;

import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.text.AbstractDocument;
import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.DocumentFilter;
import java.awt.*;
import java.awt.event.*;
import java.util.HashMap;
import java.util.Map;

/**
 * Панель с турнирной таблицей на основе двумерного массива TextField-ов.
 * Обеспечивает редактирование результатов матчей и названий команд с валидацией.
 */
public class TournamentTablePanel extends JPanel implements Tournament.TournamentListener {
    private final Tournament tournament; // Турнир для отображения
    private JTextField[][] resultFields; // Двумерный массив текстовых полей для результатов
    private JTextField[] teamNameFields; // Массив полей для названий команд
    private TournamentContextMenu contextMenu; // Контекстное меню
    private final Map<JTextField, Point> fieldToPosition = new HashMap<>(); // Карта текстовых полей для быстрого поиска
    
    private static final int CELL_SIZE = 90; // Размер ячейки результата
    private static final int HEADER_HEIGHT = 50; // Высота заголовка
    private static final int TEAM_NAME_WIDTH = 200; // Ширина поля названия команды
    
    private static final Color DISABLED_CELL_COLOR = new Color(240, 240, 240); // Цвет заблокированной ячейки
    private static final Color WIN_COLOR = new Color(200, 255, 200); // Цвет победы
    private static final Color DRAW_COLOR = new Color(255, 255, 200); // Цвет ничьи
    private static final Color LOSS_COLOR = new Color(255, 200, 200); // Цвет поражения
    private static final Color EMPTY_COLOR = Color.WHITE; // Цвет пустой ячейки
    
    // Создает панель турнирной таблицы.
    public TournamentTablePanel(Tournament tournament) {
        this.tournament = tournament;
        this.contextMenu = new TournamentContextMenu(tournament);
        
        initializeTable();
        setupLayout();
        setupEventHandlers();

        tournament.addTournamentListener(this);
        
        updateTable();
    }

    private void initializeTable() {
        int teamCount = tournament.getTeamCount();
        
        // Создаем массивы полей
        resultFields = new JTextField[teamCount][teamCount];
        teamNameFields = new JTextField[teamCount];
        
        // Создаем поля для названий команд
        for (int i = 0; i < teamCount; i++) {
            teamNameFields[i] = createTeamNameField(i);
        }
        
        // Создаем поля для результатов
        for (int i = 0; i < teamCount; i++) {
            for (int j = 0; j < teamCount; j++) {
                if (i != j) {
                    resultFields[i][j] = createResultField(i, j);
                    fieldToPosition.put(resultFields[i][j], new Point(i, j));
                }
            }
        }
    }
    
    // Создает текстовое поле для названия команды.
    private JTextField createTeamNameField(int teamIndex) {
        JTextField field = new JTextField(tournament.getTeamName(teamIndex));
        field.setPreferredSize(new Dimension(TEAM_NAME_WIDTH, HEADER_HEIGHT));
        field.setHorizontalAlignment(JTextField.CENTER);

        Font currentFont = field.getFont();
        field.setFont(currentFont.deriveFont(Font.BOLD, 14f));
        
        field.setBorder(createCellBorder());
        field.setBackground(new Color(230, 230, 255));
        
        // Горячие клавиши в подсказке
        field.setToolTipText("<html>Название команды<br/>Клавиши: ESC - снять фокус, DELETE - очистить ячейку</html>");
        
        // Добавляем обработчик изменения названия
        field.addActionListener(e -> updateTeamName(teamIndex, field.getText()));
        field.addFocusListener(new FocusAdapter() {
            @Override
            public void focusLost(FocusEvent e) {
                updateTeamName(teamIndex, field.getText());
            }
        });
        
        // Контекстное меню для заголовка команды
        field.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                if (e.isPopupTrigger()) {
                    showContextMenuForTeam(teamIndex, e.getX(), e.getY(), field);
                }
            }
            
            @Override
            public void mouseReleased(MouseEvent e) {
                if (e.isPopupTrigger()) {
                    showContextMenuForTeam(teamIndex, e.getX(), e.getY(), field);
                }
            }
        });
        
        return field;
    }
    
    // Создает текстовое поле для результата матча.
    private JTextField createResultField(int homeTeam, int awayTeam) {
        JTextField field = new JTextField();
        field.setPreferredSize(new Dimension(CELL_SIZE, CELL_SIZE));
        field.setHorizontalAlignment(JTextField.CENTER);

        Font currentFont = field.getFont();
        field.setFont(currentFont.deriveFont(Font.BOLD, 16f));
        
        field.setBorder(createCellBorder());
        
        // Добавляем фильтр документа для валидации ввода
        ((AbstractDocument) field.getDocument()).setDocumentFilter(new ScoreDocumentFilter());
        
        // Добавляем обработчики событий
        field.addActionListener(e -> updateMatchResult(homeTeam, awayTeam, field.getText()));
        field.addFocusListener(new FocusAdapter() {
            @Override
            public void focusLost(FocusEvent e) {
                updateMatchResult(homeTeam, awayTeam, field.getText());
            }
            
            @Override
            public void focusGained(FocusEvent e) {
                field.selectAll();
            }
        });
        
        // Контекстное меню
        field.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                if (e.isPopupTrigger()) {
                    showContextMenuForCell(homeTeam, awayTeam, e.getX(), e.getY(), field);
                }
            }
            
            @Override
            public void mouseReleased(MouseEvent e) {
                if (e.isPopupTrigger()) {
                    showContextMenuForCell(homeTeam, awayTeam, e.getX(), e.getY(), field);
                }
            }
        });
        
        // Подсказка с горячими клавишами
        field.setToolTipText(String.format("<html>Результат: %s vs %s (формат: X:Y)<br/>" +
                "Клавиши: ESC - снять фокус, DELETE - очистить, F2 - статистика</html>", 
                tournament.getTeamName(homeTeam), tournament.getTeamName(awayTeam)));
        
        return field;
    }
    
    // Создает границу для ячейки.
    private Border createCellBorder() {
        return BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(Color.GRAY, 1),
                BorderFactory.createEmptyBorder(4, 4, 4, 4)
        );
    }
    
    // Настраивает компоновку панели.
    private void setupLayout() {
        setLayout(new BorderLayout());
        
        JPanel tablePanel = createTablePanel();
        JScrollPane scrollPane = new JScrollPane(tablePanel);
        scrollPane.setPreferredSize(calculatePreferredSize());
        
        add(scrollPane, BorderLayout.CENTER);
    }
    
    // Создает основную панель таблицы.
    private JPanel createTablePanel() {
        int teamCount = tournament.getTeamCount();
        JPanel panel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        
        // Пустая ячейка в левом верхнем углу
        gbc.gridx = 0;
        gbc.gridy = 0;
        JLabel cornerLabel = new JLabel("Команды");
        cornerLabel.setPreferredSize(new Dimension(TEAM_NAME_WIDTH, HEADER_HEIGHT));
        cornerLabel.setHorizontalAlignment(SwingConstants.CENTER);
        cornerLabel.setBackground(new Color(200, 200, 200));
        cornerLabel.setOpaque(true);
        cornerLabel.setBorder(createCellBorder());

        Font cornerFont = cornerLabel.getFont();
        cornerLabel.setFont(cornerFont.deriveFont(Font.BOLD, 14f));
        
        panel.add(cornerLabel, gbc);
        
        // Заголовки столбцов (названия команд)
        for (int j = 0; j < teamCount; j++) {
            gbc.gridx = j + 1;
            gbc.gridy = 0;
            
            // Создаем лейбл с переносом строк для длинных названий
            JLabel columnHeader = createColumnHeader(j);
            columnHeader.setPreferredSize(new Dimension(CELL_SIZE, HEADER_HEIGHT));
            columnHeader.setBackground(new Color(200, 200, 200));
            columnHeader.setOpaque(true);
            columnHeader.setBorder(createCellBorder());
            
            panel.add(columnHeader, gbc);
        }
        
        // Строки таблицы
        for (int i = 0; i < teamCount; i++) {
            // Заголовок строки (название команды)
            gbc.gridx = 0;
            gbc.gridy = i + 1;
            panel.add(teamNameFields[i], gbc);
            
            // Ячейки результатов
            for (int j = 0; j < teamCount; j++) {
                gbc.gridx = j + 1;
                gbc.gridy = i + 1;
                
                if (i == j) {
                    // Диагональная ячейка (команда сама с собой)
                    JLabel diagonalCell = new JLabel("—");
                    diagonalCell.setPreferredSize(new Dimension(CELL_SIZE, CELL_SIZE));
                    diagonalCell.setHorizontalAlignment(SwingConstants.CENTER);
                    diagonalCell.setBackground(DISABLED_CELL_COLOR);
                    diagonalCell.setOpaque(true);
                    diagonalCell.setBorder(createCellBorder());

                    Font diagonalFont = diagonalCell.getFont();
                    diagonalCell.setFont(diagonalFont.deriveFont(Font.BOLD, 18f));
                    
                    panel.add(diagonalCell, gbc);
                } else {
                    panel.add(resultFields[i][j], gbc);
                }
            }
        }
        
        return panel;
    }
    
    // Создает заголовок столбца с поддержкой HTML для переноса строк.
    private JLabel createColumnHeader(int teamIndex) {
        String teamName = tournament.getTeamName(teamIndex);
        String displayText = formatTeamNameForColumn(teamName);
        
        JLabel label = new JLabel(displayText);
        label.setHorizontalAlignment(SwingConstants.CENTER);
        label.setVerticalAlignment(SwingConstants.CENTER);
        
        Font headerFont = label.getFont();
        label.setFont(headerFont.deriveFont(Font.BOLD, 11f));
        label.setToolTipText("<html>" + teamName + "<br/>Команда " + (teamIndex + 1) + "</html>");
        
        return label;
    }
    
    // Форматирует название команды для отображения в заголовке столбца.
    private String formatTeamNameForColumn(String teamName) {
        if (teamName.length() <= 10) {
            return "<html><center>" + teamName + "</center></html>";
        }
        
        // Разбиваем длинное название на части
        String[] words = teamName.split("\\s+");
        if (words.length == 1) {
            // Одно длинное слово - разбиваем посередине
            int mid = teamName.length() / 2;
            String part1 = teamName.substring(0, mid);
            String part2 = teamName.substring(mid);
            return "<html><center>" + part1 + "<br/>" + part2 + "</center></html>";
        } else {
            // Несколько слов - распределяем по строкам
            StringBuilder line1 = new StringBuilder();
            StringBuilder line2 = new StringBuilder();
            
            for (int i = 0; i < words.length; i++) {
                if (i < words.length / 2) {
                    if (line1.length() > 0) line1.append(" ");
                    line1.append(words[i]);
                } else {
                    if (line2.length() > 0) line2.append(" ");
                    line2.append(words[i]);
                }
            }
            
            return "<html><center>" + line1 + "<br/>" + line2 + "</center></html>";
        }
    }
    
    // Вычисляет предпочтительный размер панели.
    private Dimension calculatePreferredSize() {
        int teamCount = tournament.getTeamCount();
        int width = TEAM_NAME_WIDTH + teamCount * CELL_SIZE + 80; // +80 для полос прокрутки
        int height = HEADER_HEIGHT + teamCount * CELL_SIZE + 80;

        width = Math.min(width, 1400);
        height = Math.min(height, 900);
        
        return new Dimension(width, height);
    }
    
    // Настраивает обработчики событий.
    private void setupEventHandlers() {
        // Глобальные горячие клавиши
        setupKeyBindings();
    }
    
    // Настраивает горячие клавиши.
    private void setupKeyBindings() {
        InputMap inputMap = getInputMap(WHEN_ANCESTOR_OF_FOCUSED_COMPONENT);
        ActionMap actionMap = getActionMap();
        
        // Escape - снятие фокуса
        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), "clearFocus");
        actionMap.put("clearFocus", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                requestFocusInWindow();
            }
        });
        
        // Delete - очистка ячейки
        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_DELETE, 0), "clearCell");
        actionMap.put("clearCell", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                clearFocusedCell();
            }
        });
        
        // F1 - справка
        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_F1, 0), "showHelp");
        actionMap.put("showHelp", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                showQuickHelp();
            }
        });
        
        // F2 - статистика
        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_F2, 0), "showStats");
        actionMap.put("showStats", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                showQuickStats();
            }
        });
    }
    
    // Показывает быструю справку.
    private void showQuickHelp() {
        String helpText = "ГОРЯЧИЕ КЛАВИШИ:\n\n" +
                "ESC - снять фокус\n" +
                "DELETE - очистить ячейку\n" +
                "F1 - эта справка\n" +
                "F2 - статистика турнира\n" +
                "Ctrl+N - новый турнир\n" +
                "Ctrl+R - очистить все\n\n" +
                "ФОРМАТ ВВОДА:\n" +
                "X:Y (например: 2:1)";
        
        JOptionPane.showMessageDialog(this, helpText, "Быстрая справка (F1)", JOptionPane.INFORMATION_MESSAGE);
    }
    
    // Показывает быструю статистику.
    private void showQuickStats() {
        String stats = String.format("СТАТИСТИКА:\n\n" +
                "Команд: %d\n" +
                "Матчей сыграно: %d из %d\n" +
                "Завершенность: %.1f%%\n" +
                "Лидер: %s",
                tournament.getTeamCount(),
                tournament.getTotalMatchesPlayed(),
                tournament.getTotalMatches(),
                tournament.getCompletionPercentage(),
                tournament.getLeader().getName());
        
        JOptionPane.showMessageDialog(this, stats, "Быстрая статистика (F2)", JOptionPane.INFORMATION_MESSAGE);
    }
    
    // Очищает ячейку, находящуюся в фокусе.
    private void clearFocusedCell() {
        Component focused = KeyboardFocusManager.getCurrentKeyboardFocusManager().getFocusOwner();
        if (focused instanceof JTextField && fieldToPosition.containsKey(focused)) {
            JTextField field = (JTextField) focused;
            Point pos = fieldToPosition.get(field);
            
            if (pos != null) {
                field.setText("");
                updateMatchResult(pos.x, pos.y, "");
            }
        }
    }
    
    // Обновляет название команды.
    private void updateTeamName(int teamIndex, String newName) {
        try {
            TableValidator.ValidationResult validation = 
                    TableValidator.validateTeamNameUniqueness(tournament, newName, teamIndex);
            
            if (!validation.isValid()) {
                JOptionPane.showMessageDialog(this, validation.getErrorMessage(), 
                        "Ошибка", JOptionPane.ERROR_MESSAGE);
                teamNameFields[teamIndex].setText(tournament.getTeamName(teamIndex));
                return;
            }
            
            tournament.setTeamName(teamIndex, newName);
            
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, e.getMessage(), 
                    "Ошибка", JOptionPane.ERROR_MESSAGE);
            teamNameFields[teamIndex].setText(tournament.getTeamName(teamIndex));
        }
    }
    
    // Обновляет результат матча.
    private void updateMatchResult(int homeTeam, int awayTeam, String resultString) {
        try {
            TableValidator.ValidationResult validation = 
                    TableValidator.validateScoreString(resultString);
            
            if (!validation.isValid()) {
                showValidationError(resultFields[homeTeam][awayTeam], validation.getErrorMessage());
                return;
            }
            
            tournament.setMatchResult(homeTeam, awayTeam, resultString);
            
        } catch (Exception e) {
            showValidationError(resultFields[homeTeam][awayTeam], e.getMessage());
        }
    }
    
    // Показывает ошибку валидации.
    private void showValidationError(JTextField field, String message) {
        field.setBackground(new Color(255, 200, 200));
        field.setToolTipText("Ошибка: " + message);
        
        // Восстанавливаем цвет через 3 секунды
        Timer timer = new Timer(3000, e -> {
            updateFieldAppearance(field);
        });
        timer.setRepeats(false);
        timer.start();
    }
    
    // Показывает контекстное меню для ячейки.
    private void showContextMenuForCell(int homeTeam, int awayTeam, int x, int y, Component invoker) {
        contextMenu.showForMatchCell(homeTeam, awayTeam, x, y, invoker);
    }
    
    // Показывает контекстное меню для заголовка команды.
    private void showContextMenuForTeam(int teamIndex, int x, int y, Component invoker) {
        contextMenu.showForTeamHeader(teamIndex, x, y, invoker);
    }
    
    // Обновляет всю таблицу.
    private void updateTable() {
        SwingUtilities.invokeLater(() -> {
            updateResultFields();
            updateTeamNameFields();
            updateFieldColors();
        });
    }
    
    // Обновляет поля результатов.
    private void updateResultFields() {
        int teamCount = tournament.getTeamCount();
        
        for (int i = 0; i < teamCount; i++) {
            for (int j = 0; j < teamCount; j++) {
                if (i != j && resultFields[i][j] != null) {
                    GameResult result = tournament.getMatchResult(i, j);
                    resultFields[i][j].setText(result.getFormattedResult());
                    updateFieldAppearance(resultFields[i][j]);
                }
            }
        }
    }
    
    // Обновляет поля названий команд.
    private void updateTeamNameFields() {
        for (int i = 0; i < tournament.getTeamCount(); i++) {
            if (!teamNameFields[i].isFocusOwner()) {
                teamNameFields[i].setText(tournament.getTeamName(i));
            }
        }
    }
    
    // Обновляет цвета полей.
    private void updateFieldColors() {
        int teamCount = tournament.getTeamCount();
        
        for (int i = 0; i < teamCount; i++) {
            for (int j = 0; j < teamCount; j++) {
                if (i != j && resultFields[i][j] != null) {
                    updateFieldAppearance(resultFields[i][j]);
                }
            }
        }
    }
    
    // Обновляет внешний вид текстового поля.
    private void updateFieldAppearance(JTextField field) {
        Point pos = fieldToPosition.get(field);
        if (pos == null) return;
        
        GameResult result = tournament.getMatchResult(pos.x, pos.y);
        
        if (!result.isPlayed()) {
            field.setBackground(EMPTY_COLOR);
            field.setToolTipText(String.format("<html>Результат: %s vs %s (формат: X:Y)<br/>" +
                    "Клавиши: ESC - снять фокус, DELETE - очистить, F2 - статистика</html>", 
                    tournament.getTeamName(pos.x), tournament.getTeamName(pos.y)));
        } else {
            if (result.isHomeWin()) {
                field.setBackground(WIN_COLOR);
            } else if (result.isDraw()) {
                field.setBackground(DRAW_COLOR);
            } else {
                field.setBackground(LOSS_COLOR);
            }
            
            field.setToolTipText(String.format("<html>%s vs %s: %s (%d очков)<br/>" +
                    "Клавиши: DELETE - очистить, F2 - статистика</html>", 
                    tournament.getTeamName(pos.x), tournament.getTeamName(pos.y),
                    result.getFormattedResult(), result.getHomePoints()));
        }
    }
    
    // Реализация Tournament.TournamentListener:
    @Override
    public void onMatchResultChanged(int homeTeam, int awayTeam, GameResult result) {
        SwingUtilities.invokeLater(() -> {
            if (resultFields[homeTeam][awayTeam] != null) {
                if (!resultFields[homeTeam][awayTeam].isFocusOwner()) {
                    resultFields[homeTeam][awayTeam].setText(result.getFormattedResult());
                }
                updateFieldAppearance(resultFields[homeTeam][awayTeam]);
            }
        });
    }
    
    @Override
    public void onTeamNameChanged(int teamIndex, String newName) {
        SwingUtilities.invokeLater(() -> {
            if (!teamNameFields[teamIndex].isFocusOwner()) {
                teamNameFields[teamIndex].setText(newName);
            }
            
            // Обновляем подсказки
            for (int j = 0; j < tournament.getTeamCount(); j++) {
                if (teamIndex != j) {
                    updateFieldAppearance(resultFields[teamIndex][j]);
                    updateFieldAppearance(resultFields[j][teamIndex]);
                }
            }
        });
    }
    
    @Override
    public void onTableResorted(java.util.List<pw.ns2030.model.Team> sortedTeams) {
        // Таблица уже отсортирована...
    }
    
    // Фильтр документа для валидации ввода результатов.
    private static class ScoreDocumentFilter extends DocumentFilter {
        
        @Override
        public void insertString(FilterBypass fb, int offset, String string, AttributeSet attr) 
                throws BadLocationException {
            if (isValidInput(fb.getDocument().getText(0, fb.getDocument().getLength()), string, offset)) {
                super.insertString(fb, offset, string, attr);
            }
        }
        
        @Override
        public void replace(FilterBypass fb, int offset, int length, String text, AttributeSet attrs) 
                throws BadLocationException {
            String currentText = fb.getDocument().getText(0, fb.getDocument().getLength());
            String newText = currentText.substring(0, offset) + text + 
                           currentText.substring(offset + length);
            
            if (newText.isEmpty() || isValidPartialScore(newText)) {
                super.replace(fb, offset, length, text, attrs);
            }
        }
        
        // Проверяет, является ли ввод допустимым.
        private boolean isValidInput(String currentText, String newText, int offset) {
            String resultText = currentText.substring(0, offset) + newText + 
                              currentText.substring(offset);
            return isValidPartialScore(resultText);
        }
        
        // Проверяет, является ли строка допустимым частичным результатом.
        private boolean isValidPartialScore(String text) {
            if (text.isEmpty()) return true;
            
            // Разрешаем только цифры и двоеточие
            if (!text.matches("[0-9:]*")) return false;
            
            // Не более одного двоеточия
            long colonCount = text.chars().filter(ch -> ch == ':').count();
            if (colonCount > 1) return false;
            
            // Проверяем части до и после двоеточия
            String[] parts = text.split(":", -1);
            if (parts.length > 2) return false;
            
            for (String part : parts) {
                if (!part.isEmpty()) {
                    try {
                        int value = Integer.parseInt(part);
                        if (value > 99) return false; // Ограничиваем максимальный счёт
                    } catch (NumberFormatException e) {
                        return false;
                    }
                }
            }
            
            return true;
        }
    }
}