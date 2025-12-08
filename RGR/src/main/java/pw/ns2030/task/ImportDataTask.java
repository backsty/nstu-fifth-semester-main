package pw.ns2030.task;

import pw.ns2030.controller.*;
import pw.ns2030.model.*;
import pw.ns2030.task.TaskHelpers.*;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

/**
 * Задача импорта данных из CSV файла.
 * Поддерживает два режима: просмотр данных и полное восстановление устройств.
 */
public class ImportDataTask extends BackgroundTask<ImportDataTask.ImportResult> {
    private final File sourceFile;
    private final PowerSystemController powerSystem;
    private final DeviceAddCallback guiCallback;

    /**
     * Callback для добавления устройства в GUI через EDT.
     */
    @FunctionalInterface
    public interface DeviceAddCallback {
        void addDeviceToGUI(ApplianceController controller);
    }

    public ImportDataTask(File sourceFile, PowerSystemController powerSystem, DeviceAddCallback guiCallback) {
        this.sourceFile = sourceFile;
        this.powerSystem = powerSystem;
        this.guiCallback = guiCallback;
    }

    public ImportDataTask(File sourceFile) {
        this(sourceFile, null, null);
    }

    @Override
    protected ImportResult performTask() throws Exception {
        publishProgress(0, "Открытие файла...");
        Thread.sleep(200);
        
        checkCancelled();

        List<String[]> deviceRows = new ArrayList<>();
        List<String[]> historyRows = new ArrayList<>();
        int totalLines = 0;
        
        // Подсчет строк для прогресса
        try (BufferedReader counter = new BufferedReader(
                new InputStreamReader(
                    new FileInputStream(sourceFile), 
                    StandardCharsets.UTF_8))) {
            while (counter.readLine() != null) {
                totalLines++;
            }
        }
        
        publishProgress(5, String.format("Файл содержит %d строк", totalLines));
        
        checkCancelled();

        // Чтение и парсинг CSV
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(
                    new FileInputStream(sourceFile), 
                    StandardCharsets.UTF_8))) {
            
            String line;
            int lineNum = 0;
            boolean inDeviceSection = false;
            boolean inHistorySection = false;
            
            while ((line = reader.readLine()) != null) {
                lineNum++;
                checkCancelled();
                
                if (lineNum % 10 == 0) {
                    int percent = 5 + (int) ((lineNum / (double) totalLines) * 70);
                    publishProgress(percent, 
                        String.format("Обработано строк: %d / %d", lineNum, totalLines));
                    
                    Thread.sleep(20);
                }
                
                if (line.trim().isEmpty() || line.startsWith("#")) {
                    continue;
                }
                
                if (line.contains("=== ТЕКУЩЕЕ СОСТОЯНИЕ УСТРОЙСТВ ===")) {
                    inDeviceSection = true;
                    inHistorySection = false;
                    continue;
                } else if (line.contains("=== ИСТОРИЯ ПОТРЕБЛЕНИЯ ===")) {
                    inDeviceSection = false;
                    inHistorySection = true;
                    continue;
                }
                
                if (inDeviceSection && !line.startsWith("ID,")) {
                    String[] parts = parseCsvLine(line);
                    if (parts.length >= 5) {
                        deviceRows.add(parts);
                    }
                } else if (inHistorySection && !line.startsWith("Устройство,")) {
                    String[] parts = parseCsvLine(line);
                    if (parts.length >= 4) {
                        historyRows.add(parts);
                    }
                }
            }
        }

        publishProgress(75, "Парсинг завершен. Обработка данных...");
        Thread.sleep(300);

        // Создание устройств через callback в EDT
        List<ApplianceController> createdDevices = new ArrayList<>();
        
        if (powerSystem != null && guiCallback != null && !deviceRows.isEmpty()) {
            publishProgress(80, "Создание устройств в системе...");
            
            for (int i = 0; i < deviceRows.size(); i++) {
                checkCancelled();
                
                String[] row = deviceRows.get(i);
                
                try {
                    ApplianceController controller = createDeviceFromRow(row);
                    if (controller != null) {
                        final ApplianceController finalController = controller;
                        javax.swing.SwingUtilities.invokeLater(() -> {
                            guiCallback.addDeviceToGUI(finalController);
                        });
                        
                        createdDevices.add(controller);
                        Thread.sleep(50);
                    }
                } catch (Exception e) {
                    System.err.println("[ImportDataTask] Ошибка создания устройства: " + e.getMessage());
                    e.printStackTrace();
                }
                
                int percent = 80 + (int) ((i / (double) deviceRows.size()) * 15);
                publishProgress(percent, 
                    String.format("Создано устройств: %d / %d", i + 1, deviceRows.size()));
            }
        }

        publishProgress(95, "Финализация импорта...");
        Thread.sleep(300);

        ImportResult result = new ImportResult(
            deviceRows,
            historyRows,
            createdDevices
        );
        
        publishProgress(100, "Импорт завершен!");
        
        return result;
    }

    /**
     * Создание контроллера устройства из CSV строки.
     */
    private ApplianceController createDeviceFromRow(String[] row) {
        if (row.length < 5) {
            System.err.println("[ImportDataTask] Недостаточно колонок: " + row.length);
            return null;
        }
        
        String id = row[0].trim();
        String name = row[1].trim();
        String type = row[2].trim();
        double power = parsePower(row[4]);
        
        System.out.println("[ImportDataTask] Создание: " + name + " (" + type + ") - " + power + " Вт");
        
        switch (type) {
            case "Kettle":
                Kettle kettle = new Kettle(id, name, power > 0 ? power : 2000.0);
                return new KettleController(kettle);
                
            case "Lamp":
                Lamp lamp = new Lamp(id, name, power > 0 ? power : 60.0);
                return new LampController(lamp);
                
            case "Computer":
                Computer computer = new Computer(id, name, power > 0 ? power : 300.0);
                return new ComputerController(computer);
                
            default:
                System.err.println("[ImportDataTask] Неизвестный тип устройства: " + type);
                return null;
        }
    }

    private double parsePower(String powerStr) {
        try {
            String normalized = powerStr.trim().replace(",", ".");
            double value = Double.parseDouble(normalized);
            return value;
        } catch (NumberFormatException e) {
            System.err.println("[ImportDataTask] Ошибка парсинга мощности: " + powerStr);
            return 0.0;
        }
    }

    /**
     * Парсинг CSV строки с учетом кавычек и запятых.
     */
    private String[] parseCsvLine(String line) {
        List<String> result = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        boolean inQuotes = false;
        
        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);
            
            if (c == '"') {
                inQuotes = !inQuotes;
            } else if (c == ',' && !inQuotes) {
                result.add(current.toString());
                current = new StringBuilder();
            } else {
                current.append(c);
            }
        }
        
        result.add(current.toString());
        
        return result.toArray(new String[0]);
    }

    /**
     * Результат импорта с информацией о прочитанных и созданных устройствах.
     */
    public static class ImportResult {
        private final List<String[]> deviceRows;
        private final List<String[]> historyRows;
        private final List<ApplianceController> createdDevices;

        public ImportResult(List<String[]> deviceRows, 
                           List<String[]> historyRows,
                           List<ApplianceController> createdDevices) {
            this.deviceRows = deviceRows;
            this.historyRows = historyRows;
            this.createdDevices = createdDevices;
        }

        public int getDeviceCount() {
            return deviceRows.size();
        }

        public int getHistoryCount() {
            return historyRows.size();
        }

        public int getCreatedCount() {
            return createdDevices.size();
        }

        public List<String[]> getDeviceRows() {
            return deviceRows;
        }

        public List<String[]> getHistoryRows() {
            return historyRows;
        }

        public List<ApplianceController> getCreatedDevices() {
            return createdDevices;
        }

        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder();
            
            sb.append("=== РЕЗУЛЬТАТ ИМПОРТА ===\n\n");
            sb.append(String.format("📄 Прочитано устройств из файла: %d\n", getDeviceCount()));
            sb.append(String.format("📄 Прочитано записей истории: %d\n", getHistoryCount()));
            sb.append(String.format("✅ Создано устройств в системе: %d\n\n", getCreatedCount()));
            
            if (getCreatedCount() > 0) {
                sb.append("ДОБАВЛЕННЫЕ УСТРОЙСТВА:\n");
                for (ApplianceController controller : createdDevices) {
                    Appliance appliance = controller.getAppliance();
                    sb.append(String.format("  • %s (%s) - %.0f Вт\n", 
                        appliance.getName(),
                        appliance.getClass().getSimpleName(),
                        appliance.getRatedPower()));
                }
            }
            
            if (getCreatedCount() < getDeviceCount()) {
                int skipped = getDeviceCount() - getCreatedCount();
                sb.append(String.format("\n⚠️ Пропущено устройств: %d\n", skipped));
            }
            
            return sb.toString();
        }
    }
}