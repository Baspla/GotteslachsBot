package cf.timsprojekte.db;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.io.Serializable;
import java.time.Instant;
import java.time.temporal.TemporalUnit;
import java.util.Locale;

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

    private String getTitel() {
        return "Ehrenbruder";
        //TODO
    }

    public void addPoints(int points) {
        setPoints(getPoints() + points);
    }

    public String getLinkedStringVotes() {
        return "<a href=\"tg://user?id=" + getUserId() + "\">" + getUsername() + "</a> (<code>" + getVotes() + "</code>)";
    }

    public String getLinkedString() {
        return getTitel() + " <a href=\"tg://user?id=" + getUserId() + "\">" + getUsername() + "</a>";
    }

    public String getLinkedStringPointList() {
        return " <code>" + String.format("% 5d", getPoints()) + "</code> <b>" + getTitel() + "</b> <a href=\"tg://user?id=" + getUserId() + "\">" + getUsername() + "</a>";
    }

    public String getLinkedStringVoteList() {
        return "<code>" + String.format("% 4d", getVotes()) + " Ehre</code> - <a href=\"tg://user?id=" + getUserId() + "\">" + getUsername() + "</a>";
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
        return new Locale("de","DE","brudi");
    }
}
