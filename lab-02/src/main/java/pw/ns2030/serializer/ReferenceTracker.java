package pw.ns2030.serializer;

import pw.ns2030.exceptions.JsonException;

import java.util.*;

/**
 * Класс для отслеживания ссылок между объектами при сериализации.
 * Предотвращает циклические ссылки и дублирование объектов в JSON.
 */
public class ReferenceTracker {
    
    // Карта: объект -> его ID в JSON
    private final Map<Object, String> objectToId = new IdentityHashMap<>();
    
    // Карта: ID -> объект (для десериализации)
    private final Map<String, Object> idToObject = new HashMap<>();
    
    // Множество объектов, которые сейчас сериализуются (для обнаружения циклов)
    private final Set<Object> currentlySerializing = Collections.newSetFromMap(new IdentityHashMap<>());
    
    // Счетчик для генерации уникальных ID
    private int idCounter = 1;
    
    /**
     * Проверяет, был ли объект уже сериализован.
     * @param obj объект для проверки
     * @return true, если объект уже сериализован
     */
    public boolean isAlreadySerialized(Object obj) {
        if (obj == null) return false;
        return objectToId.containsKey(obj);
    }
    
    /**
     * Получает ID объекта или создает новый, если объект встречается впервые.
     * @param obj объект
     * @return ID объекта в JSON
     */
    public String getOrCreateId(Object obj) {
        if (obj == null) {
            throw new IllegalArgumentException("Объект не может быть null");
        }
        
        String existingId = objectToId.get(obj);
        if (existingId != null) {
            return existingId;
        }
        
        String newId = generateId();
        objectToId.put(obj, newId);
        idToObject.put(newId, obj);
        return newId;
    }
    
    /**
     * Начинает сериализацию объекта. Проверяет на циклические ссылки.
     * @param obj объект для сериализации
     * @throws JsonException.CircularReferenceException если обнаружен цикл
     */
    public void startSerialization(Object obj) {
        if (obj == null) return;
        
        if (currentlySerializing.contains(obj)) {
            throw new JsonException.CircularReferenceException(obj.getClass().getSimpleName());
        }
        
        currentlySerializing.add(obj);
    }
    
    /**
     * Завершает сериализацию объекта.
     * @param obj объект
     */
    public void endSerialization(Object obj) {
        if (obj == null) return;
        currentlySerializing.remove(obj);
    }
    
    /**
     * Получает объект по его ID (для десериализации).
     * @param id ID объекта
     * @return объект или null, если не найден
     */
    public Object getObjectById(String id) {
        return idToObject.get(id);
    }
    
    /**
     * Регистрирует объект с заданным ID (для десериализации).
     * @param id ID
     * @param obj объект
     */
    public void registerObject(String id, Object obj) {
        if (id == null || obj == null) {
            throw new IllegalArgumentException("ID и объект не могут быть null");
        }
        
        idToObject.put(id, obj);
        objectToId.put(obj, id);
    }
    
    /**
     * Очищает все накопленные ссылки.
     * Используется для начала новой сессии сериализации/десериализации.
     */
    public void clear() {
        objectToId.clear();
        idToObject.clear();
        currentlySerializing.clear();
        idCounter = 1;
    }
    
    /**
     * Проверяет, является ли строка ссылкой на объект.
     * @param value строка для проверки
     * @return true, если это ссылка вида {"$ref": "id"}
     */
    public static boolean isReference(String value) {
        if (value == null) return false;
        value = value.trim();
        return value.startsWith("{\"$ref\":") && value.endsWith("}");
    }
    
    /**
     * Извлекает ID из строки-ссылки.
     * @param referenceString строка вида {"$ref": "id"}
     * @return ID объекта
     */
    public static String extractReferenceId(String referenceString) {
        if (!isReference(referenceString)) {
            throw new IllegalArgumentException("Строка не является ссылкой: " + referenceString);
        }
        
        // Парсим {"$ref": "id"} -> извлекаем id
        int start = referenceString.indexOf("\"", referenceString.indexOf(":")) + 1;
        int end = referenceString.lastIndexOf("\"");
        
        if (start <= 0 || end <= start) {
            throw new JsonException.DeserializationException("Некорректный формат ссылки: " + referenceString);
        }
        
        return referenceString.substring(start, end);
    }
    
    /**
     * Создает строку-ссылку по ID.
     * @param id ID объекта
     * @return строка вида {"$ref": "id"}
     */
    public static String createReferenceString(String id) {
        return "{\"$ref\": \"" + id + "\"}";
    }
    
    /**
     * Генерирует уникальный ID для объекта.
     */
    private String generateId() {
        return "ref_" + (idCounter++);
    }
    
    /**
     * Получает статистику по ссылкам (для отладки).
     * @return информация о количестве отслеживаемых объектов
     */
    public String getStatistics() {
        return String.format("Отслеживается объектов: %d, текущая сериализация: %d", 
                objectToId.size(), currentlySerializing.size());
    }
}