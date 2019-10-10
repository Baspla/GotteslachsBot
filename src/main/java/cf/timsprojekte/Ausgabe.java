package cf.timsprojekte;

import org.telegram.abilitybots.api.sender.SilentSender;
import org.telegram.telegrambots.meta.api.methods.AnswerCallbackQuery;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.DeleteMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;

import java.text.MessageFormat;
import java.util.*;

public class Ausgabe {

    private long owner;
    private List<Long> listGroups;
    private SilentSender silent;
    private ArrayList<Message> toBeRemoved;
    private Locale groupLocale;

    public Ausgabe(int creatorId, List<Long> listGroups, SilentSender silent, Locale groupLocale) {
        this.owner = creatorId;
        this.listGroups = listGroups;
        this.silent = silent;
        this.groupLocale = groupLocale;
        toBeRemoved = new ArrayList<>();
    }

    public void answerCallback(String id) {
        silent.execute(new AnswerCallbackQuery().setCallbackQueryId(id));
    }

    public void sendToAllGroups(String key, Locale locale, Object[] arguments) {
        if (silent != null)
            listGroups.forEach(group ->
                    send(group, locale, key, arguments));
    }

    public void sendToAllGroups(String key, Object[] arguments) {
        sendToAllGroups(key, groupLocale, arguments);
    }

    public Optional<Message> sendToGroup(Long chatId, String key, Object[] arguments) {
        return send(chatId, groupLocale, key, arguments);
    }

    public Optional<Message> send(Long chatId, Locale locale, String key, Object[] arguments) {
        return silent.execute(new SendMessage().setChatId(chatId).setText(format(locale, key, arguments)).setParseMode("HTML"));
    }

    public void edit(Long chatId, Integer messageId, Locale locale, String key, Object[] arguments) {
        silent.execute(new EditMessageText().setText(format(locale, key, arguments)).setChatId(chatId).setMessageId(messageId));
    }


    public Optional<Message> sendTemp(Long id, Locale locale, String key, Object arguments) {
        if (silent == null) return Optional.empty();
        Optional<Message> message = silent.execute(new SendMessage().setChatId(id)
                .setText(new MessageFormat(ResourceBundle.getBundle("Nachrichten", locale).getString(key), locale).format(arguments))
                .setParseMode("HTML"));
        message.ifPresent(value -> toBeRemoved.add(value));
        return message;
    }

    public void sendTempClear(Long id, Locale locale, String key, Object arguments) {
        if (silent == null) return;
        clear();
        sendTemp(id, locale, key, arguments);
    }

    private void clear() {
        toBeRemoved.forEach(message -> silent.execute(new DeleteMessage(message.getChatId(), message.getMessageId())));
        toBeRemoved.clear();
    }

    public String format(Locale locale, String key, Object[] arguments) {
        return new MessageFormat(ResourceBundle.getBundle("Nachrichten", locale).getString(key), locale).format(arguments);
    }

    public void sendOwnerGroupCheck(Long chatId, String title) {
        InlineKeyboardMarkup keyboard = InlineKeyboardFactory.build()
                .addRow(InlineKeyboardFactory.button("Hinzufügen", "adm+" + chatId))
                .addRow(InlineKeyboardFactory.button("Blockieren", "adm-" + chatId))
                .toMarkup();
        silent.execute(new SendMessage().setText("Die Gruppe \"" + title + "\" <i>(" + chatId + ")</i> benutzt den Bot")
                .setChatId(owner)
                .setParseMode("HTML")
                .setReplyMarkup(keyboard));
    }


    public void removeMessage(Long chatId, Integer messageId) {
        silent.execute(new DeleteMessage().setChatId(chatId).setMessageId(messageId));
    }

    public void sendRaw(Long chatId, String toString) {

    }
}
