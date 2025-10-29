package pw.ns2030.serializer;

import pw.ns2030.annotations.JsonField;
import pw.ns2030.annotations.JsonIgnore;
import pw.ns2030.annotations.JsonSerializable;
import pw.ns2030.exceptions.JsonException;

import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.*;

/**
 * Основной класс для сериализации Java-объектов в JSON.
 * Использует рефлексию и аннотации для настройки процесса сериализации.
 */
public class JsonSerializer {
    
    private final ReferenceTracker referenceTracker;
    private final boolean prettyPrint;
    
    public JsonSerializer() {
        this(false);
    }
    
    public JsonSerializer(boolean prettyPrint) {
        this.referenceTracker = new ReferenceTracker();
        this.prettyPrint = prettyPrint;
    }
    
    /**
     * Сериализует объект в JSON строку.
     * @param obj объект для сериализации
     * @return JSON строка
     */
    public String serialize(Object obj) {
        if (obj == null) {
            return "null";
        }
        
        try {
            referenceTracker.clear();
            return serializeObject(obj, 0);
        } catch (Exception e) {
            throw new JsonException.SerializationException("Не удалось сериализовать объект", e);
        }
    }
    
    /**
     * Сериализует объект по имени его класса.
     * @param className имя класса
     * @param obj объект для сериализации
     * @return JSON строка
     */
    public String serializeByClassName(String className, Object obj) {
        if (obj == null) {
            return "null";
        }
        
        try {
            Class<?> clazz = Class.forName(className);
            if (!clazz.isInstance(obj)) {
                throw new JsonException.SerializationException(
                    "Объект не является экземпляром класса " + className);
            }
            
            return serialize(obj);
        } catch (ClassNotFoundException e) {
            throw new JsonException.SerializationException("Класс не найден: " + className, e);
        }
    }
    
    /**
     * Основной метод сериализации объекта.
     */
    private String serializeObject(Object obj, int depth) throws IllegalAccessException {
        if (obj == null) {
            return "null";
        }
        
        // Примитивы и строки
        if (isPrimitive(obj)) {
            return serializePrimitive(obj);
        }
        
        // Массивы
        if (obj.getClass().isArray()) {
            return serializeArray(obj, depth);
        }
        
        // Коллекции
        if (obj instanceof Collection) {
            return serializeCollection((Collection<?>) obj, depth);
        }
        
        // Map
        if (obj instanceof Map) {
            return serializeMap((Map<?, ?>) obj, depth);
        }
        
        // Пользовательские объекты
        return serializeCustomObject(obj, depth);
    }
    
    /**
     * Сериализация массивов.
     */
    private String serializeArray(Object array, int depth) throws IllegalAccessException {
        int length = Array.getLength(array);
        StringBuilder sb = new StringBuilder();
        
        sb.append("[");
        
        for (int i = 0; i < length; i++) {
            if (i > 0) {
                sb.append(",");
                if (prettyPrint) sb.append(" ");
            }
            
            Object element = Array.get(array, i);
            sb.append(serializeObject(element, depth + 1));
        }
        
        sb.append("]");
        return sb.toString();
    }
    
    /**
     * Сериализация коллекций.
     */
    private String serializeCollection(Collection<?> collection, int depth) throws IllegalAccessException {
        StringBuilder sb = new StringBuilder();
        sb.append("[");
        
        boolean first = true;
        for (Object element : collection) {
            if (!first) {
                sb.append(",");
                if (prettyPrint) sb.append(" ");
            }
            first = false;
            
            sb.append(serializeObject(element, depth + 1));
        }
        
        sb.append("]");
        return sb.toString();
    }
    
    /**
     * Сериализация Map.
     */
    private String serializeMap(Map<?, ?> map, int depth) throws IllegalAccessException {
        StringBuilder sb = new StringBuilder();
        sb.append("{");
        
        if (prettyPrint && !map.isEmpty()) {
            sb.append("\n");
        }
        
        boolean first = true;
        for (Map.Entry<?, ?> entry : map.entrySet()) {
            if (!first) {
                sb.append(",");
                if (prettyPrint) sb.append("\n");
            }
            first = false;
            
            if (prettyPrint) {
                sb.append(getIndent(depth + 1));
            }
            
            // Ключ всегда строка
            String key = entry.getKey().toString();
            sb.append("\"").append(escapeString(key)).append("\":");
            
            if (prettyPrint) sb.append(" ");
            
            sb.append(serializeObject(entry.getValue(), depth + 1));
        }
        
        if (prettyPrint && !map.isEmpty()) {
            sb.append("\n").append(getIndent(depth));
        }
        
        sb.append("}");
        return sb.toString();
    }
    
