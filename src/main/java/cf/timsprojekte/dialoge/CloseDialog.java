package cf.timsprojekte.dialoge;

import org.telegram.telegrambots.meta.api.methods.updatingmessages.DeleteMessage;
import org.telegram.telegrambots.meta.api.objects.Message;

public class CloseDialog extends ActionDialog {
    public CloseDialog() {
        super("close", "Close");
    }

    @Override
    protected void action(Message message) {
        destroy(false);
        getSender().execute(new DeleteMessage(message.getChatId(), message.getMessageId()));
    }
}
