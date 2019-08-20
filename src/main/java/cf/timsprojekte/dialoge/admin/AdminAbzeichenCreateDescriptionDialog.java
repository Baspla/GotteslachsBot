package cf.timsprojekte.dialoge.admin;

import cf.timsprojekte.dialoge.ReplyDialog;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ForceReplyKeyboard;

import java.util.Optional;

public class AdminAbzeichenCreateDescriptionDialog extends ReplyDialog {
    private final String titel;

    public AdminAbzeichenCreateDescriptionDialog(String titel) {
        super("abzdesc", "Beschreibung");
        this.titel = titel;
    }

    @Override
    protected void handleReply(Message message) {
        AdminAbzeichenCreateRewardDialog dialog = new AdminAbzeichenCreateRewardDialog(titel,message.getText());
        addChild(dialog);
        dialog.showDialog(message.getReplyToMessage());
    }

    @Override
    public void showDialog(Message message) {
        Optional<Message> msg = getSender().execute(new SendMessage().setText("Antworte auf diese Nachricht mit der Beschreibung des Abzichens.").setChatId(getChatId()).setReplyMarkup(new ForceReplyKeyboard()));
        msg.ifPresent(value -> lastMessageId = value.getMessageId());
    }
}

