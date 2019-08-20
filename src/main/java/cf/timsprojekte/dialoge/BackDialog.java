package cf.timsprojekte.dialoge;

import cf.timsprojekte.DialogCallback;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.DeleteMessage;
import org.telegram.telegrambots.meta.api.objects.Message;

public class BackDialog extends Dialog {

    public BackDialog() {
        super("back", "Back");
    }

    @Override
    protected void processCallback(DialogCallback callback) {
        if (callback.getData().isEmpty())
            showDialog(callback.getQuery().getMessage());
    }

    @Override
    public void showDialog(Message message) {
        if (getParent().getParent().getIdentifier() == "root")
            getSender().execute(new DeleteMessage(message.getChatId(), message.getMessageId()));
        else
            getParent().getParent().showDialog(message);
    }

}
