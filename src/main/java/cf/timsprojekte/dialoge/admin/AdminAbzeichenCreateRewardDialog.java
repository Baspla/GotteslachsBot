package cf.timsprojekte.dialoge.admin;

import cf.timsprojekte.dialoge.ReplyDialog;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ForceReplyKeyboard;

import java.util.Optional;

public class AdminAbzeichenCreateRewardDialog extends ReplyDialog {
    private final String titel;
    private final String description;

    public AdminAbzeichenCreateRewardDialog(String text,String description) {
        super("abzrew", "Reward");
        this.titel = text;
        this.description=description;
    }

    @Override
    protected void handleReply(Message message) {
        int reward;
        try {
            reward = Integer.valueOf(message.getText());
            AdminAbzeichenCreateUsersDialog dialog = new AdminAbzeichenCreateUsersDialog(titel,description, reward);
            addChild(dialog);
            dialog.showDialog(null);
        } catch (NumberFormatException e) {
            getSender().send("Keine Zulässige Zahl.", message.getChatId());
        }
    }

    @Override
    public void showDialog(Message message) {
        Optional<Message> msg = getSender().execute(new SendMessage().setText("Antworte auf diese Nachricht mit der Belohnung des Abzichens.").setChatId(getChatId()).setReplyMarkup(new ForceReplyKeyboard()));
        msg.ifPresent(value -> lastMessageId = value.getMessageId());

    }
}
