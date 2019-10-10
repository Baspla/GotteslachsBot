package cf.timsprojekte.db;

import cf.timsprojekte.Ausgabe;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.io.Serializable;
import java.time.Instant;
import java.time.temporal.TemporalUnit;
import java.util.Locale;

import cf.timsprojekte.LevelManager;

public class Nutzer implements Serializable {

    public static final long serialVersionUID = 187111222333L;
    @JsonProperty
    private Integer votes;
    @JsonProperty
    private String username;
    @JsonProperty
    private Integer userId;
    @JsonProperty
    private Integer points;
    @JsonProperty
    private String lang_language;
    @JsonProperty
    private String lang_country;
    @JsonProperty
    private String lang_variant;

    private transient Instant cooldownSuperUpvote = Instant.now();
    private transient Instant cooldownUpvote = Instant.now();
    private transient Instant cooldownDownvote = Instant.now();
    private transient Instant cooldownReward = Instant.now();
    private transient NutzerManager nutzerManager;


    public Nutzer(Integer userId, String username, int points, int votes) {
        this.userId = userId;
        this.points = points;
        this.username = username;
        this.votes = votes;
    }

    public Integer getVotes() {
        return votes;
    }

    public void setVotes(int votes) {
        this.votes = votes;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public Integer getUserId() {
        return userId;
    }

    public void setUserId(Integer userId) {
        this.userId = userId;
    }

    public Integer getPoints() {
        return points;
    }

    public void setPoints(int points) {
        this.points = points;
    }

    public String getTitel() {
        return LevelManager.getTitelForPoints(getPoints(), getLocale());
    }

    public void addPoints(int points) {
        setPoints(getPoints() + points);
    }

    private Integer getPointsNextLevel() {
        return LevelManager.getZiel(getLevel());
    }

    public String getLinkedVotes() {
        return Ausgabe.format(getLocale(),"user.namevotes",getVotes(),getUserId(),getUsername());
    }

    public String getLinkedPoints() {
        return Ausgabe.format(getLocale(),"user.titlenamepoints",getVotes(),getUserId(),getUsername());
    }

    public String getLinkedTitleUsername() {
        return Ausgabe.format(getLocale(),"user.titlename",getVotes(),getUserId(),getUsername());
    }

    public String getLinkedUsername() {
        return Ausgabe.format(getLocale(),"user.name",getVotes(),getUserId(),getUsername());
    }

    public String getLinkedPointListEntry() {
        return Ausgabe.format(getLocale(),"user.entry.points",getVotes(),getUserId(),getUsername());
    }

    public String getLinkedVoteListEntry() {
        return Ausgabe.format(getLocale(),"user.entry.votes",getVotes(),getUserId(),getUsername());
    }

    public void removeVote(int i) {
        setVotes(votes - i);
    }

    public void addVote(int i) {
        setVotes(votes + i);
    }

    public void setCooldownSuperUpvote(int time, TemporalUnit unit) {
        cooldownSuperUpvote = Instant.now().plus(time, unit);
    }

    public void setCooldownUpvote(int time, TemporalUnit unit) {
        cooldownUpvote = Instant.now().plus(time, unit);
    }

    public void setCooldownDownvote(int time, TemporalUnit unit) {
        cooldownDownvote = Instant.now().plus(time, unit);
    }

    public void setCooldownReward(int time, TemporalUnit unit) {
        cooldownReward = Instant.now().plus(time, unit);
    }

    private Instant nowIfNull(Instant instant) {
        return (instant == null) ? Instant.now() : instant;
    }

    public Instant getCooldownSuperUpvote() {
        return nowIfNull(cooldownSuperUpvote);
    }

    public Instant getCooldownUpvote() {
        return nowIfNull(cooldownUpvote);
    }

    public Instant getCooldownDownvote() {
        return nowIfNull(cooldownDownvote);
    }

    public Instant getCooldownReward() {
        return nowIfNull(cooldownReward);
    }

    public boolean hasCooldownDownvote() {
        return getCooldownDownvote().isAfter(Instant.now());
    }

    public boolean hasCooldownSuperUpvote() {
        return getCooldownSuperUpvote().isAfter(Instant.now());
    }

    public boolean hasCooldownUpvote() {
        return getCooldownUpvote().isAfter(Instant.now());
    }

    public boolean hasCooldownReward() {
        return getCooldownReward().isAfter(Instant.now());
    }

    public void setManager(NutzerManager nutzerManager) {
        this.nutzerManager = nutzerManager;
    }

    public Locale getLocale() {
        if (lang_language != null && !lang_language.isEmpty())
            if (lang_country != null && !lang_country.isEmpty())
                if (lang_variant != null && !lang_variant.isEmpty())
                    return new Locale(lang_language, lang_country, lang_variant);
                else
                    return new Locale(lang_language, lang_country);
            else
                return new Locale(lang_language);
        else
            return Locale.getDefault();
    }

    public void setLocale(String language, String country, String variant) {
        lang_language = language;
        lang_country = country;
        lang_variant = variant;
    }

    public int getLevel() {
        return LevelManager.getLevel(getPoints());
    }
}
