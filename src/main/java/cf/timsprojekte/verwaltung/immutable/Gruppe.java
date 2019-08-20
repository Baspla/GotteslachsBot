package cf.timsprojekte.verwaltung.immutable;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.io.Serializable;
import java.util.Objects;

public class Gruppe implements Serializable {
    public static final long serialVersionUID = 1871337513248766L;

    private final long chatId;
    private final boolean isAusgabe;
    private final int lastMessage;

    @JsonCreator
    public Gruppe(@JsonProperty("chatId") long chatId, @JsonProperty("isAusgabe") boolean isAusgabe,@JsonProperty("messageId") int messageId) {
        this.chatId = chatId;
        this.isAusgabe = isAusgabe;
        this.lastMessage=messageId;
    }

    public long getChatId() {
        return chatId;
    }

    public boolean getIsAusgabe() {
        return isAusgabe;
    }

    public int getLastMessage() {
        return lastMessage;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Gruppe gruppe = (Gruppe) o;
        return chatId == gruppe.chatId;
    }

    @Override
    public int hashCode() {
        return Objects.hash(chatId);
    }
}
