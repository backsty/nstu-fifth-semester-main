package pw.ns2030.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Аннотация для переименования поля при сериализации в JSON.
 * Позволяет задать альтернативное имя поля в результирующем JSON.
 * 
 * Пример:
 * @JsonField("full_name")
 * private String name; // в JSON будет "full_name": "John"
 */
// указывает, что аннотацию @JsonIgnore можно применять только к полям класса (переменным), но не к методам, классам или конструкторам.
@Target(ElementType.FIELD)
// указывает, что аннотация должна сохраняться во время выполнения программы, чтобы к ней можно было обратиться через рефлексию.
@Retention(RetentionPolicy.RUNTIME)
public @interface JsonField {
    
    /**
     * Имя поля в JSON.
     * Если не указано, используется оригинальное имя поля.
     */
    String value();
    
    /**
     * Обязательность поля при десериализации.
     * Если true и поле отсутствует в JSON, будет выброшено исключение.
     */
    boolean required() default false;
    
    /**
     * Порядок сериализации полей (меньше = раньше).
     * Используется для контроля порядка полей в JSON.
     */
    int order() default 0;
}