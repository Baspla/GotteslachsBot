package cf.timsprojekte;

import org.telegram.telegrambots.meta.api.objects.Message;

public interface ReplyListener {
    void onReply(Message message);

    long listeningForMessageId();
}
