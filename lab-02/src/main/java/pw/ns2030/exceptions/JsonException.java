package pw.ns2030.exceptions;

/**
 * Базовое исключение для ошибок JSON-сериализации/десериализации.
 */
public class JsonException extends RuntimeException {
    
    private final String className;
    private final String fieldName;
    
    public JsonException(String message) {
        super(message);
        this.className = null;
        this.fieldName = null;
    }
    
    public JsonException(String message, Throwable cause) {
        super(message, cause);
        this.className = null;
        this.fieldName = null;
    }
    
    public JsonException(String message, String className, String fieldName) {
        super(formatMessage(message, className, fieldName));
        this.className = className;
        this.fieldName = fieldName;
    }
    
    public JsonException(String message, String className, String fieldName, Throwable cause) {
        super(formatMessage(message, className, fieldName), cause);
        this.className = className;
        this.fieldName = fieldName;
    }
    
    private static String formatMessage(String message, String className, String fieldName) {
        StringBuilder sb = new StringBuilder(message);
        if (className != null) {
            sb.append(" (класс: ").append(className);
            if (fieldName != null) {
                sb.append(", поле: ").append(fieldName);
            }
            sb.append(")");
        }
        return sb.toString();
    }
    
    public String getClassName() {
        return className;
    }
    
    public String getFieldName() {
        return fieldName;
    }
    
    /**
     * Исключение для ошибок сериализации.
     */
    public static class SerializationException extends JsonException {
        public SerializationException(String message) {
            super("Ошибка сериализации: " + message);
        }
        
        public SerializationException(String message, String className, String fieldName) {
            super("Ошибка сериализации: " + message, className, fieldName);
        }
        
        public SerializationException(String message, Throwable cause) {
            super("Ошибка сериализации: " + message, cause);
        }
    }
    
    /**
     * Исключение для ошибок десериализации.
     */
    public static class DeserializationException extends JsonException {
        public DeserializationException(String message) {
            super("Ошибка десериализации: " + message);
        }
        
        public DeserializationException(String message, String className, String fieldName) {
            super("Ошибка десериализации: " + message, className, fieldName);
        }
        
        public DeserializationException(String message, Throwable cause) {
            super("Ошибка десериализации: " + message, cause);
        }
    }
    
    /**
     * Исключение для циклических ссылок.
     */
    public static class CircularReferenceException extends JsonException {
        public CircularReferenceException(String className) {
            super("Обнаружена циклическая ссылка в объекте класса: " + className, className, null);
        }
    }
}