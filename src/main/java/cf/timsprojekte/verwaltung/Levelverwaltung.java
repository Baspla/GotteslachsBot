package cf.timsprojekte.verwaltung;

public class Levelverwaltung {
    private static final int[] levels = {
            100, 350, 800, 813, 1400,
            1900, 2200, 2500, 3000, 3500,
            4000, 4500, 5000, 5500, 6000,
            6500, 7000, 8000};
    private static final String[] titles = {
            "Rentner", "Rekrut", "Frischling", "Gottkönig", "Freischwimmer",
            "Woke Chatter", "Plappermaul", "Informant", "Kurznachrichten Goethe", "Aufmerksamkeitsgeil",
            "Message Meister", "Chat-Süchtiger", "Yeet-aholic", "Legende", "Meme-Lord",
            "Cleverbot", "Captain", "Chat-Gott"};

    public String getTitleForLevel(int level) {
        if (level >= 0 && level < titles.length) return titles[level];
        else return "Unbekanntes Level (" + level + ")";
    }

    public int getLevel(int punkte) {
        for (int i = 0; i < levels.length; i++) {
            if (punkte < levels[i]) return i;
        }
        return levels.length - 1;
    }

    public int getGoal(int level) {
        if (level < 0 || level >= levels.length) return 0;
        return levels[level];
    }
}
