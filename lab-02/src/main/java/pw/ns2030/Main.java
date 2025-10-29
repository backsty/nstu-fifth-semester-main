package pw.ns2030;

import pw.ns2030.models.Company;
import pw.ns2030.models.Department;
import pw.ns2030.models.Person;
import pw.ns2030.serializer.JsonDeserializer;
import pw.ns2030.serializer.JsonSerializer;
import pw.ns2030.exceptions.JsonException;

import java.util.Arrays;
import java.util.List;

/**
 * Демонстрационное приложение для тестирования JSON-сериализатора.
 * Показывает работу с рефлексией, аннотациями и ссылками.
 */
public class Main {
    
    public static void main(String[] args) {
        System.out.println("=== JSON Сериализатор с Рефлексией ===\n");
        
        // Тестируем различные сценарии
        testBasicSerialization();
        testReferenceSerialization();
        testArraySerialization();
        testCircularReferencePrevention();
        testDeserializationByClassName();
        testComplexObjectGraph();
        testJsonModificationBeforeDeserialization();
    }
    
    /**
     * Тест 1: Базовая сериализация простого объекта.
     */
    private static void testBasicSerialization() {
        System.out.println("--- Тест 1: Базовая сериализация ---");
        
        try {
            JsonSerializer serializer = new JsonSerializer(true); // pretty print
            
            Person person = new Person("Иван Петров", 30, "ivan@example.com");
            person.setPassword("secret123"); // должно игнорироваться
            
            String json = serializer.serialize(person);
            System.out.println("Сериализованный объект:");
            System.out.println(json);
            
            // Десериализация
            JsonDeserializer deserializer = new JsonDeserializer();
            Person restored = deserializer.deserialize(json, Person.class);
            
            System.out.println("Восстановленный объект: " + restored);
            System.out.println("Пароль (должен быть null): " + restored.getPassword());
            System.out.println();
            
        } catch (Exception e) {
            System.err.println("Ошибка: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Тест 2: Сериализация объектов со ссылками.
     */
    private static void testReferenceSerialization() {
        System.out.println("--- Тест 2: Сериализация ссылок ---");
        
        try {
            JsonSerializer serializer = new JsonSerializer(true);
            
            // Создаем компанию
            Company company = new Company("ТехКорп", "Москва, ул. Ленина, 1", 2020);
            company.setRevenue(1000000.0); // должно игнорироваться
            
            // Создаем сотрудников одной компании
            Person john = new Person("Джон Смит", 35, "john@techcorp.com");
            Person jane = new Person("Джейн Доу", 28, "jane@techcorp.com");
            
            // Связываем с компанией
            company.addEmployee(john);
            company.addEmployee(jane);
            
            String json = serializer.serialize(company);
            System.out.println("Компания с сотрудниками:");
            System.out.println(json);
            
            // Десериализация
            JsonDeserializer deserializer = new JsonDeserializer();
            Company restored = deserializer.deserialize(json, Company.class);
            
            System.out.println("Восстановленная компания: " + restored);
            System.out.println("Количество сотрудников: " + restored.getEmployees().size());
            System.out.println();
            
        } catch (Exception e) {
            System.err.println("Ошибка: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Тест 3: Сериализация массивов и коллекций.
     */
    private static void testArraySerialization() {
        System.out.println("--- Тест 3: Массивы и коллекции ---");
        
        try {
            JsonSerializer serializer = new JsonSerializer(true);
            
            // Создаем отдел с массивом проектов
            Department dept = new Department("IT Отдел");
            dept.setProjects(new String[]{"Проект A", "Проект B", "Проект C"});
            dept.setBudget(500000.0);
            dept.setDepartmentCode(123); // должно игнорироваться
            
            // Добавляем сотрудников
            Person manager = new Person("Менеджер Иванов", 40);
            Person dev1 = new Person("Разработчик 1", 25);
            Person dev2 = new Person("Разработчик 2", 27);
            
            dept.setManager(manager);
            dept.addEmployee(dev1);
            dept.addEmployee(dev2);
            
            String json = serializer.serialize(dept);
            System.out.println("Отдел с массивами:");
            System.out.println(json);
            
            // Тест сериализации массива объектов
            Person[] peopleArray = {manager, dev1, dev2, manager}; // manager повторяется
            String arrayJson = serializer.serialize(peopleArray);
            System.out.println("Массив людей (с повтором):");
            System.out.println(arrayJson);
            System.out.println();
            
        } catch (Exception e) {
            System.err.println("Ошибка: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Тест 4: Предотвращение циклических ссылок.
     */
    private static void testCircularReferencePrevention() {
        System.out.println("--- Тест 4: Циклические ссылки ---");
        
        try {
            JsonSerializer serializer = new JsonSerializer(true);
            
            // Создаем родителя и ребенка
            Person parent = new Person("Родитель", 45);
            Person child = new Person("Ребенок", 15);
            
            // Создаем циклическую ссылку
            parent.addChild(child); // parent -> child
            // child.parent уже установлен в addChild
            
            String json = serializer.serialize(parent);
            System.out.println("Объект с циклической ссылкой:");
            System.out.println(json);
            
            // Десериализация
            JsonDeserializer deserializer = new JsonDeserializer();
            Person restored = deserializer.deserialize(json, Person.class);
            
            System.out.println("Восстановленный родитель: " + restored);
            System.out.println("Количество детей: " + restored.getChildren().size());
            if (!restored.getChildren().isEmpty()) {
                Person restoredChild = restored.getChildren().get(0);
                System.out.println("Ребенок: " + restoredChild);
                System.out.println("Родитель ребенка тот же объект: " + (restoredChild.getParent() == restored));
            }
            System.out.println();
            
        } catch (JsonException.CircularReferenceException e) {
            System.out.println("Циклическая ссылка корректно обнаружена: " + e.getMessage());
            System.out.println();
        } catch (Exception e) {
            System.err.println("Неожиданная ошибка: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Тест 5: Десериализация по имени класса.
     */
    private static void testDeserializationByClassName() {
        System.out.println("--- Тест 5: Десериализация по имени класса ---");
        
        try {
            JsonSerializer serializer = new JsonSerializer();
            JsonDeserializer deserializer = new JsonDeserializer();
            
            Person person = new Person("Тестовый пользователь", 33);
            
            // Сериализация по имени класса
            String className = "pw.ns2030.models.Person";
            String json = serializer.serializeByClassName(className, person);
            System.out.println("JSON: " + json);
            
            // Десериализация по имени класса
            Object restored = deserializer.deserializeByClassName(json, className);
            System.out.println("Восстановленный объект: " + restored);
            System.out.println("Класс объекта: " + restored.getClass().getName());
            System.out.println();
            
        } catch (Exception e) {
            System.err.println("Ошибка: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Тест 6: Сложный граф объектов.
     */
    private static void testComplexObjectGraph() {
        System.out.println("--- Тест 6: Сложный граф объектов ---");
        
        try {
            JsonSerializer serializer = new JsonSerializer(true);
            
            // Создаем компанию
            Company company = new Company("МегаКорп", "СПб, Невский пр., 100", 2010);
            
            // Создаем отделы
            Department itDept = new Department("IT", null, company);
            Department hrDept = new Department("HR", null, company);
            
            // Создаем людей
            Person ceo = new Person("Генеральный директор", 50);
            Person itManager = new Person("IT Менеджер", 40);
            Person hrManager = new Person("HR Менеджер", 38);
            Person developer = new Person("Разработчик", 28);
            
            // Устанавливаем связи
            company.addEmployee(ceo);
            
            itDept.setManager(itManager);
            hrDept.setManager(hrManager);
            
            itDept.addEmployee(developer);
            itDept.setProjects(new String[]{"CRM", "ERP", "Mobile App"});
            itDept.setBudget(750000.0);
            
            hrDept.setProjects(new String[]{"Recruiting", "Training"});
            hrDept.setBudget(200000.0);
            
            company.addDepartment(itDept);
            company.addDepartment(hrDept);
            
            // Создаем семейные связи
            Person child1 = new Person("Ребенок 1", 12);
            Person child2 = new Person("Ребенок 2", 10);
            ceo.addChild(child1);
            ceo.addChild(child2);
            
            String json = serializer.serialize(company);
            System.out.println("Сложный граф объектов:");
            System.out.println(json);
            
            // Проверяем размер JSON
            System.out.println("Размер JSON: " + json.length() + " символов");
            System.out.println("Статистика трекера: " + serializer.getReferenceTracker().getStatistics());
            System.out.println();
            
        } catch (Exception e) {
            System.err.println("Ошибка: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Тест 7: Модификация JSON перед десериализацией.
     * Демонстрирует, что изменения в JSON отражаются на восстановленном объекте.
     */
    private static void testJsonModificationBeforeDeserialization() {
        System.out.println("--- Тест 7: Модификация JSON перед десериализацией ---");
        
        try {
            JsonSerializer serializer = new JsonSerializer(true);
            JsonDeserializer deserializer = new JsonDeserializer();
            
            // Создание исходного объекта
            Person originalPerson = new Person("Иван Петров", 30, "ivan@example.com");
            originalPerson.setPassword("secret123");
            
            System.out.println("Исходный объект: " + originalPerson);
            
            // Сериализация в JSON
            String originalJson = serializer.serialize(originalPerson);
            System.out.println("Исходный JSON:");
            System.out.println(originalJson);
            
            // изменение возраста
            String modifiedJson = originalJson.replace("\"age\": 30", "\"age\": 45");
            
            // изменение имени
            modifiedJson = modifiedJson.replace("\"full_name\": \"Иван Петров\"", 
                                            "\"full_name\": \"Иван Сидоров\"");
            
            // добавление email, если его не было
            if (!modifiedJson.contains("email_address")) {
                modifiedJson = modifiedJson.replace("\"age\": 45,", 
                    "\"age\": 45,\n  \"email_address\": \"ivan.sidorov@newcompany.com\",");
            } else {
                // Или изменение существующего email
                modifiedJson = modifiedJson.replace("ivan@example.com", "ivan.sidorov@newcompany.com");
            }
            
            System.out.println("\nМодифицированный JSON:");
            System.out.println(modifiedJson);
            
            // Десериализация модифицированного JSON
            Person modifiedPerson = deserializer.deserialize(modifiedJson, Person.class);
            
            System.out.println("\nВосстановленный объект после модификации JSON:");
            System.out.println(modifiedPerson);
            
            // Сравнение значений
            System.out.println("\n=== СРАВНЕНИЕ ЗНАЧЕНИЙ ===");
            System.out.println("Имя:");
            System.out.println("  Исходное: " + originalPerson.getName());
            System.out.println("  После модификации: " + modifiedPerson.getName());
            System.out.println("  Изменилось: " + !originalPerson.getName().equals(modifiedPerson.getName()));
            
            System.out.println("Возраст:");
            System.out.println("  Исходный: " + originalPerson.getAge());
            System.out.println("  После модификации: " + modifiedPerson.getAge());
            System.out.println("  Изменился: " + (originalPerson.getAge() != modifiedPerson.getAge()));
            
            System.out.println("Email:");
            System.out.println("  Исходный: " + originalPerson.getEmail());
            System.out.println("  После модификации: " + modifiedPerson.getEmail());
            System.out.println("  Изменился: " + !originalPerson.getEmail().equals(modifiedPerson.getEmail()));
            
            System.out.println("Пароль (должен остаться null из-за @JsonIgnore):");
            System.out.println("  Исходный: " + originalPerson.getPassword());
            System.out.println("  После модификации: " + modifiedPerson.getPassword());

            // Демонстрация того, что объекты не равны
            System.out.println("\nОбъекты равны: " + originalPerson.equals(modifiedPerson));

            System.out.println();
            
        } catch (Exception e) {
            System.err.println("Ошибка: " + e.getMessage());
            e.printStackTrace();
        }
    }
}