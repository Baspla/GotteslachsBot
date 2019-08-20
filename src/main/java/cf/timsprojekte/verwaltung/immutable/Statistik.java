package cf.timsprojekte.verwaltung.immutable;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.io.Serializable;
import java.util.Objects;

public class Statistik implements Serializable {
    public static final long serialVersionUID = 1871337424269123L;
    private final long userId;
    private final int text, sticker, photo, video, voice;
    private final int got, lost, won, spent;
    private final int monday, tuesday, thursday, wednesday, friday, saturaday, sunday;
    private final int night, morning, day, evening;
    private final int likeIn, likeOut;
    private final int rewards, dailys;


    @JsonCreator
    public Statistik(@JsonProperty("userId") long userId, @JsonProperty("text") int text, @JsonProperty("sticker") int sticker, @JsonProperty("photo") int photo, @JsonProperty("video") int video, @JsonProperty("voice") int voice,
                     @JsonProperty("got") int got, @JsonProperty("lost") int lost, @JsonProperty("won") int won, @JsonProperty("spent") int spent,
                     @JsonProperty("monday") int monday, @JsonProperty("tuesday") int tuesday, @JsonProperty("wednesday") int wednesday, @JsonProperty("thursday") int thursday,
                     @JsonProperty("friday") int friday, @JsonProperty("saturday") int saturaday, @JsonProperty("sunday") int sunday,
                     @JsonProperty("night") int night, @JsonProperty("morning") int morning, @JsonProperty("day") int day, @JsonProperty("evening") int evening,
                     @JsonProperty("likeIn") int likeIn, @JsonProperty("likeOut") int likeOut, @JsonProperty("rewards") int rewards, @JsonProperty("dailys") int dailys) {
        this.userId = userId;
        this.text = text;
        this.sticker = sticker;
        this.photo = photo;
        this.video = video;
        this.voice = voice;
        this.got = got;
        this.lost = lost;
        this.won = won;
        this.spent = spent;
        this.monday = monday;
        this.tuesday = tuesday;
        this.thursday = thursday;
        this.wednesday = wednesday;
        this.friday = friday;
        this.saturaday = saturaday;
        this.sunday = sunday;
        this.night = night;
        this.morning = morning;
        this.day = day;
        this.evening = evening;
        this.likeIn = likeIn;
        this.likeOut = likeOut;
        this.rewards = rewards;
        this.dailys = dailys;
    }

    public long getUserId() {
        return userId;
    }

    public int getText() {
        return text;
    }

    public int getSticker() {
        return sticker;
    }

    public int getPhoto() {
        return photo;
    }

    public int getVideo() {
        return video;
    }

    public int getVoice() {
        return voice;
    }

    public int getGot() {
        return got;
    }

    public int getLost() {
        return lost;
    }

    public int getWon() {
        return won;
    }

    public int getSpent() {
        return spent;
    }

    public int getMonday() {
        return monday;
    }

    public int getTuesday() {
        return tuesday;
    }

    public int getThursday() {
        return thursday;
    }

    public int getWednesday() {
        return wednesday;
    }

    public int getFriday() {
        return friday;
    }

    public int getSaturaday() {
        return saturaday;
    }

    public int getSunday() {
        return sunday;
    }

    public int getNight() {
        return night;
    }

    public int getMorning() {
        return morning;
    }

    public int getDay() {
        return day;
    }

    public int getEvening() {
        return evening;
    }

    public int getLikeIn() {
        return likeIn;
    }

    public int getLikeOut() {
        return likeOut;
    }

    public int getRewards() {
        return rewards;
    }

    public int getDailys() {
        return dailys;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Statistik statistik = (Statistik) o;
        return userId == statistik.userId;
    }

    @Override
    public int hashCode() {
        return Objects.hash(userId);
    }
}
