package cf.timsprojekte.dialoge.admin;

import cf.timsprojekte.dialoge.ReplyDialog;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.DeleteMessage;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ForceReplyKeyboard;

import java.util.Optional;

public class AdminAbzeichenCreateDialog extends ReplyDialog {
    public AdminAbzeichenCreateDialog() {
        super("abzname", "Erstellen");
    }

    @Override
    protected void handleReply(Message message) {
        AdminAbzeichenCreateDescriptionDialog dialog = new AdminAbzeichenCreateDescriptionDialog(message.getText());
        addChild(dialog);
        dialog.showDialog(message.getReplyToMessage());
    }

    @Override
    public void showDialog(Message message) {
        if (message != null)
            getSender().execute(new DeleteMessage(message.getChatId(), message.getMessageId()));
        Optional<Message> msg = getSender().execute(new SendMessage().setText("Antworte auf diese Nachricht mit dem Namen des Abzichens.").setChatId(getChatId()).setReplyMarkup(new ForceReplyKeyboard()));
        msg.ifPresent(value -> lastMessageId = value.getMessageId());
    }
}
