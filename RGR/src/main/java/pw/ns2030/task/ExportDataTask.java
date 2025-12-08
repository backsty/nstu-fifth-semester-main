package pw.ns2030.task;

import pw.ns2030.controller.ApplianceController;
import pw.ns2030.model.*;
import pw.ns2030.task.TaskHelpers.*;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Задача экспорта данных системы в CSV файл.
 * Экспортирует текущее состояние устройств и историю потребления.
 */
public class ExportDataTask extends BackgroundTask<File> {
    private static final DateTimeFormatter TIMESTAMP_FORMAT = 
        DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    
    private final List<ApplianceController> controllers;
    private final File targetFile;
    private final boolean includeHistory;

    public ExportDataTask(List<ApplianceController> controllers, 
                         File targetFile, 
                         boolean includeHistory) {
        this.controllers = controllers;
        this.targetFile = targetFile;
        this.includeHistory = includeHistory;
    }

    @Override
    protected File performTask() throws Exception {
        publishProgress(0, "Подготовка к экспорту...");
        Thread.sleep(300);
        
        checkCancelled();

        try (BufferedWriter writer = new BufferedWriter(
                new OutputStreamWriter(
                    new FileOutputStream(targetFile), 
                    StandardCharsets.UTF_8))) {
            
            // Заголовок файла
            writer.write("# Экспорт данных системы потребителей энергии\n");
            writer.write("# Дата: " + LocalDateTime.now().format(TIMESTAMP_FORMAT) + "\n");
            writer.write("# Устройств: " + controllers.size() + "\n");
            writer.write("\n");
            
            publishProgress(5, "Заголовок записан");
            checkCancelled();
            
            // Экспорт текущего состояния устройств
            writer.write("=== ТЕКУЩЕЕ СОСТОЯНИЕ УСТРОЙСТВ ===\n");
            writer.write("ID,Название,Тип,Состояние,Мощность (Вт),Доп. информация\n");
            
            int total = controllers.size();
            for (int i = 0; i < total; i++) {
                checkCancelled();
                
                ApplianceController controller = controllers.get(i);
                Appliance appliance = controller.getAppliance();
                
                String extraInfo = getExtraInfo(appliance);
                
                writer.write(String.format("%s,%s,%s,%s,%.2f,%s\n",
                    appliance.getId(),
                    escapeCsv(appliance.getName()),
                    appliance.getClass().getSimpleName(),
                    appliance.getState().getDisplayName(),
                    appliance.getCurrentPower(),
                    extraInfo));
                
                int percent = 5 + (int) ((i / (double) total) * 40);
                publishProgress(percent, 
                    String.format("Экспортировано устройств: %d / %d", i + 1, total));
                
                if (i % 3 == 0) {
                    Thread.sleep(100);
                }
            }
            
            writer.write("\n");
            publishProgress(45, "Состояние устройств экспортировано");
            
            // Экспорт истории потребления
            if (includeHistory) {
                checkCancelled();
                
                writer.write("=== ИСТОРИЯ ПОТРЕБЛЕНИЯ ===\n");
                writer.write("Устройство,Время,Состояние,Мощность (Вт)\n");
                
                int recordsWritten = 0;
                for (int i = 0; i < total; i++) {
                    checkCancelled();
                    
                    ApplianceController controller = controllers.get(i);
                    Appliance appliance = controller.getAppliance();
                    
                    // Генерируем пример истории
                    for (int j = 0; j < 10; j++) {
                        writer.write(String.format("%s,%s,%s,%.2f\n",
                            escapeCsv(appliance.getName()),
                            LocalDateTime.now().minusMinutes(10 - j).format(TIMESTAMP_FORMAT),
                            appliance.getState().getDisplayName(),
                            appliance.getCurrentPower()));
                        
                        recordsWritten++;
                    }
                    
                    int percent = 45 + (int) ((i / (double) total) * 50);
                    publishProgress(percent, 
                        String.format("Экспортировано записей: %d", recordsWritten));
                    
                    Thread.sleep(50);
                }
                
                writer.write("\n");
            }
            
            publishProgress(95, "Завершение записи...");
            writer.flush();
        }

        publishProgress(100, "Экспорт завершен!");
        Thread.sleep(200);
        
        return targetFile;
    }

    /**
     * Получение дополнительной информации о устройстве.
     */
    private String getExtraInfo(Appliance appliance) {
        if (appliance instanceof Kettle) {
            Kettle kettle = (Kettle) appliance;
            return String.format("Температура: %.1f°C", kettle.getTemperature());
        } else if (appliance instanceof Computer) {
            Computer computer = (Computer) appliance;
            return String.format("Батарея: %.0f%%", computer.getBatteryLevel());
        } else if (appliance instanceof Lamp) {
            return "N/A";
        }
        return "";
    }

    /**
     * Экранирование строк для CSV формата.
     */
    private String escapeCsv(String value) {
        if (value == null) {
            return "";
        }
        
        if (value.contains(",") || value.contains("\"") || value.contains("\n")) {
            return "\"" + value.replace("\"", "\"\"") + "\"";
        }
        
        return value;
    }
}