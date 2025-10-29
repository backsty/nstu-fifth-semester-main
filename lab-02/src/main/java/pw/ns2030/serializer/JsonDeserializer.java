package pw.ns2030.serializer;

import pw.ns2030.annotations.JsonField;
import pw.ns2030.annotations.JsonIgnore;
import pw.ns2030.annotations.JsonSerializable;
import pw.ns2030.exceptions.JsonException;

import java.lang.reflect.Array;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.*;

/**
 * Класс для десериализации JSON в Java-объекты.
 * Использует рефлексию для создания объектов и заполнения полей.
 */
public class JsonDeserializer {
    
    private final ReferenceTracker referenceTracker;
    
    public JsonDeserializer() {
        this.referenceTracker = new ReferenceTracker();
    }
    
    /**
     * Десериализует JSON строку в объект указанного класса.
     * @param json JSON строка
     * @param clazz класс результирующего объекта
     * @return десериализованный объект
     */
    public <T> T deserialize(String json, Class<T> clazz) {
        if (json == null || json.trim().isEmpty()) {
            throw new JsonException.DeserializationException("JSON строка не может быть пустой");
        }
        
        if (json.trim().equals("null")) {
            return null;
        }
        
        try {
            referenceTracker.clear();
            Object result = deserializeValue(json.trim(), clazz);
            return clazz.cast(result);
        } catch (JsonException e) {
            // Если это уже JsonException, просто перебрасываем
            throw e;
        } catch (Exception e) {
            // Любые другие исключения оборачиваем в JsonException
            throw new JsonException.DeserializationException("Ошибка десериализации", e);
        }
    }
    
    /**
     * Десериализует JSON по имени класса.
     * @param json JSON строка
     * @param className имя класса
     * @return десериализованный объект
     */
    public Object deserializeByClassName(String json, String className) {
        try {
            Class<?> clazz = Class.forName(className);
            return deserialize(json, clazz);
        } catch (ClassNotFoundException e) {
            throw new JsonException.DeserializationException("Класс не найден: " + className, e);
        }
    }
    
    /**
     * Основной метод десериализации значения.
     */
    private Object deserializeValue(String json, Type type) throws Exception {
        json = json.trim();
        
        if (json.equals("null")) {
            return null;
        }
        
        // Проверяем на ссылку
        if (ReferenceTracker.isReference(json)) {
            String refId = ReferenceTracker.extractReferenceId(json);
            Object referencedObject = referenceTracker.getObjectById(refId);
            if (referencedObject == null) {
                throw new JsonException.DeserializationException("Ссылка не найдена: " + refId);
            }
            return referencedObject;
        }
        
        // Определяем тип
        Class<?> clazz = (type instanceof Class) ? (Class<?>) type : (Class<?>) ((ParameterizedType) type).getRawType();
        
        // Примитивы и строки
        if (isPrimitiveType(clazz)) {
            return deserializePrimitive(json, clazz);
        }
        
        // Массивы
        if (json.startsWith("[")) {
            return deserializeArray(json, type);
        }
        
        // Объекты
        if (json.startsWith("{")) {
            return deserializeObject(json, clazz);
        }
        
        throw new JsonException.DeserializationException("Неподдерживаемый формат JSON: " + json);
    }
    
    /**
     * Десериализация примитивных типов.
     */
    private Object deserializePrimitive(String json, Class<?> clazz) {
        try {
            if (clazz == String.class) {
                if (!json.startsWith("\"") || !json.endsWith("\"")) {
                    throw new JsonException.DeserializationException("Строка должна быть в кавычках: " + json);
                }
                return unescapeString(json.substring(1, json.length() - 1));
            }
            
            if (clazz == int.class || clazz == Integer.class) {
                return Integer.parseInt(json);
            }
            
            if (clazz == long.class || clazz == Long.class) {
                return Long.parseLong(json);
            }
            
            if (clazz == double.class || clazz == Double.class) {
                return Double.parseDouble(json);
            }
            
            if (clazz == float.class || clazz == Float.class) {
                return Float.parseFloat(json);
            }
            
            if (clazz == boolean.class || clazz == Boolean.class) {
                return Boolean.parseBoolean(json);
            }
            
            if (clazz == byte.class || clazz == Byte.class) {
                return Byte.parseByte(json);
            }
            
            if (clazz == short.class || clazz == Short.class) {
                return Short.parseShort(json);
            }
            
            if (clazz == char.class || clazz == Character.class) {
                String str = unescapeString(json.substring(1, json.length() - 1));
                return str.length() > 0 ? str.charAt(0) : '\0';
            }
            
            throw new JsonException.DeserializationException("Неподдерживаемый примитивный тип: " + clazz);
            
        } catch (NumberFormatException e) {
            throw new JsonException.DeserializationException("Некорректное числовое значение: " + json, e);
        }
    }
    
