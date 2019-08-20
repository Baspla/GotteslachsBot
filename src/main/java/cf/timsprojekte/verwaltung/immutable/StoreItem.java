package cf.timsprojekte.verwaltung.immutable;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.io.Serializable;
import java.util.Objects;

public class StoreItem implements Serializable {
    public static final long serialVersionUID = 187133724869666L;
    private final int id;
    private final int value;
    private final String name;
    private final String desc;
    private final int limit;

    public StoreItem(@JsonProperty("id") int id, @JsonProperty("value") int value, @JsonProperty("name") String name, @JsonProperty("desc") String desc, @JsonProperty("limit") int limit) {
        this.id = id;
        this.value = value;
        this.name = name;
        this.desc = desc;
        this.limit = limit;
    }

    public int getId() {
        return id;
    }

    public int getValue() {
        return value;
    }

    public String getName() {
        return name;
    }

    public String getDesc() {
        return desc;
    }

    public int getLimit() {
        return limit;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        StoreItem storeItem = (StoreItem) o;
        return id == storeItem.id;
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
