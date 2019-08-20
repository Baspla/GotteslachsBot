package cf.timsprojekte.verwaltung;

import cf.timsprojekte.verwaltung.immutable.Benutzer;
import cf.timsprojekte.UniqueBot;
import cf.timsprojekte.verwaltung.immutable.Abzeichen;
import org.telegram.abilitybots.api.db.DBContext;

import javax.validation.constraints.NotNull;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.Random;
import java.util.Set;
import java.util.stream.Collectors;

public class Abzeichenverwaltung {
    private final Set<Abzeichen> abzeichenSet;

    public Abzeichenverwaltung(DBContext db) {
        abzeichenSet = db.getSet("Abzeichen");
    }


    public Abzeichen getAbzeichen(long abzeichenId) {
        Optional<Abzeichen> abzeichen = abzeichenSet.stream().filter(b -> b.getAbzeichenId() == abzeichenId).findAny();
        return abzeichen.orElse(null);
    }

    public Abzeichen createAbzeichen(int belohnung, String name, String beschreibung) {
        Random rnd = new Random(System.currentTimeMillis());
        int rndInt;
        do {
            rndInt = Math.abs(rnd.nextInt());
        } while (isAbzeichenIdUsed(rndInt));
        Abzeichen abzeichen = new Abzeichen(rndInt, belohnung, name, beschreibung);
        abzeichenSet.add(abzeichen);
        return abzeichen;
    }

    public void removeAbzeichen(Abzeichen abzeichen) {
        abzeichenSet.remove(abzeichen);
        Nutzerverwaltung n = UniqueBot.unique().nutzerverwaltung;
        List<Benutzer> user = n.getBenuzerList().stream().filter(benutzer -> benutzer.hasAbzeichen(abzeichen.getAbzeichenId())).collect(Collectors.toList());
        user.forEach(benutzer -> n.removeBenutzerAbzeichen(benutzer, abzeichen));
    }

    private boolean isAbzeichenIdUsed(int rndInt) {
        return abzeichenSet.stream().anyMatch(abzeichen -> abzeichen.getAbzeichenId() == rndInt);
    }

    public Abzeichen setAbzeichenBelohnung(@NotNull Abzeichen abzeichen, int belohung) {
        return replace(abzeichen, new Abzeichen(abzeichen.getAbzeichenId(), belohung, abzeichen.getName(), abzeichen.getBeschreibung()));
    }

    public Abzeichen setAbzeichenName(@NotNull Abzeichen abzeichen, @NotNull String name) {
        return replace(abzeichen, new Abzeichen(abzeichen.getAbzeichenId(), abzeichen.getBelohnung(), name, abzeichen.getBeschreibung()));
    }

    public Abzeichen setAbzeichenBeschreibung(@NotNull Abzeichen abzeichen, @NotNull String beschreibung) {
        return replace(abzeichen, new Abzeichen(abzeichen.getAbzeichenId(), abzeichen.getBelohnung(), abzeichen.getName(), beschreibung));
    }

    public Abzeichen replace(@NotNull Abzeichen vorher, @NotNull Abzeichen nachher) {
        if (abzeichenSet.remove(vorher)) {
            if (abzeichenSet.add(nachher)) {
                return nachher;
            }
            abzeichenSet.add(vorher);
        }
        return vorher;
    }

    public String convertToString(int[] convert) {
        String str = "";
        for (int i = 0; i < convert.length; i++) {
            Abzeichen abzeichen = getAbzeichen(convert[i]);
            if (abzeichen != null)
                str = str + abzeichen.getName() + "\n";
        }
        if (convert.length == 0) str = "Du hast keine Abzeichen.";
        return str;
    }

    public List<Abzeichen> getAbzeichenList() {
        ArrayList<Abzeichen> list = new ArrayList<>(abzeichenSet);
        list.sort(Comparator.comparingInt(Abzeichen::getAbzeichenId));
        return list;
    }
}
