package cf.timsprojekte.verwaltung;

import cf.timsprojekte.verwaltung.immutable.Gruppe;
import org.telegram.abilitybots.api.db.DBContext;

import javax.validation.constraints.NotNull;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

public class Gruppenverwaltung {
    private final Set<Gruppe> gruppenSet;

    public Gruppenverwaltung(DBContext db) {
        gruppenSet = db.getSet("Gruppen");
    }

    public Gruppe getGruppe(long chatId) {
        Optional<Gruppe> abzeichen = gruppenSet.stream().filter(b -> b.getChatId() == chatId).findAny();
        return abzeichen.orElse(null);
    }

    public void createGruppe(@NotNull Gruppe gruppe) {
        if (gruppenSet.contains(gruppe)) return;
        gruppenSet.add(gruppe);
    }

    public void removeGruppe(Gruppe gruppe) {
        gruppenSet.remove(gruppe);
    }

    public Gruppe setGruppeIsAusgabe(@NotNull Gruppe gruppe, boolean isAusgabe) {
        return replace(gruppe, new Gruppe(gruppe.getChatId(), isAusgabe, gruppe.getLastMessage()));
    }

    public Gruppe setGruppeLastMessage(@NotNull Gruppe gruppe, int messageId) {
        return replace(gruppe, new Gruppe(gruppe.getChatId(), gruppe.getIsAusgabe(),messageId));
    }

    public Gruppe replace(@NotNull Gruppe vorher, @NotNull Gruppe nachher) {
        if (gruppenSet.remove(vorher)) {
            if (gruppenSet.add(nachher)) {
                return nachher;
            }
            gruppenSet.add(vorher);
        }
        return vorher;
    }

    public Set<Gruppe> getAusgabegruppen() {
        return gruppenSet.stream().filter(Gruppe::getIsAusgabe).collect(Collectors.toSet());
    }
}
