# Лабораторная работа 2: JSON-сериализатор с рефлексией

## Описание

Универсальный JSON-сериализатор/десериализатор, использующий рефлексию и аннотации для работы с объектами произвольных классов.

## Основные возможности

- ✅ **Рефлексия**: работа с любыми Java-классами через `Class<?>` или имя класса
- ✅ **Аннотации**: настройка сериализации через `@JsonSerializable`, `@JsonField`, `@JsonIgnore`
- ✅ **Ссылки**: сериализация объектов со ссылками (`$id` и `$ref`)
- ✅ **Массивы**: поддержка массивов и коллекций объектов
- ✅ **Циклические ссылки**: автоматическое обнаружение и предотвращение
- ✅ **Pretty Print**: форматированный вывод JSON

## Архитектура

### Основные компоненты:

1. **JsonSerializer** - основной сериализатор
2. **JsonDeserializer** - десериализатор  
3. **ReferenceTracker** - отслеживание ссылок
4. **Аннотации** - настройка поведения

### Аннотации:

- `@JsonSerializable` - маркер класса для сериализации
- `@JsonField("name")` - переименование поля в JSON
- `@JsonIgnore` - исключение поля из сериализации

## Примеры использования

### Базовая сериализация:

```java
@JsonSerializable
class Person {
    @JsonField("full_name")
    private String name;
    
    @JsonIgnore
    private String password;
}

JsonSerializer serializer = new JsonSerializer(true);
Person person = new Person("John", 25);
String json = serializer.serialize(person);
```

### Сериализация со ссылками:

```java
Company company = new Company("TechCorp");
Person john = new Person("John", company);
Person jane = new Person("Jane", company); // та же компания

// Результат:
// [
//   {"$id": "ref_1", "full_name": "John", "company": {"$id": "ref_2", "name": "TechCorp"}},
//   {"$id": "ref_3", "full_name": "Jane", "company": {"$ref": "ref_2"}}
// ]
```

### Десериализация по имени класса:

```java
JsonDeserializer deserializer = new JsonDeserializer();
Object restored = deserializer.deserializeByClassName(json, "com.example.Person");
```

## Запуск

```bash
# Компиляция
./gradlew build

# Запуск демонстрации
./gradlew run

# Создание JAR
./gradlew jar

# Тесты
./gradlew test
```

## Структура проекта

```
lab-02/
├── src/main/java/pw/ns2030/
│   ├── Main.java                 # Демонстрация
│   ├── annotations/              # Аннотации
│   │   ├── JsonSerializable.java
│   │   ├── JsonField.java
│   │   └── JsonIgnore.java
│   ├── serializer/               # Основная логика
│   │   ├── JsonSerializer.java
│   │   ├── JsonDeserializer.java
│   │   └── ReferenceTracker.java
│   ├── models/                   # Тестовые модели
│   │   ├── Person.java
│   │   ├── Company.java
│   │   └── Department.java
│   └── exceptions/               # Исключения
│       └── JsonException.java
├── build.gradle.kts
├── settings.gradle.kts
├── plantUML.puml                 # UML диаграмма
└── README.md
```

## Технические детали

### Рефлексия:
- `Class.forName()` - получение класса по имени
- `Field.getDeclaredFields()` - анализ полей
- `Constructor.newInstance()` - создание объектов
- `Field.get/set()` - доступ к полям

### Обработка ссылок:
- `IdentityHashMap` для точного сравнения объектов
- Генерация уникальных ID для объектов
- Замена повторов на `{"$ref": "id"}`

### Поддерживаемые типы:
- Примитивы и их обертки
- Строки
- Массивы и коллекции
- Пользовательские объекты с аннотациями

## Особенности реализации

- **Циклические ссылки**: обнаружение через стек сериализации
- **Null-значения**: настройка через аннотацию `includeNulls`
- **Порядок полей**: контроль через параметр `order`
- **Валидация**: проверка на этапе сериализации/десериализации

## Ограничения

- Требуется конструктор по умолчанию для десериализации
- Поддержка только аннотированных классов (`@JsonSerializable`)
- Простой JSON-парсер (не полная реализация RFC)

## Автор

Студент группы АВТ-343 Шаламов А.Е. - Лабораторная работа по рефлексии и аннотациям