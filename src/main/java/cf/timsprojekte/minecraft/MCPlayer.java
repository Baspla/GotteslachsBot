package cf.timsprojekte.minecraft;

import java.util.Objects;

public class MCPlayer {
    private final String name;
    private final String id;

    public MCPlayer(String name, String id) {
        this.name=name;
        this.id=id;
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        MCPlayer player = (MCPlayer) o;
        return Objects.equals(name, player.name) &&
                Objects.equals(id, player.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, id);
    }
}
