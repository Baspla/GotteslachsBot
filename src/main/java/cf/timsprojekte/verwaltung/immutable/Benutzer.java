package cf.timsprojekte.verwaltung.immutable;


import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.io.Serializable;
import java.util.Objects;

public class Benutzer implements Serializable {
    public static final long serialVersionUID = 1871337424269666L;
    private final long userId;
    private final int punkte;
    private final int[] abzeichen;
    private final long nextBelohnung;
    private final long nextLike;
    private final long nextDaily;
    private final String nutzername;

    @JsonCreator
    public Benutzer(@JsonProperty("userId") long userId, @JsonProperty("punkte") int punkte, @JsonProperty("abzeichen") int[] abzeichen, @JsonProperty("nextBelohnung") long nextBelohnung, @JsonProperty("nextLike") long nextLike, @JsonProperty("nextDaily") long nextDaily, @JsonProperty("nutzername") String nutzername) {
        this.userId = userId;
        this.punkte = punkte;
        this.abzeichen = abzeichen.clone();
        this.nextBelohnung = nextBelohnung;
        this.nextLike = nextLike;
        this.nextDaily = nextDaily;
        this.nutzername = nutzername;
    }


    public long getUserId() {
        return userId;
    }

    public int getPunkte() {
        return punkte;
    }

    public int[] getAbzeichen() {
        return abzeichen.clone();
    }

    public long getNextBelohnung() {
        return nextBelohnung;
    }

    public long getNextLike() {
        return nextLike;
    }

    public long getNextDaily() {
        return nextDaily;
    }

    public String getNutzername() {
        return nutzername;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Benutzer benutzer = (Benutzer) o;
        return userId == benutzer.userId;
    }

    @Override
    public int hashCode() {
        return Objects.hash(userId);
    }

    public boolean hasAbzeichen(int abzeichenId) {
        for (int value : abzeichen) {
            if (value == abzeichenId) return true;
        }
        return false;
    }
}
