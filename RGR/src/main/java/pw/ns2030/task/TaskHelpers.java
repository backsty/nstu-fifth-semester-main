package pw.ns2030.task;

/**
 * Вспомогательные классы для работы с фоновыми задачами.
 */
public class TaskHelpers {
    
    /**
     * Слушатель обновлений прогресса.
     */
    @FunctionalInterface
    public interface ProgressListener {
        /**
         * Вызывается при обновлении прогресса задачи.
         * 
         * @param percent процент выполнения (0-100)
         * @param message текстовое описание текущего шага
         */
        void onProgressUpdate(int percent, String message);
    }

    /**
     * Слушатель завершения задачи.
     */
    public interface TaskCompletionListener<T> {
        void onSuccess(T result);
        void onError(Exception error);
        void onCancelled();
    }

    /**
     * Исключение отмены задачи.
     */
    public static class TaskCancelledException extends Exception {
        public TaskCancelledException() {
            super("Операция отменена пользователем");
        }
        
        public TaskCancelledException(String message) {
            super(message);
        }
    }

    /**
     * Класс для передачи обновлений прогресса через SwingWorker.
     */
    public static class ProgressUpdate {
        private final int percent;
        private final String message;
        private final long timestamp;

        public ProgressUpdate(int percent, String message) {
            this.percent = Math.max(0, Math.min(100, percent));
            this.message = message != null ? message : "";
            this.timestamp = System.currentTimeMillis();
        }

        public int getPercent() {
            return percent;
        }

        public String getMessage() {
            return message;
        }

        public long getTimestamp() {
            return timestamp;
        }

        @Override
        public String toString() {
            return String.format("ProgressUpdate{percent=%d%%, message='%s'}", percent, message);
        }
    }
}