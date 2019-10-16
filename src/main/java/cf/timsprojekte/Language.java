package cf.timsprojekte;


import kotlin.Pair;

import java.util.HashMap;
import java.util.Locale;

public enum Language {
    Deutsch("Deutsch",new Locale("de","DE"),13),
    Englisch("Englisch",new Locale("en","US"),13),
    BrudiDeutsch("Brudi Deutsch",new Locale("de","DE","brudi"),9),
    PiratDeutsch("Piraten Deutsch",new Locale("de","DE","pirat"),7),
    UwuDeutsch("uwu Deutsch",new Locale("de","DE","uwu"),5),
    Enchant("MC Enchanting",new Locale("mc","enchant"),1);

    private final String title;
    private final Locale locale;
    private final int rarity;

    Language(String title, Locale locale,int rarity) {
        this.title=title;
        this.locale=locale;
        this.rarity=rarity;
    }

    public static Pair<Integer, HashMap<Integer, Language>> getLootMap() {
        int total = 0;
        HashMap<Integer, Language> lootMap = new HashMap<>();
        for (Language value : Language.values()) {
            lootMap.put(total, value);
            total += value.rarity();
        }
        return new Pair<>(total, lootMap);
    }

    public String title() {
        return title;
    }

    public String data() {
        return locale.toString();
    }

    public int rarity() {
        return rarity;
    }
}
