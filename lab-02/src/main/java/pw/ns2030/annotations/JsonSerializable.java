package pw.ns2030.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Маркер-аннотация для классов, которые могут быть сериализованы в JSON.
 * Классы без этой аннотации не будут обрабатываться сериализатором.
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface JsonSerializable {
    
    /**
     * Указывает, включать ли null-поля в JSON.
     * По умолчанию null-поля включаются.
     */
    boolean includeNulls() default true;
    
    /**
     * Комментарий для класса в JSON (опционально).
     */
    String comment() default "";
}