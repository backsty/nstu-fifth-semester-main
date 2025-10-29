package pw.ns2030.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Аннотация для исключения поля из процесса сериализации/десериализации.
 * Поля с этой аннотацией будут полностью игнорироваться.
 * 
 * Пример:
 * @JsonIgnore
 * private String password; // не попадет в JSON
 */
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
public @interface JsonIgnore {
    
    /**
     * Причина игнорирования поля (для документации).
     */
    String reason() default "Excluded from serialization";
}