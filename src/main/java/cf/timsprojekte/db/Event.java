package cf.timsprojekte.db;

import com.fasterxml.jackson.annotation.*;
import org.jetbrains.annotations.NotNull;

import java.io.Serializable;
import java.util.Objects;

public class Event implements Serializable, Comparable<Event> {

    public static final long serialVersionUID = 18769696969L;

    private String name;
    private String desc;
    private int points;
    private int creator;
    private int id;
    private boolean qr;
    private boolean groupJoin;
    private boolean finished;
    private boolean accepted;

    public Event(String name, String desc, int points, int id, int creator) {
        this.name = name;
        this.desc = desc;
        this.points = points;
        this.id = id;
        this.creator = creator;
        qr=false;
        groupJoin=false;
        finished=false;
        accepted=false;
    }

    public int getId() {
        return id;
    }

    public int getPoints() {
        return points;
    }

    public String getDesc() {
        return desc;
    }

    public String getName() {
        return name;
    }

    public int getCreator() {
        return creator;
    }

    public boolean isQr() {
        return qr;
    }

    public boolean isGroupJoin() {
        return groupJoin;
    }

    public boolean isFinished() {
        return finished;
    }

    public boolean isAccepted() {
        return accepted;
    }

    public void setQr(boolean qr) {
        this.qr = qr;
    }

    public void setGroupJoin(boolean groupJoin) {
        this.groupJoin = groupJoin;
    }

    public void setFinished(boolean finished) {
        this.finished = finished;
    }

    public void setAccepted(boolean accepted) {
        this.accepted = accepted;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Event event = (Event) o;
        return id == event.id;
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public int compareTo(@NotNull Event o) {
        return o.getId() - getId();
    }
}
