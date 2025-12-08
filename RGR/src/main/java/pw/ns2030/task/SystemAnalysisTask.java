package pw.ns2030.task;

import pw.ns2030.controller.PowerSystemController;
import pw.ns2030.model.*;
import pw.ns2030.task.TaskHelpers.*;

import java.util.ArrayList;
import java.util.List;

/**
 * Фоновая задача анализа системы энергопотребления.
 * Генерирует подробный отчет о состоянии системы.
 */
public class SystemAnalysisTask extends BackgroundTask<SystemReport> {
    private final PowerSystemController powerSystem;

    public SystemAnalysisTask(PowerSystemController powerSystem) {
        this.powerSystem = powerSystem;
    }

    @Override
    protected SystemReport performTask() throws Exception {
        publishProgress(0, "Начало анализа системы...");
        Thread.sleep(300);
        
        checkCancelled();

        SystemReport report = new SystemReport();
        
        // Этап 1: Базовая статистика
        publishProgress(10, "Сбор базовой статистики...");
        report.totalDevices = powerSystem.getDeviceCount();
        report.totalConsumption = powerSystem.getTotalConsumption();
        report.maxPower = powerSystem.getMaxPower();
        report.powerAvailable = powerSystem.isPowerAvailable();
        Thread.sleep(500);
        
        checkCancelled();

        // Этап 2: Анализ устройств
        publishProgress(30, "Анализ устройств...");
        List<Appliance> devices = powerSystem.getDevices();
        
        for (int i = 0; i < devices.size(); i++) {
            checkCancelled();
            
            Appliance device = devices.get(i);
            
            if (device.isOn()) {
                report.activeDevices++;
            }
            
            if (device instanceof Kettle) {
                report.kettleCount++;
            } else if (device instanceof Lamp) {
                report.lampCount++;
            } else if (device instanceof Computer) {
                report.computerCount++;
            }
            
            int percent = 30 + (int) ((i / (double) devices.size()) * 30);
            publishProgress(percent, 
                String.format("Анализ устройства %d / %d", i + 1, devices.size()));
            
            Thread.sleep(100);
        }
        
        // Этап 3: Расчет эффективности
        publishProgress(60, "Расчет эффективности...");
        report.loadPercentage = (report.totalConsumption / report.maxPower) * 100.0;
        report.averageDevicePower = report.activeDevices > 0 ? 
            report.totalConsumption / report.activeDevices : 0;
        Thread.sleep(700);
        
        checkCancelled();

        // Этап 4: Рекомендации
        publishProgress(80, "Генерация рекомендаций...");
        generateRecommendations(report);
        Thread.sleep(500);
        
        // Этап 5: Финализация
        publishProgress(95, "Финализация отчета...");
        report.analysisTime = getElapsedTime();
        Thread.sleep(300);
        
        publishProgress(100, "Анализ завершен!");
        
        return report;
    }

    /**
     * Генерация рекомендаций на основе анализа.
     */
    private void generateRecommendations(SystemReport report) {
        report.recommendations = new ArrayList<>();
        
        if (report.loadPercentage > 90) {
            report.recommendations.add("⚠️ КРИТИЧЕСКАЯ НАГРУЗКА! Рекомендуется отключить неиспользуемые устройства.");
        } else if (report.loadPercentage > 75) {
            report.recommendations.add("⚠️ Высокая нагрузка на систему. Контролируйте потребление.");
        } else if (report.loadPercentage < 20) {
            report.recommendations.add("✓ Низкая нагрузка. Система работает эффективно.");
        }
        
        if (report.activeDevices == 0) {
            report.recommendations.add("ℹ️ Все устройства выключены.");
        }
        
        if (report.computerCount > 0) {
            report.recommendations.add("ℹ️ Компьютеры с ИБП обеспечивают защиту от отключения питания.");
        }
        
        if (!report.powerAvailable) {
            report.recommendations.add("❌ ПИТАНИЕ ОТКЛЮЧЕНО! Восстановите электропитание.");
        }
    }
}