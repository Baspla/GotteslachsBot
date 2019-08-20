package cf.timsprojekte.verwaltung;

import cf.timsprojekte.verwaltung.immutable.Benutzer;
import cf.timsprojekte.Bot;
import cf.timsprojekte.exceptions.ElementAlreadyExistsException;
import cf.timsprojekte.exceptions.ElementDoesntExistException;
import cf.timsprojekte.verwaltung.immutable.Abzeichen;
import org.telegram.abilitybots.api.db.DBContext;

import javax.validation.constraints.NotNull;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

public class Nutzerverwaltung {

    private final Set<Benutzer> benutzerSet;
    private Bot bot;

    public Nutzerverwaltung(DBContext db, Bot bot) {
        benutzerSet = db.getSet("Benutzer");
        this.bot = bot;
    }

    public Benutzer getBenutzer(long userId) {
        Optional<Benutzer> benutzer = benutzerSet.stream().filter(b -> b.getUserId() == userId).findAny();
        return benutzer.orElse(null);
    }

    public void createBenutzer(@NotNull Benutzer benutzer) {
        if (benutzerSet.contains(benutzer)) return;
        benutzerSet.add(benutzer);
    }

    public Benutzer setBenutzerPunkte(@NotNull Benutzer benutzer, int punkte) {
        int alt = benutzer.getPunkte();
        int neu = punkte;
        Benutzer out = replace(benutzer, new Benutzer(benutzer.getUserId(), punkte, benutzer.getAbzeichen(), benutzer.getNextBelohnung(), benutzer.getNextLike(), benutzer.getNextDaily(), benutzer.getNutzername()));
        bot.checkLevelUp(benutzer, alt, neu);
        return out;
    }

    public Benutzer addBenutzerPunkte(@NotNull Benutzer benutzer, int punkte) {
        int alt = benutzer.getPunkte();
        int neu = alt + punkte;
        Benutzer out = replace(benutzer, new Benutzer(benutzer.getUserId(), neu, benutzer.getAbzeichen(), benutzer.getNextBelohnung(), benutzer.getNextLike(), benutzer.getNextDaily(), benutzer.getNutzername()));
        bot.checkLevelUp(benutzer, alt, neu);
        return out;
    }
    public Benutzer removeBenutzerPunkte(@NotNull Benutzer benutzer, int punkte) {
        Benutzer out = replace(benutzer, new Benutzer(benutzer.getUserId(),benutzer.getPunkte()-punkte, benutzer.getAbzeichen(), benutzer.getNextBelohnung(), benutzer.getNextLike(), benutzer.getNextDaily(), benutzer.getNutzername()));
        return out;
    }

    public Benutzer addBenutzerAbzeichen(@NotNull Benutzer benutzer, @NotNull Abzeichen abzeichen) throws ElementAlreadyExistsException {
        int[] altAbzeichen = benutzer.getAbzeichen();
        int[] newAbzeichen = new int[altAbzeichen.length + 1];
        for (int i = 0; i < altAbzeichen.length; i++) {
            newAbzeichen[i] = altAbzeichen[i];
            if (altAbzeichen[i] == abzeichen.getAbzeichenId()) throw new ElementAlreadyExistsException();
        }
        newAbzeichen[altAbzeichen.length] = abzeichen.getAbzeichenId();
        int alt = benutzer.getPunkte();
        Benutzer neu = replace(benutzer, new Benutzer(benutzer.getUserId(), benutzer.getPunkte() + abzeichen.getBelohnung(), newAbzeichen, benutzer.getNextBelohnung(), benutzer.getNextLike(), benutzer.getNextDaily(), benutzer.getNutzername()));
        bot.checkLevelUp(benutzer, alt, neu.getPunkte());
        return neu;
    }

    public Benutzer removeBenutzerAbzeichen(@NotNull Benutzer benutzer, @NotNull Abzeichen abzeichen) throws ElementDoesntExistException {
        int[] altAbzeichen = benutzer.getAbzeichen();
        if (altAbzeichen.length <= 0) throw new ElementDoesntExistException();
        int exists = -1;
        for (int i = 0; i < altAbzeichen.length; i++) {
            if (altAbzeichen[i] == abzeichen.getAbzeichenId()) exists = i;
        }
        if (exists == -1) throw new ElementDoesntExistException();
        int[] newAbzeichen = new int[altAbzeichen.length - 1];
        for (int i = 0; i < newAbzeichen.length; i++) {
            newAbzeichen[i] = altAbzeichen[(i < exists) ? i : i - 1];
        }
        return replace(benutzer, new Benutzer(benutzer.getUserId(), benutzer.getPunkte(), newAbzeichen, benutzer.getNextBelohnung(), benutzer.getNextLike(), benutzer.getNextDaily(), benutzer.getNutzername()));
    }

    public Benutzer setBenutzerNextBelohnung(@NotNull Benutzer benutzer, long nextBelohnung) {
        return replace(benutzer, new Benutzer(benutzer.getUserId(), benutzer.getPunkte(), benutzer.getAbzeichen(), nextBelohnung, benutzer.getNextLike(), benutzer.getNextDaily(), benutzer.getNutzername()));
    }

    public Benutzer setBenutzerNextLike(@NotNull Benutzer benutzer, long nextLike) {
        return replace(benutzer, new Benutzer(benutzer.getUserId(), benutzer.getPunkte(), benutzer.getAbzeichen(), benutzer.getNextBelohnung(), nextLike, benutzer.getNextDaily(), benutzer.getNutzername()));
    }

    public Benutzer setBenutzerNextDaily(@NotNull Benutzer benutzer, long nextDaily) {
        return replace(benutzer, new Benutzer(benutzer.getUserId(), benutzer.getPunkte(), benutzer.getAbzeichen(), benutzer.getNextBelohnung(), benutzer.getNextLike(), nextDaily, benutzer.getNutzername()));
    }

    public Benutzer setBenutzerNutzername(@NotNull Benutzer benutzer, @NotNull String nutzername) {
        return replace(benutzer, new Benutzer(benutzer.getUserId(), benutzer.getPunkte(), benutzer.getAbzeichen(), benutzer.getNextBelohnung(), benutzer.getNextLike(), benutzer.getNextDaily(), nutzername));
    }

    public Benutzer replace(@NotNull Benutzer vorher, @NotNull Benutzer nachher) {
        if (benutzerSet.remove(vorher)) {
            if (benutzerSet.add(nachher)) {
                return nachher;
            }
            benutzerSet.add(vorher);
        }
        return vorher;
    }

    public List<Benutzer> getTopBenuzerList(int limit) {
        return benutzerSet.stream().sorted(Comparator.comparing(Benutzer::getPunkte).reversed()).limit(limit).collect(Collectors.toList());
    }

    public List<Benutzer> getBenuzerList() {
        return benutzerSet.stream().sorted(Comparator.comparing(Benutzer::getUserId)).collect(Collectors.toList());
    }

    public boolean checkBenutzerNutzername(String name) {
        return (name.length() <= 20 && name.matches("^([A-Za-z0-9äöüß_])+$"));
    }
}