    /**
     * Десериализация массивов и коллекций.
     */
    private Object deserializeArray(String json, Type type) throws Exception {
        List<String> elements = parseJsonArray(json);
        
        if (type instanceof Class && ((Class<?>) type).isArray()) {
            // Обычный массив
            Class<?> arrayClass = (Class<?>) type;
            Class<?> componentType = arrayClass.getComponentType();
            
            Object array = Array.newInstance(componentType, elements.size());
            for (int i = 0; i < elements.size(); i++) {
                Object element = deserializeValue(elements.get(i), componentType);
                Array.set(array, i, element);
            }
            return array;
        }
        
        if (type instanceof ParameterizedType) {
            // Коллекция с generic типом
            ParameterizedType pt = (ParameterizedType) type;
            Class<?> rawType = (Class<?>) pt.getRawType();
            Type elementType = pt.getActualTypeArguments()[0];
            
            Collection<Object> collection;
            if (List.class.isAssignableFrom(rawType)) {
                collection = new ArrayList<>();
            } else if (Set.class.isAssignableFrom(rawType)) {
                collection = new HashSet<>();
            } else {
                throw new JsonException.DeserializationException("Неподдерживаемый тип коллекции: " + rawType);
            }
            
            for (String element : elements) {
                Object obj = deserializeValue(element, elementType);
                collection.add(obj);
            }
            
            return collection;
        }
        
        // Обычный List без generic
        List<Object> list = new ArrayList<>();
        for (String element : elements) {
            Object obj = deserializeValue(element, Object.class);
            list.add(obj);
        }
        return list;
    }
    
    /**
     * Десериализация объектов.
     */
    private Object deserializeObject(String json, Class<?> clazz) throws Exception {
        // Проверяем аннотацию
        if (!clazz.isAnnotationPresent(JsonSerializable.class)) {
            throw new JsonException.DeserializationException(
                "Класс не помечен аннотацией @JsonSerializable", clazz.getSimpleName(), null);
        }
        
        Map<String, String> fieldValues = parseJsonObject(json);
        
        // Создаем экземпляр объекта
        Object instance = createInstance(clazz);
        
        // Регистрируем объект по ID (если есть)
        String objectId = fieldValues.get("$id");
        if (objectId != null) {
            objectId = unescapeString(objectId.substring(1, objectId.length() - 1)); // убираем кавычки
            referenceTracker.registerObject(objectId, instance);
        }
        
        // Заполняем поля
        for (Field field : getAllFields(clazz)) {
            if (shouldSkipField(field)) {
                continue;
            }
            
            String fieldName = getFieldName(field);
            String fieldValue = fieldValues.get(fieldName);
            
            if (fieldValue != null) {
                field.setAccessible(true);
                Object value = deserializeValue(fieldValue, field.getGenericType());
                field.set(instance, value);
            } else if (field.isAnnotationPresent(JsonField.class) && 
                      field.getAnnotation(JsonField.class).required()) {
                throw new JsonException.DeserializationException(
                    "Обязательное поле отсутствует: " + fieldName, clazz.getSimpleName(), fieldName);
            }
        }
        
        return instance;
    }
    
    // Вспомогательные методы аналогичны JsonSerializer
    private List<Field> getAllFields(Class<?> clazz) {
        List<Field> fields = new ArrayList<>();
        while (clazz != null) {
            fields.addAll(Arrays.asList(clazz.getDeclaredFields()));
            clazz = clazz.getSuperclass();
        }
        return fields;
    }
    
    private boolean shouldSkipField(Field field) {
        return field.isAnnotationPresent(JsonIgnore.class) || 
               java.lang.reflect.Modifier.isStatic(field.getModifiers()) ||
               java.lang.reflect.Modifier.isTransient(field.getModifiers());
    }
    
    private String getFieldName(Field field) {
        if (field.isAnnotationPresent(JsonField.class)) {
            return field.getAnnotation(JsonField.class).value();
        }
        return field.getName();
    }
    
