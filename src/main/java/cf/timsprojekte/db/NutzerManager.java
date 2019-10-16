package cf.timsprojekte.db;

import cf.timsprojekte.Language;
import org.telegram.abilitybots.api.util.AbilityUtils;
import org.telegram.telegrambots.meta.api.objects.User;

import java.util.*;
import java.util.stream.Collectors;

public class NutzerManager {
    private final Map<Integer, Nutzer> map;
    private final Map<Integer, Nutzer> db;


    public NutzerManager(Map<Integer, Nutzer> db) {
        this.db = db;
        this.map = new HashMap<>(db);
    }

    public void saveNutzer() {
        db.putAll(map);
    }

    public Nutzer getNutzer(User user) {
        Nutzer nutzer = map.get(user.getId());
        if (nutzer == null && !user.getBot())
            nutzer = createNutzer(user.getId(), AbilityUtils.fullName(user));
        return nutzer;
    }

    private Nutzer createNutzer(Integer userId, String username) {
        HashSet<Language> langs = new HashSet<Language>();
        langs.add(Language.Deutsch);
        Nutzer nutzer = new Nutzer(userId, username, 0, 0, langs);
        map.put(userId, nutzer);
        return nutzer;
    }

    public List<Nutzer> getNutzerListeTopPoints(int i) {
        return getNutzerListe().stream().sorted(Comparator.comparingInt(Nutzer::getPoints).reversed()).limit(i).collect(Collectors.toList());
    }

    public List<Nutzer> getNutzerListeTopVotes(int i) {
        return getNutzerListe().stream().sorted(Comparator.comparingInt(Nutzer::getVotes).reversed()).limit(i).collect(Collectors.toList());
    }

    private List<Nutzer> getNutzerListe() {
        return new ArrayList<>(map.values());
    }

    public boolean hasNutzer(Integer user) {
        return map.containsKey(user);
    }
}
