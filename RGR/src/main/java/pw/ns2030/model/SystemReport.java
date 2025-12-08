package pw.ns2030.model;

import java.util.ArrayList;
import java.util.List;

/**
 * Модель отчета о системе энергопотребления.
 * Содержит результаты анализа и рекомендации.
 */
public class SystemReport {
    // Базовая статистика
    public int totalDevices;
    public int activeDevices;
    public double totalConsumption;
    public double maxPower;
    public boolean powerAvailable;
    
    // Статистика по типам устройств
    public int kettleCount;
    public int lampCount;
    public int computerCount;
    
    // Расчетные показатели
    public double loadPercentage;
    public double averageDevicePower;
    
    // Рекомендации
    public List<String> recommendations;
    
    // Метаданные
    public long analysisTime;  // Время анализа (мс)

    public SystemReport() {
        this.recommendations = new ArrayList<>();
    }

    /**
     * Формирование текстового представления отчета.
     */
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        
        sb.append("=== ОТЧЕТ О СИСТЕМЕ ЭНЕРГОПОТРЕБЛЕНИЯ ===\n\n");
        
        sb.append("ОБЩАЯ ИНФОРМАЦИЯ:\n");
        sb.append(String.format("  Всего устройств: %d\n", totalDevices));
        sb.append(String.format("  Активных устройств: %d\n", activeDevices));
        sb.append(String.format("  Потребление: %.0f Вт / %.0f Вт\n", 
            totalConsumption, maxPower));
        sb.append(String.format("  Загрузка системы: %.1f%%\n", loadPercentage));
        sb.append(String.format("  Состояние питания: %s\n\n", 
            powerAvailable ? "[OK] Включено" : "[X] Отключено"));
        
        sb.append("РАСПРЕДЕЛЕНИЕ ПО ТИПАМ:\n");
        sb.append(String.format("  Чайники: %d\n", kettleCount));
        sb.append(String.format("  Лампы: %d\n", lampCount));
        sb.append(String.format("  Компьютеры: %d\n\n", computerCount));
        
        sb.append("ПОКАЗАТЕЛИ ЭФФЕКТИВНОСТИ:\n");
        sb.append(String.format("  Средняя мощность устройства: %.0f Вт\n", 
            averageDevicePower));
        sb.append(String.format("  Время анализа: %d мс\n\n", analysisTime));
        
        if (!recommendations.isEmpty()) {
            sb.append("РЕКОМЕНДАЦИИ:\n");
            for (String rec : recommendations) {
                sb.append("  ").append(rec).append("\n");
            }
        }
        
        return sb.toString();
    }

    /**
     * Получение краткого описания отчета.
     */
    public String getSummary() {
        return String.format("Устройств: %d (%d активных), Потребление: %.0f/%.0f Вт (%.1f%%)",
            totalDevices, activeDevices, totalConsumption, maxPower, loadPercentage);
    }
}