    private boolean isPrimitiveType(Class<?> clazz) {
        return clazz.isPrimitive() || 
               clazz == String.class ||
               clazz == Integer.class || clazz == Long.class ||
               clazz == Double.class || clazz == Float.class ||
               clazz == Boolean.class || clazz == Byte.class ||
               clazz == Short.class || clazz == Character.class;
    }
    
    private Object createInstance(Class<?> clazz) throws Exception {
        try {
            Constructor<?> constructor = clazz.getDeclaredConstructor();
            constructor.setAccessible(true);
            return constructor.newInstance();
        } catch (Exception e) {
            throw new JsonException.DeserializationException(
                "Не удалось создать экземпляр класса " + clazz.getSimpleName() + 
                ". Убедитесь, что у класса есть конструктор по умолчанию", e);
        }
    }
    
    private String unescapeString(String str) {
        return str.replace("\\\"", "\"")
                  .replace("\\\\", "\\")
                  .replace("\\n", "\n")
                  .replace("\\r", "\r")
                  .replace("\\t", "\t")
                  .replace("\\b", "\b")
                  .replace("\\f", "\f");
    }
    
    // Простой парсер JSON (базовая реализация)
    private List<String> parseJsonArray(String json) {
        // Убираем [ и ]
        json = json.substring(1, json.length() - 1).trim();
        
        if (json.isEmpty()) {
            return new ArrayList<>();
        }
        
        List<String> elements = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        int depth = 0;
        boolean inString = false;
        boolean escaped = false;
        
        for (char c : json.toCharArray()) {
            if (escaped) {
                current.append(c);
                escaped = false;
                continue;
            }
            
            if (c == '\\') {
                escaped = true;
                current.append(c);
                continue;
            }
            
            if (c == '"') {
                inString = !inString;
                current.append(c);
                continue;
            }
            
            if (!inString) {
                if (c == '{' || c == '[') {
                    depth++;
                } else if (c == '}' || c == ']') {
                    depth--;
                } else if (c == ',' && depth == 0) {
                    elements.add(current.toString().trim());
                    current = new StringBuilder();
                    continue;
                }
            }
            
            current.append(c);
        }
        
        if (current.length() > 0) {
            elements.add(current.toString().trim());
        }
        
        return elements;
    }
    
    private Map<String, String> parseJsonObject(String json) {
        // Убираем { и }
        json = json.substring(1, json.length() - 1).trim();
        
        Map<String, String> result = new HashMap<>();
        
        if (json.isEmpty()) {
            return result;
        }
        
        // Простой парсер пар ключ-значение
        List<String> pairs = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        int depth = 0;
        boolean inString = false;
        boolean escaped = false;
        
        for (char c : json.toCharArray()) {
            if (escaped) {
                current.append(c);
                escaped = false;
                continue;
            }
            
            if (c == '\\') {
                escaped = true;
                current.append(c);
                continue;
            }
            
            if (c == '"') {
                inString = !inString;
                current.append(c);
                continue;
            }
            
            if (!inString) {
                if (c == '{' || c == '[') {
                    depth++;
                } else if (c == '}' || c == ']') {
                    depth--;
                } else if (c == ',' && depth == 0) {
                    pairs.add(current.toString().trim());
                    current = new StringBuilder();
                    continue;
                }
            }
            
            current.append(c);
        }
        
        if (current.length() > 0) {
            pairs.add(current.toString().trim());
        }
        
        for (String pair : pairs) {
            int colonIndex = findColonIndex(pair);
            if (colonIndex == -1) {
                throw new JsonException.DeserializationException("Некорректная пара ключ-значение: " + pair);
            }
            
            String key = pair.substring(0, colonIndex).trim();
            String value = pair.substring(colonIndex + 1).trim();
            
            // Убираем кавычки с ключа
            if (key.startsWith("\"") && key.endsWith("\"")) {
                key = key.substring(1, key.length() - 1);
            }
            
            result.put(key, value);
        }
        
        return result;
    }
    
    private int findColonIndex(String pair) {
        boolean inString = false;
        boolean escaped = false;
        
        for (int i = 0; i < pair.length(); i++) {
            char c = pair.charAt(i);
            
            if (escaped) {
                escaped = false;
                continue;
            }
            
            if (c == '\\') {
                escaped = true;
                continue;
            }
            
            if (c == '"') {
                inString = !inString;
                continue;
            }
            
            if (!inString && c == ':') {
                return i;
            }
        }
        
        return -1;
    }
}