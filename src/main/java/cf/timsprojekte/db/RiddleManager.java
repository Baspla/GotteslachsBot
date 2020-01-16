package cf.timsprojekte.db;

import java.util.*;
import java.util.stream.Collectors;

public class RiddleManager {
    private final Map<String, Riddle> map;
    private final Map<String, Riddle> db;


    public RiddleManager(Map<String, Riddle> db) {
        this.db = db;
        this.map = new HashMap<>(db);
    }

    public void saveRiddles() {
        db.putAll(map);
    }

    public void loadRiddles() {
        map.putAll(db);
    }

    public Optional<Riddle> getRiddle(String riddleId) {
        return Optional.ofNullable(map.get(riddleId));
    }

    public Riddle createRiddle(String titel, String text, String antwort, String belohnung, boolean abgeschlossen, boolean einmalig, boolean benachrichtigen, int creator) {
        Random r = new Random();
        String rnd;
        do {
            rnd = toCode(r.nextInt());
        } while (map.keySet().contains(rnd));
        Riddle riddle = new Riddle(rnd,titel, text, antwort, belohnung, abgeschlossen, einmalig, benachrichtigen, creator);
        map.put(rnd, riddle);
        return riddle;
    }

    public static void main(String[] args) {
        for (int i = 1; i < 4000000; i = i + 4000) {
            System.out.println(i + " - " + toCode(i));
        }
    }


    public List<Riddle> getRiddleListe() {
        return new ArrayList<>(map.values());
    }

    public List<Riddle> getAbgeschlosseneRiddleListe() {
        return map.values().stream().filter(riddle -> !riddle.isAbgeschlossen()).collect(Collectors.toList());
    }

    public boolean hasRiddle(String riddleId) {
        return map.containsKey(riddleId);
    }

    public void removeRiddle(String riddleId) {
        map.remove(riddleId);
    }

    public List<Riddle> getRiddleListe(Integer userId) {
        return map.values().stream().filter(riddle -> riddle.getCreator() == userId).collect(Collectors.toList());
    }

    private static String toCode(int n) {
        int b = 36;
        int k = 0;
        int x = n / b;
        int a = n % b;
        StringBuilder code = new StringBuilder();
        codeInsert(code, a);
        while (x != 0) {
            k = k + 1;          //Schleifendurchlauf
            int y = x / b;      //y ist Hhilfsvariable für x
            a = x % b;
            x = y;
            codeInsert(code, a);
        }
        return code.toString().replaceAll("O", "0").replaceAll("Q", "0").replaceAll("I", "1");
    }

    private static void codeInsert(StringBuilder code, int a) {
        if (a < 10) {
            code.insert(0, a);
        } else {
            code.insert(0, ((char) (a + 55)));
        }
    }
}
