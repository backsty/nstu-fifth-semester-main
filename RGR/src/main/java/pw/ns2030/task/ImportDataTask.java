package pw.ns2030.task;

import pw.ns2030.controller.*;
import pw.ns2030.model.*;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

public class ImportDataTask extends BackgroundTask<ImportDataTask.ImportResult> {
    private final File sourceFile;
    private final PowerSystemController powerSystem;
    private final DeviceAddCallback guiCallback;

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
        int totalLines = 0;
        
        try (BufferedReader counter = new BufferedReader(
                new InputStreamReader(new FileInputStream(sourceFile), StandardCharsets.UTF_8))) {
            while (counter.readLine() != null) totalLines++;
        }
        
        publishProgress(5, String.format("Файл: %d строк", totalLines));
        checkCancelled();

        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(new FileInputStream(sourceFile), StandardCharsets.UTF_8))) {
            
            String line;
            int lineNum = 0;
            boolean inDeviceSection = false;
            
            while ((line = reader.readLine()) != null) {
                lineNum++;
                checkCancelled();
                
                if (lineNum % 10 == 0) {
                    int percent = 5 + (int) ((lineNum / (double) totalLines) * 70);
                    publishProgress(percent, String.format("Чтение: %d/%d", lineNum, totalLines));
                    Thread.sleep(20);
                }
                
                if (line.trim().isEmpty() || line.startsWith("#")) continue;
                
                if (line.contains("=== УСТРОЙСТВА ===")) {
                    inDeviceSection = true;
                    continue;
                }
                
                if (inDeviceSection && !line.startsWith("ID;")) {
                    // Используем точку с запятой как разделитель
                    String[] parts = line.split(";", -1);
                    if (parts.length >= 6) {
                        deviceRows.add(parts);
                    }
                }
            }
        }

        publishProgress(75, "Парсинг завершен");
        Thread.sleep(300);

        List<ApplianceController> createdDevices = new ArrayList<>();
        
        if (powerSystem != null && guiCallback != null && !deviceRows.isEmpty()) {
            publishProgress(80, "Восстановление устройств...");
            
            for (int i = 0; i < deviceRows.size(); i++) {
                checkCancelled();
                
                String[] row = deviceRows.get(i);
                
                try {
                    ApplianceController controller = restoreDeviceFromRow(row);
                    if (controller != null) {
                        final ApplianceController finalController = controller;
                        javax.swing.SwingUtilities.invokeLater(() -> {
                            guiCallback.addDeviceToGUI(finalController);
                        });
                        
                        createdDevices.add(controller);
                        Thread.sleep(50);
                    }
                } catch (Exception e) {
                    System.err.println("[ImportDataTask] Ошибка восстановления строки " + (i+1) + ": " + e.getMessage());
                    e.printStackTrace();
                }
                
                int percent = 80 + (int) ((i / (double) deviceRows.size()) * 15);
                publishProgress(percent, String.format("Создано: %d/%d", i+1, deviceRows.size()));
            }
        }

        publishProgress(95, "Финализация...");
        Thread.sleep(300);

        ImportResult result = new ImportResult(deviceRows, createdDevices);
        publishProgress(100, "Импорт завершен!");
        
        return result;
    }

    private ApplianceController restoreDeviceFromRow(String[] row) {
        String id = row[0].trim();
        String type = row[1].trim();
        String name = row[2].trim();
        double power = parseDouble(row[3]);
        String stateStr = row[4].trim();
        String extraParams = row.length > 5 ? row[5].trim() : "";
        
        PowerState state;
        try {
            state = PowerState.valueOf(stateStr);
        } catch (IllegalArgumentException e) {
            System.err.println("[Import] Неизвестное состояние '" + stateStr + "', используем OFF");
            state = PowerState.OFF;
        }
        
        System.out.println(String.format("[Import] %s: %s (%.0f Вт, %s, доп: '%s')", 
            type, name, power, state, extraParams));
        
        switch (type) {
            case "Kettle":
                return restoreKettle(id, name, power, state, extraParams);
            case "Lamp":
                return restoreLamp(id, name, power, state, extraParams);
            case "Computer":
                return restoreComputer(id, name, power, state, extraParams);
            default:
                System.err.println("[Import] Неизвестный тип: " + type);
                return null;
        }
    }

    private KettleController restoreKettle(String id, String name, double power, 
                                          PowerState state, String extraParams) {
        Kettle kettle = new Kettle(id, name, power);
        
        Map<String, String> params = parseExtraParams(extraParams);
        if (params.containsKey("temp")) {
            double temp = parseDouble(params.get("temp"));
            try {
                java.lang.reflect.Field tempField = Kettle.class.getDeclaredField("temperature");
                tempField.setAccessible(true);
                tempField.set(kettle, temp);
                System.out.println("[Import] Восстановлена температура: " + temp + "°C");
            } catch (Exception e) {
                System.err.println("[Import] Не удалось восстановить температуру: " + e.getMessage());
            }
        }
        
        if (state == PowerState.HEATING || state == PowerState.COOLING) {
            kettle.turnOn();
        }
        
        return new KettleController(kettle);
    }

    private LampController restoreLamp(String id, String name, double power, 
                                       PowerState state, String extraParams) {
        Lamp lamp = new Lamp(id, name, power);
        
        if (state == PowerState.ON_GRID) {
            lamp.turnOn();
        }
        
        return new LampController(lamp);
    }

    private ComputerController restoreComputer(String id, String name, double power, 
                                              PowerState state, String extraParams) {
        Computer computer = new Computer(id, name, power);
        
        Map<String, String> params = parseExtraParams(extraParams);
        if (params.containsKey("battery")) {
            double battery = parseDouble(params.get("battery"));
            try {
                java.lang.reflect.Field batteryField = Computer.class.getDeclaredField("batteryLevel");
                batteryField.setAccessible(true);
                batteryField.set(computer, battery);
                System.out.println("[Import] Восстановлен уровень батареи: " + battery + "%");
            } catch (Exception e) {
                System.err.println("[Import] Не удалось восстановить уровень батареи: " + e.getMessage());
            }
        }
        
        if (state == PowerState.ON_GRID || state == PowerState.ON_BATTERY) {
            computer.turnOn();
        }
        
        return new ComputerController(computer);
    }

    private Map<String, String> parseExtraParams(String extraParams) {
        Map<String, String> result = new HashMap<>();
        
        if (extraParams == null || extraParams.isEmpty() || extraParams.equals("none")) {
            return result;
        }
        
        // Разделитель для дополнительных параметров - вертикальная черта
        String[] pairs = extraParams.split("\\|");
        for (String pair : pairs) {
            String[] kv = pair.split("=");
            if (kv.length == 2) {
                result.put(kv[0].trim(), kv[1].trim());
            }
        }
        
        return result;
    }

    private double parseDouble(String value) {
        try {
            return Double.parseDouble(value.trim().replace(",", "."));
        } catch (NumberFormatException e) {
            System.err.println("[Import] Не удалось распарсить число: '" + value + "'");
            return 0.0;
        }
    }

    public static class ImportResult {
        private final List<String[]> deviceRows;
        private final List<ApplianceController> createdDevices;

        public ImportResult(List<String[]> deviceRows, List<ApplianceController> createdDevices) {
            this.deviceRows = deviceRows;
            this.createdDevices = createdDevices;
        }

        public int getDeviceCount() {
            return deviceRows.size();
        }

        public int getCreatedCount() {
            return createdDevices.size();
        }

        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder();
            
            sb.append("=== РЕЗУЛЬТАТ ИМПОРТА ===\n\n");
            sb.append(String.format("📄 Прочитано из файла: %d устройств\n", getDeviceCount()));
            sb.append(String.format("✅ Восстановлено в системе: %d устройств\n\n", getCreatedCount()));
            
            if (getCreatedCount() > 0) {
                sb.append("ВОССТАНОВЛЕННЫЕ УСТРОЙСТВА:\n");
                for (ApplianceController controller : createdDevices) {
                    Appliance appliance = controller.getAppliance();
                    
                    String extra = "";
                    if (appliance instanceof Kettle) {
                        Kettle k = (Kettle) appliance;
                        extra = String.format(" | Температура: %.1f°C", k.getTemperature());
                    } else if (appliance instanceof Computer) {
                        Computer c = (Computer) appliance;
                        extra = String.format(" | Батарея: %.0f%%", c.getBatteryLevel());
                    }
                    
                    sb.append(String.format("  • %s (%s) - %.0f Вт [%s]%s\n", 
                        appliance.getName(),
                        appliance.getClass().getSimpleName(),
                        appliance.getRatedPower(),
                        appliance.getState().getDisplayName(),
                        extra));
                }
            }
            
            if (getCreatedCount() < getDeviceCount()) {
                int skipped = getDeviceCount() - getCreatedCount();
                sb.append(String.format("\n⚠️ Пропущено: %d устройств\n", skipped));
            }
            
            return sb.toString();
        }
    }
}