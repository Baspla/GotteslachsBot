package cf.timsprojekte;

import java.util.*;

public class LevelManager {

    private static final int[] levels = {
            100, 350, 800, 1000, 1400,
            1900, 2200, 2500, 3000, 3500,
            4000, 4500, 5000, 5500, 6000,
            6500, 7000, 8000, 10000, 15000,20000};

    public static String getTitelForLevel(int level, Locale locale) {
        ResourceBundle bundle = ResourceBundle.getBundle("messages/Nachrichten", locale);
        try {
            return bundle.getString("level.name." + level);
        } catch (MissingResourceException e) {
            e.printStackTrace();
            return "Noch keine Übersetztung";
        }
    }

    public static int getLevel(int punkte) {
        for (int i = 0; i < levels.length; i++) {
            if (punkte < levels[i]) return i;
        }
        return levels.length - 1;
    }

    public static int getZiel(int level) {
        if (level < 0 || level >= levels.length) return 0;
        return levels[level];
    }

    public static String getTitelForPoints(Integer points, Locale locale) {
        return getTitelForLevel(getLevel(points), locale);
    }
}
