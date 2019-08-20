package cf.timsprojekte.verwaltung.immutable;

import java.io.Serializable;
import java.util.Objects;

public class Abzeichen implements Serializable {
    public static final long serialVersionUID = 187133724869666L;
    private final int abzeichenId;
    private final int belohnung;
    private final String name;
    private final String beschreibung;

    public Abzeichen(int abzeichenId, int belohnung, String name, String beschreibung) {
        this.abzeichenId = abzeichenId;
        this.belohnung = belohnung;
        this.name = name;
        this.beschreibung = beschreibung;
    }

    public int getAbzeichenId() {
        return abzeichenId;
    }

    public int getBelohnung() {
        return belohnung;
    }

    public String getName() {
        return name;
    }

    public String getBeschreibung() {
        return beschreibung;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Abzeichen abzeichen = (Abzeichen) o;
        return abzeichenId == abzeichen.abzeichenId;
    }

    @Override
    public int hashCode() {
        return Objects.hash(abzeichenId);
    }
}
