package cf.timsprojekte.dialoge;

import cf.timsprojekte.DialogCallback;
import cf.timsprojekte.ReplyListener;
import org.telegram.telegrambots.meta.api.methods.AnswerCallbackQuery;
import org.telegram.telegrambots.meta.api.objects.Message;

import javax.validation.constraints.NotNull;

public abstract class ReplyDialog extends Dialog implements ReplyListener {
    protected long lastMessageId;

    protected ReplyDialog(@NotNull String identifier, @NotNull String messageTitle) {
        super(identifier, messageTitle);
    }

    @Override
    protected void processCallback(DialogCallback callback) {
        if (callback.getData().isEmpty()) {
            showDialog(callback.getQuery().getMessage());
            registerReplyListener(this);
            getSender().execute(new AnswerCallbackQuery().setCallbackQueryId(callback.getQuery().getId()));
        }
    }


    public void onReply(Message message) {
        handleReply(message);
    }

    protected abstract void handleReply(Message message);

    @Override
    public long listeningForMessageId() {
        return lastMessageId;
    }
}
