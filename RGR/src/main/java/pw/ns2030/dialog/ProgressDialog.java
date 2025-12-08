package pw.ns2030.dialog;

import pw.ns2030.task.BackgroundTask;
import pw.ns2030.task.TaskHelpers.*;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;

/**
 * Модальный диалог с индикатором прогресса для фоновых операций.
 * Блокирует основное окно до завершения задачи.
 */
public class ProgressDialog extends JDialog {
    private JProgressBar progressBar;
    private JLabel statusLabel;
    private JLabel detailsLabel;
    private JLabel timeLabel;
    private JButton cancelButton;
    private BackgroundTask<?> currentTask;
    private Timer timeUpdateTimer;
    private long startTime;

    public ProgressDialog(Frame owner, String title) {
        super(owner, title, true);  // modal
        
        initComponents();
        setupLayout();
        
        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
        setResizable(false);
        pack();
        setLocationRelativeTo(owner);
    }

    private void initComponents() {
        progressBar = new JProgressBar(0, 100);
        progressBar.setStringPainted(true);
        progressBar.setPreferredSize(new Dimension(400, 30));
        progressBar.setFont(new Font("Arial", Font.BOLD, 14));
        
        statusLabel = new JLabel("Выполнение операции...");
        statusLabel.setFont(new Font("Arial", Font.BOLD, 13));
        statusLabel.setHorizontalAlignment(SwingConstants.CENTER);
        
        detailsLabel = new JLabel(" ");
        detailsLabel.setFont(new Font("Arial", Font.PLAIN, 11));
        detailsLabel.setHorizontalAlignment(SwingConstants.CENTER);
        detailsLabel.setForeground(Color.GRAY);
        
        timeLabel = new JLabel("Прошло: 0 сек");
        timeLabel.setFont(new Font("Arial", Font.PLAIN, 10));
        timeLabel.setHorizontalAlignment(SwingConstants.CENTER);
        timeLabel.setForeground(Color.DARK_GRAY);
        
        cancelButton = new JButton("Отменить");
        cancelButton.setFont(new Font("Arial", Font.BOLD, 12));
        cancelButton.setBackground(new Color(244, 67, 54));
        cancelButton.setForeground(Color.WHITE);
        cancelButton.setFocusPainted(false);
        cancelButton.setBorderPainted(false);
        cancelButton.setOpaque(true);
        cancelButton.setPreferredSize(new Dimension(120, 32));
        cancelButton.setCursor(new Cursor(Cursor.HAND_CURSOR));
        
        cancelButton.addActionListener(e -> onCancel());
    }

    private void setupLayout() {
        JPanel contentPane = new JPanel(new BorderLayout(10, 10));
        contentPane.setBorder(new EmptyBorder(20, 20, 20, 20));
        
        JPanel topPanel = new JPanel(new BorderLayout(5, 5));
        topPanel.add(statusLabel, BorderLayout.NORTH);
        topPanel.add(detailsLabel, BorderLayout.CENTER);
        topPanel.add(timeLabel, BorderLayout.SOUTH);
        
        JPanel centerPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        centerPanel.add(progressBar);
        
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        buttonPanel.add(cancelButton);
        
        contentPane.add(topPanel, BorderLayout.NORTH);
        contentPane.add(centerPanel, BorderLayout.CENTER);
        contentPane.add(buttonPanel, BorderLayout.SOUTH);
        
        setContentPane(contentPane);
    }

    /**
     * Запуск фоновой задачи с отображением прогресса.
     */
    public <T> void executeTask(BackgroundTask<T> task) {
        this.currentTask = task;
        this.startTime = System.currentTimeMillis();
        
        // Подписываемся на обновления прогресса
        task.setProgressListener((percent, message) -> {
            SwingUtilities.invokeLater(() -> {
                progressBar.setValue(percent);
                detailsLabel.setText(message);
            });
        });
        
        // Подписываемся на завершение
        task.setCompletionListener(new TaskCompletionListener<T>() {
            @Override
            public void onSuccess(T result) {
                SwingUtilities.invokeLater(() -> {
                    stopTimeUpdates();
                    dispose();
                    showSuccess(result);
                });
            }
            
            @Override
            public void onError(Exception error) {
                SwingUtilities.invokeLater(() -> {
                    stopTimeUpdates();
                    dispose();
                    showError(error);
                });
            }
            
            @Override
            public void onCancelled() {
                SwingUtilities.invokeLater(() -> {
                    stopTimeUpdates();
                    dispose();
                });
            }
        });
        
        // Запускаем задачу
        task.execute();
        
        // Запускаем таймер обновления времени
        startTimeUpdates();
        
        // Показываем диалог (блокирует до завершения)
        setVisible(true);
    }

    private void startTimeUpdates() {
        timeUpdateTimer = new Timer(1000, e -> {
            long elapsed = System.currentTimeMillis() - startTime;
            long seconds = elapsed / 1000;
            long minutes = seconds / 60;
            seconds = seconds % 60;
            
            String timeText = minutes > 0 ? 
                String.format("Прошло: %d мин %d сек", minutes, seconds) :
                String.format("Прошло: %d сек", seconds);
            
            timeLabel.setText(timeText);
        });
        timeUpdateTimer.start();
    }

    private void stopTimeUpdates() {
        if (timeUpdateTimer != null) {
            timeUpdateTimer.stop();
            timeUpdateTimer = null;
        }
    }

    private void onCancel() {
        int result = JOptionPane.showConfirmDialog(
            this,
            "Прервать выполнение операции?",
            "Подтверждение",
            JOptionPane.YES_NO_OPTION,
            JOptionPane.WARNING_MESSAGE
        );
        
        if (result == JOptionPane.YES_OPTION) {
            statusLabel.setText("Отмена операции...");
            detailsLabel.setText("Ожидание завершения текущего шага...");
            cancelButton.setEnabled(false);
            currentTask.requestCancel();
        }
    }

    private <T> void showSuccess(T result) {
        String message = result != null ? result.toString() : "Операция успешно завершена!";
        
        JTextArea textArea = new JTextArea(message, 15, 50);
        textArea.setEditable(false);
        textArea.setFont(new Font("Monospaced", Font.PLAIN, 11));
        JScrollPane scrollPane = new JScrollPane(textArea);
        
        JOptionPane.showMessageDialog(
            getOwner(),
            scrollPane,
            "Операция завершена",
            JOptionPane.INFORMATION_MESSAGE
        );
    }

    private void showError(Exception error) {
        String message = String.format(
            "Ошибка выполнения операции:\n\n%s\n\nПодробности:\n%s",
            error.getMessage(),
            error.getClass().getSimpleName()
        );
        
        JOptionPane.showMessageDialog(
            getOwner(),
            message,
            "Ошибка",
            JOptionPane.ERROR_MESSAGE
        );
    }
}