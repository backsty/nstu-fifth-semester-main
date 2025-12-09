package pw.ns2030.task;

import pw.ns2030.controller.ApplianceController;
import pw.ns2030.model.*;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

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
            
            writer.write("# Экспорт системы потребителей энергии\n");
            writer.write("# Дата: " + LocalDateTime.now().format(TIMESTAMP_FORMAT) + "\n");
            writer.write("# Устройств: " + controllers.size() + "\n");
            writer.write("\n");
            
            publishProgress(5, "Заголовок записан");
            checkCancelled();
            
            writer.write("=== УСТРОЙСТВА ===\n");
            writer.write("ID;Тип;Название;Мощность;Состояние;ДопПараметры\n");
            
            int total = controllers.size();
            for (int i = 0; i < total; i++) {
                checkCancelled();
                
                ApplianceController controller = controllers.get(i);
                Appliance appliance = controller.getAppliance();
                
                String row = buildDeviceRow(appliance);
                writer.write(row + "\n");
                
                int percent = 5 + (int) ((i / (double) total) * 90);
                publishProgress(percent, 
                    String.format("Экспорт: %d/%d - %s", i+1, total, appliance.getName()));
                
                if (i % 3 == 0) Thread.sleep(100);
            }
            
            writer.write("\n");
            publishProgress(95, "Завершение записи...");
            writer.flush();
        }

        publishProgress(100, "Экспорт завершен!");
        Thread.sleep(200);
        return targetFile;
    }

    private String buildDeviceRow(Appliance appliance) {
        StringBuilder sb = new StringBuilder();
        
        // Используем точку с запятой как разделитель
        sb.append(appliance.getId()).append(";");
        sb.append(appliance.getClass().getSimpleName()).append(";");
        sb.append(appliance.getName()).append(";");
        sb.append(String.format("%.2f", appliance.getRatedPower())).append(";");
        sb.append(appliance.getState().name()).append(";");
        
        String extraParams = extractExtraParameters(appliance);
        sb.append(extraParams);
        
        return sb.toString();
    }

    private String extractExtraParameters(Appliance appliance) {
        if (appliance instanceof Kettle) {
            Kettle kettle = (Kettle) appliance;
            return String.format("temp=%.2f", kettle.getTemperature());
            
        } else if (appliance instanceof Computer) {
            Computer computer = (Computer) appliance;
            return String.format("battery=%.2f|charging=%b|onBattery=%b", 
                computer.getBatteryLevel(),
                computer.isCharging(),
                computer.isOnBattery());
                
        } else if (appliance instanceof Lamp) {
            return "none";
        }
        
        return "";
    }
}