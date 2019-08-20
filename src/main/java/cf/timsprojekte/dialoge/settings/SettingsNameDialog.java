package cf.timsprojekte.dialoge.settings;

import cf.timsprojekte.UniqueBot;
import cf.timsprojekte.dialoge.ReplyDialog;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.DeleteMessage;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ForceReplyKeyboard;

import java.util.Optional;

public class SettingsNameDialog extends ReplyDialog {
    protected SettingsNameDialog() {
        super("chgname", "Name ändern");
    }

    @Override
    protected void handleReply(Message message) {
        String name = message.getText();
        String msg = "";
        if (UniqueBot.unique().nutzerverwaltung.checkBenutzerNutzername(name)) {
            UniqueBot.unique().nutzerverwaltung.setBenutzerNutzername(UniqueBot.unique().nutzerverwaltung.getBenutzer(message.getFrom().getId()), name);
            msg = "Dein Nutzername wurde auf <i>" + name + "</i> geändert.";
        } else {
            msg = "<b>Ungültiger Benutzername!</b>";
        }
        getSender().execute(new SendMessage().setChatId(message.getReplyToMessage().getChatId()).setText(msg).setParseMode("HTML").setReplyMarkup(null));
        //destroy(false);
    }

    @Override
    public void showDialog(Message message) {
        if (message != null)
            getSender().execute(new DeleteMessage(message.getChatId(), message.getMessageId()));
        Optional<Message> msg = getSender().execute(new SendMessage().setText("Antworte auf diese Nachricht mit deinem Neuen Nutzernamen.").setChatId(getChatId()).setReplyMarkup(new ForceReplyKeyboard()));
        msg.ifPresent(value -> lastMessageId = value.getMessageId());
    }
}
