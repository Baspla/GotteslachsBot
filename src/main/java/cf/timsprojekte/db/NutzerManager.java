package cf.timsprojekte.db;

import cf.timsprojekte.db.Nutzer;
import org.telegram.abilitybots.api.util.AbilityUtils;
import org.telegram.telegrambots.meta.api.objects.User;

import java.util.*;
import java.util.concurrent.*;
import java.util.stream.Collectors;

public class NutzerManager {
    private final Map<Integer, Nutzer> map;
    private final Map<Integer, Nutzer> db;
    private Map<Integer, User> users;


    public NutzerManager(Map<Integer, Nutzer> db, Map<Integer, User> users) {
        this.db = db;
        this.map = new HashMap<>(db);
        this.users = users;
    }

    public void saveNutzer(){
        db.putAll(map);
    }

    public Nutzer getNutzer(Integer id) {
        Nutzer nutzer = map.get(id);
        if (nutzer == null)
            nutzer = createNutzer(id, getFullName(id));
        nutzer.setManager(this);
        return nutzer;
    }

    private String getFullName(Integer id) {
        User user = users.get(id);
        if (user == null) return "Unbekannt";
        return AbilityUtils.fullName(user);
    }

    public Nutzer createNutzer(Integer userId) {
        return createNutzer(userId, "Unbekannt");
    }

    public Nutzer createNutzer(Integer userId, String username) {
        Nutzer nutzer = new Nutzer(userId, username, 0, 0);
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
}
