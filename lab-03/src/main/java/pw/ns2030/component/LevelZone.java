package pw.ns2030.component;

import java.awt.Color;

/**
 * Enum трех зон индикатора с цветовым кодированием и уровнем опасности(зеленый/желтый/красный).
 */
public enum LevelZone {
    NORMAL("Нормальный", new Color(76, 175, 80)),
    WARNING("Предупреждение", new Color(255, 193, 7)),
    CRITICAL("Авария", new Color(244, 67, 54));

    private final String displayName;
    private final Color color;

    LevelZone(String displayName, Color color) {
        this.displayName = displayName;
        this.color = color;
    }

    public String getDisplayName() {
        return displayName;
    }

    public Color getColor() {
        return color;
    }

    // Возвращает более темный оттенок цвета зоны.
    public Color getDarkerColor() {
        return color.darker();
    }

    // Возвращает более светлый оттенок цвета зоны.
    public Color getLighterColor() {
        return color.brighter();
    }

    public boolean isCritical() {
        return this == CRITICAL;
    }

    public boolean isWarning() {
        return this == WARNING;
    }

    public boolean isNormal() {
        return this == NORMAL;
    }

    // Возвращает уровень опасности зоны.
    public int getSeverityLevel() {
        switch (this) {
            case NORMAL: return 0;
            case WARNING: return 1;
            case CRITICAL: return 2;
            default: return 0;
        }
    }

    @Override
    public String toString() {
        return displayName;
    }
}