    /**
     * Сериализация пользовательских объектов.
     */
    private String serializeCustomObject(Object obj, int depth) throws IllegalAccessException {
        Class<?> clazz = obj.getClass();
        
        // Проверяем аннотацию @JsonSerializable
        if (!clazz.isAnnotationPresent(JsonSerializable.class)) {
            throw new JsonException.SerializationException(
                "Класс не помечен аннотацией @JsonSerializable", clazz.getSimpleName(), null);
        }
        
        // ИСПРАВЛЕНИЕ: Проверяем, был ли объект уже сериализован ПЕРЕД началом сериализации
        if (referenceTracker.isAlreadySerialized(obj)) {
            String id = referenceTracker.getOrCreateId(obj);
            return ReferenceTracker.createReferenceString(id);
        }
        
        // Проверяем циклические ссылки
        referenceTracker.startSerialization(obj);
        
        try {
            // Создаем ID для объекта
            String id = referenceTracker.getOrCreateId(obj);
            
            JsonSerializable annotation = clazz.getAnnotation(JsonSerializable.class);
            
            StringBuilder sb = new StringBuilder();
            sb.append("{");
            
            if (prettyPrint) sb.append("\n");
            
            // Добавляем ID объекта
            if (prettyPrint) {
                sb.append(getIndent(depth + 1));
            }
            sb.append("\"$id\":").append(prettyPrint ? " " : "").append("\"").append(id).append("\"");
            
            // Получаем все поля класса
            List<Field> fields = getAllFields(clazz);
            
            // Сортируем поля по порядку (если указан в аннотации)
            fields.sort((f1, f2) -> {
                int order1 = f1.isAnnotationPresent(JsonField.class) ? f1.getAnnotation(JsonField.class).order() : 0;
                int order2 = f2.isAnnotationPresent(JsonField.class) ? f2.getAnnotation(JsonField.class).order() : 0;
                return Integer.compare(order1, order2);
            });
            
            // Сериализуем поля
            for (Field field : fields) {
                if (shouldSkipField(field)) {
                    continue;
                }
                
                field.setAccessible(true);
                Object value = field.get(obj);
                
                // Пропускаем null значения, если указано в аннотации
                if (value == null && !annotation.includeNulls()) {
                    continue;
                }
                
                sb.append(",");
                if (prettyPrint) sb.append("\n").append(getIndent(depth + 1));
                
                // Получаем имя поля (с учетом аннотации @JsonField)
                String fieldName = getFieldName(field);
                sb.append("\"").append(escapeString(fieldName)).append("\":");
                
                if (prettyPrint) sb.append(" ");
                
                sb.append(serializeObject(value, depth + 1));
            }
            
            if (prettyPrint) {
                sb.append("\n").append(getIndent(depth));
            }
            
            sb.append("}");
            
            return sb.toString();
            
        } finally {
            referenceTracker.endSerialization(obj);
        }
    }
    
    /**
     * Получает все поля класса, включая унаследованные.
     */
    private List<Field> getAllFields(Class<?> clazz) {
        List<Field> fields = new ArrayList<>();
        
        while (clazz != null) {
            fields.addAll(Arrays.asList(clazz.getDeclaredFields()));
            clazz = clazz.getSuperclass();
        }
        
        return fields;
    }
    
    /**
     * Определяет, нужно ли пропустить поле при сериализации.
     */
    private boolean shouldSkipField(Field field) {
        // Пропускаем поля с аннотацией @JsonIgnore
        if (field.isAnnotationPresent(JsonIgnore.class)) {
            return true;
        }
        
        // Пропускаем статические и финальные поля
        int modifiers = field.getModifiers();
        if (Modifier.isStatic(modifiers) || Modifier.isTransient(modifiers)) {
            return true;
        }
        
        return false;
    }
    
    /**
     * Получает имя поля для JSON (с учетом аннотации @JsonField).
     */
    private String getFieldName(Field field) {
        if (field.isAnnotationPresent(JsonField.class)) {
            JsonField annotation = field.getAnnotation(JsonField.class);
            return annotation.value();
        }
        return field.getName();
    }
    
    /**
     * Проверяет, является ли объект примитивным типом.
     */
    private boolean isPrimitive(Object obj) {
        return obj instanceof String || 
               obj instanceof Number || 
               obj instanceof Boolean || 
               obj instanceof Character;
    }
    
    /**
     * Сериализует примитивные типы.
     */
    private String serializePrimitive(Object obj) {
        if (obj instanceof String) {
            return "\"" + escapeString((String) obj) + "\"";
        }
        return obj.toString();
    }
    
    /**
     * Экранирует специальные символы в строке.
     */
    private String escapeString(String str) {
        return str.replace("\\", "\\\\")
                  .replace("\"", "\\\"")
                  .replace("\n", "\\n")
                  .replace("\r", "\\r")
                  .replace("\t", "\\t")
                  .replace("\b", "\\b")
                  .replace("\f", "\\f");
    }
    
    /**
     * Создает отступ для pretty print.
     */
    private String getIndent(int depth) {
        if (!prettyPrint) return "";
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < depth * 2; i++) {
            sb.append(" ");
        }
        return sb.toString();
    }
    
    /**
     * Получает трекер ссылок (для тестирования).
     */
    public ReferenceTracker getReferenceTracker() {
        return referenceTracker;
    }
}