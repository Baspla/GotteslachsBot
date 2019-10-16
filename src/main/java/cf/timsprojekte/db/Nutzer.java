package cf.timsprojekte.db;

import cf.timsprojekte.*;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.apache.commons.codec.language.bm.Lang;

import java.io.Serializable;
import java.time.Instant;
import java.time.temporal.TemporalUnit;
import java.util.*;
import java.util.stream.Collectors;

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

    @JsonIgnore
    private Instant cooldownSuperUpvote = Instant.now();
    @JsonIgnore
    private Instant cooldownUpvote = Instant.now();
    @JsonIgnore
    private Instant cooldownDownvote = Instant.now();
    @JsonIgnore
    private Instant cooldownReward = Instant.now();
    @JsonIgnore
    private State state;
    private Set<Language> languages;


    public Nutzer(Integer userId, String username, int points, int votes, Set<Language> languages) {
        this.userId = userId;
        this.points = points;
        this.username = username;
        this.votes = votes;
        this.languages = languages;
    }

    public Nutzer(Integer userId, String username, int points, int votes) {
        this.userId = userId;
        this.points = points;
        this.username = username;
        this.votes = votes;
        this.languages = new HashSet<>();
    }

    public Set<Language> getLanguages() {
        if (languages == null) languages = new HashSet<>();
        return new HashSet<>(languages);
    }

    public void addLanguage(Language lang) {
        if (languages == null) languages = new HashSet<>();
        languages.add(lang);
    }

    public void removeLanguage(Language lang) {
        if (languages == null) languages = new HashSet<>();
        languages.remove(lang);
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

    @JsonIgnore
    public String getTitel() {
        return LevelManager.getTitelForPoints(getPoints(), getLocale());
    }

    @JsonIgnore
    public void addPoints(int points) {
        setPoints(getPoints() + points);
    }

    @JsonIgnore
    public void removePoints(int points) {
        setPoints(getPoints() - points);
    }

    @JsonIgnore
    private Integer getPointsNextLevel() {
        return LevelManager.getZiel(getLevel());
    }

    @JsonIgnore
    public String getLinkedVotes() {
        return Ausgabe.format(getLocale(), "user.namevotes", getUserId(), getUsername(), getVotes());
    }

    @JsonIgnore
    public String getLinkedPoints() {
        return Ausgabe.format(getLocale(), "user.titlenamepoints", getTitel(), getUserId(), getUsername(), getPoints(), getPointsNextLevel());
    }

    @JsonIgnore
    public String getLinkedTitleUsername() {
        return Ausgabe.format(getLocale(), "user.titlename", getTitel(), getUserId(), getUsername());
    }

    @JsonIgnore
    public String getLinkedUsername() {
        return Ausgabe.format(getLocale(), "user.name", getUserId(), getUsername());
    }

    @JsonIgnore
    public String getLinkedPointListEntry() {
        return Ausgabe.format(getLocale(), "user.entry.points", getLevel(), getTitel(), getUserId(), getUsername(), getPoints(), getPointsNextLevel());
    }

    @JsonIgnore
    public String getLinkedVoteListEntry() {
        return Ausgabe.format(getLocale(), "user.entry.votes", getVotes(), getUserId(), getUsername());
    }

    @JsonIgnore
    public void removeVote(int i) {
        setVotes(votes - i);
    }

    @JsonIgnore
    public void addVote(int i) {
        setVotes(votes + i);
    }

    @JsonIgnore
    public void setCooldownSuperUpvote(int time, TemporalUnit unit) {
        cooldownSuperUpvote = Instant.now().plus(time, unit);
    }

    @JsonIgnore
    public void setCooldownUpvote(int time, TemporalUnit unit) {
        cooldownUpvote = Instant.now().plus(time, unit);
    }

    @JsonIgnore
    public void setCooldownDownvote(int time, TemporalUnit unit) {
        cooldownDownvote = Instant.now().plus(time, unit);
    }

    @JsonIgnore
    public void setCooldownReward(int time, TemporalUnit unit) {
        cooldownReward = Instant.now().plus(time, unit);
    }

    @JsonIgnore
    private Instant nowIfNull(Instant instant) {
        return (instant == null) ? Instant.now() : instant;
    }

    @JsonIgnore
    private Instant getCooldownSuperUpvote() {
        return nowIfNull(cooldownSuperUpvote);
    }

    @JsonIgnore
    private Instant getCooldownUpvote() {
        return nowIfNull(cooldownUpvote);
    }

    @JsonIgnore
    private Instant getCooldownDownvote() {
        return nowIfNull(cooldownDownvote);
    }

    @JsonIgnore
    private Instant getCooldownReward() {
        return nowIfNull(cooldownReward);
    }

    @JsonIgnore
    public boolean hasCooldownDownvote() {
        return getCooldownDownvote().isAfter(Instant.now());
    }

    @JsonIgnore
    public boolean hasCooldownSuperUpvote() {
        return getCooldownSuperUpvote().isAfter(Instant.now());
    }

    @JsonIgnore
    public boolean hasCooldownUpvote() {
        return getCooldownUpvote().isAfter(Instant.now());
    }

    @JsonIgnore
    public boolean hasCooldownReward() {
        return getCooldownReward().isAfter(Instant.now());
    }

    @JsonIgnore
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

    @JsonIgnore
    public void setLocale(String language, String country, String variant) {
        lang_language = language;
        lang_country = country;
        lang_variant = variant;
    }

    @JsonIgnore
    public int getLevel() {
        return LevelManager.getLevel(getPoints());
    }

    @JsonIgnore
    public void setState(State state) {
        this.state = state;
    }

    @JsonIgnore
    public State getState() {
        if (state == null) state = State.Default;
        return state;
    }

}
