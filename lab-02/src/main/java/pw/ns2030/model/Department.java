package pw.ns2030.models;

import pw.ns2030.annotations.JsonField;
import pw.ns2030.annotations.JsonIgnore;
import pw.ns2030.annotations.JsonSerializable;

import java.util.List;
import java.util.ArrayList;
import java.util.Objects;

/**
 * Модель Department для демонстрации сложных связей и массивов ссылок.
 */
@JsonSerializable(includeNulls = false)
public class Department {
    
    @JsonField(value = "dept_name", order = 1)
    private String name;
    
    @JsonField(value = "manager", order = 2)
    private Person manager;
    
    @JsonField(value = "employees", order = 3)
    private List<Person> employees;
    
    @JsonField(value = "company", order = 4)
    private Company company;
    
    @JsonField(value = "budget", order = 5)
    private Double budget;
    
    @JsonField(value = "projects", order = 6)
    private String[] projects; // Массив строк для демонстрации
    
    @JsonIgnore(reason = "Служебная информация")
    private int departmentCode;
    
    // Конструктор по умолчанию
    public Department() {
        this.employees = new ArrayList<>();
        this.projects = new String[0];
    }
    
    public Department(String name) {
        this();
        this.name = name;
    }
    
    public Department(String name, Person manager) {
        this(name);
        this.manager = manager;
    }
    
    public Department(String name, Person manager, Company company) {
        this(name, manager);
        this.company = company;
    }
    
    // Геттеры и сеттеры
    public String getName() {
        return name;
    }
    
    public void setName(String name) {
        this.name = name;
    }
    
    public Person getManager() {
        return manager;
    }
    
    public void setManager(Person manager) {
        this.manager = manager;
    }
    
    public List<Person> getEmployees() {
        return employees;
    }
    
    public void setEmployees(List<Person> employees) {
        this.employees = employees != null ? employees : new ArrayList<>();
    }
    
    public Company getCompany() {
        return company;
    }
    
    public void setCompany(Company company) {
        this.company = company;
    }
    
    public Double getBudget() {
        return budget;
    }
    
    public void setBudget(Double budget) {
        this.budget = budget;
    }
    
    public String[] getProjects() {
        return projects;
    }
    
    public void setProjects(String[] projects) {
        this.projects = projects != null ? projects : new String[0];
    }
    
    public int getDepartmentCode() {
        return departmentCode;
    }
    
    public void setDepartmentCode(int departmentCode) {
        this.departmentCode = departmentCode;
    }
    
    // Удобные методы для работы с сотрудниками
    public void addEmployee(Person employee) {
        if (employee != null && !this.employees.contains(employee)) {
            this.employees.add(employee);
            // // Если сотрудника еще нет в компании, добавляем
            // if (this.company != null && !this.company.getEmployees().contains(employee)) {
            //     this.company.addEmployee(employee);
            // }
        }
    }
    
    public void removeEmployee(Person employee) {
        if (employee != null) {
            this.employees.remove(employee);
        }
    }
    
    // Удобные методы для работы с проектами
    public void addProject(String project) {
        if (project != null && !project.trim().isEmpty()) {
            String[] newProjects = new String[projects.length + 1];
            System.arraycopy(projects, 0, newProjects, 0, projects.length);
            newProjects[projects.length] = project;
            this.projects = newProjects;
        }
    }
    
    public void removeProject(String project) {
        if (project == null) return;
        
        List<String> projectList = new ArrayList<>();
        for (String p : projects) {
            if (!Objects.equals(p, project)) {
                projectList.add(p);
            }
        }
        this.projects = projectList.toArray(new String[0]);
    }
    
    /**
     * Получает общий размер команды (менеджер + сотрудники).
     */
    public int getTeamSize() {
        int size = employees.size();
        if (manager != null) {
            size += 1;
        }
        return size;
    }
    
    /**
     * Проверяет, работает ли человек в этом отделе.
     */
    public boolean hasEmployee(Person person) {
        if (person == null) return false;
        return Objects.equals(manager, person) || employees.contains(person);
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Department that = (Department) o;
        return Objects.equals(name, that.name) && 
               Objects.equals(company, that.company);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(name, company);
    }
    
    @Override
    public String toString() {
        return String.format("Department{name='%s', manager=%s, employees=%d, projects=%d, budget=%.2f}", 
                name, 
                manager != null ? manager.getName() : "null",
                employees != null ? employees.size() : 0,
                projects != null ? projects.length : 0,
                budget != null ? budget : 0.0);
    }
}