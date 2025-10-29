package pw.ns2030.models;

import pw.ns2030.annotations.JsonField;
import pw.ns2030.annotations.JsonIgnore;
import pw.ns2030.annotations.JsonSerializable;

import java.util.List;
import java.util.ArrayList;
import java.util.Objects;

/**
 * Модель Company для демонстрации сериализации ссылок и массивов.
 */
@JsonSerializable(includeNulls = true)
public class Company {
    
    @JsonField(value = "company_name", order = 1)
    private String name;
    
    @JsonField(value = "address", order = 2)
    private String address;
    
    @JsonField(value = "employees", order = 3)
    private List<Person> employees;
    
    @JsonField(value = "departments", order = 4)
    private List<Department> departments;
    
    @JsonField(value = "founded_year", order = 5)
    private Integer foundedYear;
    
    @JsonIgnore(reason = "Внутренняя информация")
    private double revenue;
    
    // Конструктор по умолчанию
    public Company() {
        this.employees = new ArrayList<>();
        this.departments = new ArrayList<>();
    }
    
    public Company(String name) {
        this();
        this.name = name;
    }
    
    public Company(String name, String address) {
        this(name);
        this.address = address;
    }
    
    public Company(String name, String address, Integer foundedYear) {
        this(name, address);
        this.foundedYear = foundedYear;
    }
    
    // Геттеры и сеттеры
    public String getName() {
        return name;
    }
    
    public void setName(String name) {
        this.name = name;
    }
    
    public String getAddress() {
        return address;
    }
    
    public void setAddress(String address) {
        this.address = address;
    }
    
    public List<Person> getEmployees() {
        return employees;
    }
    
    public void setEmployees(List<Person> employees) {
        this.employees = employees != null ? employees : new ArrayList<>();
    }
    
    public List<Department> getDepartments() {
        return departments;
    }
    
    public void setDepartments(List<Department> departments) {
        this.departments = departments != null ? departments : new ArrayList<>();
    }
    
    public Integer getFoundedYear() {
        return foundedYear;
    }
    
    public void setFoundedYear(Integer foundedYear) {
        this.foundedYear = foundedYear;
    }
    
    public double getRevenue() {
        return revenue;
    }
    
    public void setRevenue(double revenue) {
        this.revenue = revenue;
    }
    
    // Удобные методы для работы с сотрудниками
    public void addEmployee(Person employee) {
        if (employee != null && !this.employees.contains(employee)) {
            this.employees.add(employee);
            // Устанавливаем связь только если она еще не установлена
            if (employee.getCompany() != this) {
                employee.setCompany(this);
            }
        }
    }
    
    public void removeEmployee(Person employee) {
        if (employee != null && this.employees.remove(employee)) {
            // Убираем связь только если она указывает на нас
            if (employee.getCompany() == this) {
                employee.setCompany(null);
            }
        }
    }

    // Методы для работы с отделами
    public void addDepartment(Department department) {
        if (department != null && !this.departments.contains(department)) {
            this.departments.add(department);
            // Устанавливаем связь только если она еще не установлена
            if (department.getCompany() != this) {
                department.setCompany(this);
            }
        }
    }
    
    public void removeDepartment(Department department) {
        if (department != null && this.departments.remove(department)) {
            // Убираем связь только если она указывает на нас
            if (department.getCompany() == this) {
                department.setCompany(null);
            }
        }
    }
    
    /**
     * Получает общее количество сотрудников во всех отделах.
     */
    public int getTotalEmployeeCount() {
        int total = employees.size();
        for (Department dept : departments) {
            total += dept.getEmployees().size();
        }
        return total;
    }
    
    /**
     * Находит отдел по имени.
     */
    public Department findDepartmentByName(String name) {
        return departments.stream()
                .filter(dept -> Objects.equals(dept.getName(), name))
                .findFirst()
                .orElse(null);
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Company company = (Company) o;
        return Objects.equals(name, company.name) && 
               Objects.equals(address, company.address);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(name, address);
    }
    
    @Override
    public String toString() {
        return String.format("Company{name='%s', address='%s', employees=%d, departments=%d, founded=%d}", 
                name, address, 
                employees != null ? employees.size() : 0,
                departments != null ? departments.size() : 0,
                foundedYear);
    }
}