package pw.ns2030.models;

import pw.ns2030.annotations.JsonField;
import pw.ns2030.annotations.JsonIgnore;
import pw.ns2030.annotations.JsonSerializable;

import java.util.List;
import java.util.ArrayList;
import java.util.Objects;

/**
 * Базовая модель Person для демонстрации сериализации.
 * Включает аннотации для настройки JSON-вывода.
 */
@JsonSerializable(includeNulls = false)
public class Person {
    
    @JsonField(value = "full_name", order = 1)
    private String name;
    
    @JsonField(value = "age", order = 2)
    private int age;
    
    @JsonField(value = "email_address", order = 3)
    private String email;
    
    @JsonIgnore(reason = "Конфиденциальная информация")
    private String password;
    
    @JsonField(value = "company", order = 4)
    private Company company;
    
    @JsonField(value = "children", order = 5)
    private List<Person> children;
    
    @JsonField(value = "parent", order = 6)
    private Person parent;
    
    // Конструктор по умолчанию (обязателен для десериализации)
    public Person() {
        this.children = new ArrayList<>();
    }
    
    public Person(String name, int age) {
        this();
        this.name = name;
        this.age = age;
    }
    
    public Person(String name, int age, String email) {
        this(name, age);
        this.email = email;
    }
    
    public Person(String name, int age, Company company) {
        this(name, age);
        this.company = company;
    }
    
    // Геттеры и сеттеры
    public String getName() {
        return name;
    }
    
    public void setName(String name) {
        this.name = name;
    }
    
    public int getAge() {
        return age;
    }
    
    public void setAge(int age) {
        this.age = age;
    }
    
    public String getEmail() {
        return email;
    }
    
    public void setEmail(String email) {
        this.email = email;
    }
    
    public String getPassword() {
        return password;
    }
    
    public void setPassword(String password) {
        this.password = password;
    }
    
    public Company getCompany() {
        return company;
    }
    
    public void setCompany(Company company) {
        this.company = company;
    }
    
    public List<Person> getChildren() {
        return children;
    }
    
    public void setChildren(List<Person> children) {
        this.children = children != null ? children : new ArrayList<>();
    }
    
    public Person getParent() {
        return parent;
    }
    
    public void setParent(Person parent) {
        this.parent = parent;
    }
    
    // Удобные методы для работы с детьми
    public void addChild(Person child) {
        if (child != null && !this.children.contains(child)) {
            this.children.add(child);
            child.setParent(this);
        }
    }
    
    public void removeChild(Person child) {
        if (child != null && this.children.remove(child)) {
            child.setParent(null);
        }
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Person person = (Person) o;
        return age == person.age && 
               Objects.equals(name, person.name) && 
               Objects.equals(email, person.email);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(name, age, email);
    }
    
    @Override
    public String toString() {
        return String.format("Person{name='%s', age=%d, email='%s', company=%s, children=%d}", 
                name, age, email, 
                company != null ? company.getName() : "null",
                children != null ? children.size() : 0);
    }
}