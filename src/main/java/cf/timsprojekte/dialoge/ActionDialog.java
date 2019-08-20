package cf.timsprojekte.dialoge;

import cf.timsprojekte.DialogCallback;
import org.telegram.telegrambots.meta.api.methods.AnswerCallbackQuery;
import org.telegram.telegrambots.meta.api.objects.Message;

import javax.validation.constraints.NotNull;

public abstract class ActionDialog extends Dialog {

    protected ActionDialog(@NotNull String identifier, @NotNull String messageTitle) {
        super(identifier, messageTitle);
    }

    @Override
    protected void processCallback(DialogCallback callback) {
        getSender().execute(new AnswerCallbackQuery().setCallbackQueryId(callback.getQuery().getId()));
        if (callback.getData().isEmpty())
            showDialog(callback.getQuery().getMessage());
    }

    @Override
    public void showDialog(Message message) {
        action(message);
        destroy(false);
    }

    protected abstract void action(Message message);

}
