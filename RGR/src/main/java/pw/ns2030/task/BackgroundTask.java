package pw.ns2030.task;

import pw.ns2030.task.TaskHelpers.*;

import javax.swing.SwingWorker;
import java.util.List;

/**
 * Базовый класс для фоновых операций с прогресс-баром.
 * Использует SwingWorker для выполнения задачи без блокировки GUI.
 * Реализует паттерн Template Method.
 */
public abstract class BackgroundTask<T> extends SwingWorker<T, ProgressUpdate> {
    private volatile boolean cancelled = false;
    private ProgressListener progressListener;
    private TaskCompletionListener<T> completionListener;
    private final long startTime;
    
    public BackgroundTask() {
        this.startTime = System.currentTimeMillis();
    }

    /**
     * Основная работа задачи. Переопределяется наследниками.
     */
    protected abstract T performTask() throws Exception;

    /**
     * Отправка обновления прогресса в GUI.
     */
    protected void publishProgress(int percent, String message) {
        if (percent < 0 || percent > 100) {
            throw new IllegalArgumentException("Процент должен быть в диапазоне 0-100");
        }
        
        ProgressUpdate update = new ProgressUpdate(percent, message);
        publish(update);
        setProgress(percent);
    }

    /**
     * Проверка отмены задачи пользователем.
     */
    protected void checkCancelled() throws TaskCancelledException {
        if (cancelled || isCancelled()) {
            throw new TaskCancelledException();
        }
    }

    protected long getElapsedTime() {
        return System.currentTimeMillis() - startTime;
    }

    protected String getFormattedElapsedTime() {
        long elapsed = getElapsedTime();
        long seconds = elapsed / 1000;
        long minutes = seconds / 60;
        seconds = seconds % 60;
        
        if (minutes > 0) {
            return String.format("%d мин %d сек", minutes, seconds);
        } else {
            return String.format("%d сек", seconds);
        }
    }

    @Override
    protected final T doInBackground() throws Exception {
        try {
            System.out.println("[BackgroundTask] Начало выполнения: " + getClass().getSimpleName());
            publishProgress(0, "Запуск операции...");
            
            T result = performTask();
            
            System.out.println("[BackgroundTask] Завершено успешно за " + getFormattedElapsedTime());
            publishProgress(100, "Операция завершена");
            
            return result;
            
        } catch (TaskCancelledException e) {
            System.out.println("[BackgroundTask] Отменено пользователем");
            throw e;
        } catch (Exception e) {
            System.err.println("[BackgroundTask] Ошибка выполнения: " + e.getMessage());
            e.printStackTrace();
            throw e;
        }
    }

    @Override
    protected final void process(List<ProgressUpdate> chunks) {
        if (progressListener != null && !chunks.isEmpty()) {
            ProgressUpdate latest = chunks.get(chunks.size() - 1);
            progressListener.onProgressUpdate(latest.getPercent(), latest.getMessage());
        }
    }

    @Override
    protected final void done() {
        if (completionListener == null) {
            return;
        }

        try {
            if (isCancelled()) {
                completionListener.onCancelled();
            } else {
                T result = get();
                completionListener.onSuccess(result);
            }
        } catch (java.util.concurrent.CancellationException e) {
            completionListener.onCancelled();
        } catch (Exception e) {
            Throwable cause = e.getCause();
            if (cause instanceof TaskCancelledException) {
                completionListener.onCancelled();
            } else {
                Exception error = (cause instanceof Exception) ? 
                                 (Exception) cause : e;
                completionListener.onError(error);
            }
        }
    }

    public void setProgressListener(ProgressListener listener) {
        this.progressListener = listener;
    }

    public void setCompletionListener(TaskCompletionListener<T> listener) {
        this.completionListener = listener;
    }

    public void requestCancel() {
        this.cancelled = true;
        super.cancel(true);
    }
}