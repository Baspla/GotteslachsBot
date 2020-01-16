package cf.timsprojekte.db;

import java.io.Serializable;

public class Riddle implements Serializable {

    public static final long serialVersionUID = 7671285687962556326L;

    private String code;
    private String titel;
    private String text;
    private String antwort;
    private String belohnung;
    private boolean abgeschlossen;
    private boolean einmalig;
    private boolean benachrichtigen;
    private int creator;

    public Riddle(String code, String titel, String text, String antwort, String belohnung, boolean abgeschlossen, boolean einmalig, boolean benachrichtigen, int creator) {
        this.code = code;
        this.titel = titel;
        this.text = text;
        this.antwort = antwort;
        this.belohnung = belohnung;
        this.abgeschlossen = abgeschlossen;
        this.einmalig = einmalig;
        this.benachrichtigen = benachrichtigen;
        this.creator = creator;
    }

    public boolean isAbgeschlossen() {
        return abgeschlossen;
    }

    public void setAbgeschlossen(boolean abgeschlossen) {
        this.abgeschlossen = abgeschlossen;
    }

    public int getCreator() {
        return creator;
    }

    public void setCreator(int creator) {
        this.creator = creator;
    }

    public String getTitel() {
        return titel;
    }

    public String getText() {
        return text;
    }

    public String getAntwort() {
        return antwort;
    }

    public String getBelohnung() {
        return belohnung;
    }

    public boolean isEinmalig() {
        return einmalig;
    }

    public boolean isBenachrichtigen() {
        return benachrichtigen;
    }

    public String getCode() {
        return code;
    }
}
