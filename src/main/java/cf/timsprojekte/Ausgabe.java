package cf.timsprojekte;

import org.telegram.abilitybots.api.sender.MessageSender;
import org.telegram.abilitybots.api.sender.SilentSender;
import org.telegram.telegrambots.meta.api.methods.AnswerCallbackQuery;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.send.SendPhoto;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.*;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboard;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.text.MessageFormat;
import java.util.*;
import java.util.stream.Collectors;

public class Ausgabe {

    private long owner;
    private List<Long> listGroups;
    private SilentSender silent;
    private ArrayList<Message> toBeRemoved;
    private Locale groupLocale;
    private MessageSender sender;

    public Ausgabe(int creatorId, List<Long> listGroups, SilentSender silent, Locale groupLocale, MessageSender sender) {
        this.owner = creatorId;
        this.listGroups = listGroups;
        this.silent = silent;
        this.groupLocale = groupLocale;
        this.sender = sender;
        toBeRemoved = new ArrayList<>();
    }

    public void answerCallback(String id) {
        silent.execute(new AnswerCallbackQuery().setCallbackQueryId(id));
    }

    public void sendToAllGroups(String key, Locale locale, Object... arguments) {
        listGroups.forEach(group ->
                send(group, locale, key, arguments));
    }

    public void sendToAllGroups(String key, Object... arguments) {
        sendToAllGroups(key, groupLocale, arguments);
    }

    public Optional<Message> sendToGroup(Long chatId, String key, Object... arguments) {
        return send(chatId, groupLocale, key, arguments);
    }

    public Optional<Message> send(Long chatId, Locale locale, String key, Object... arguments) {
        return silent.execute(new SendMessage().setChatId(chatId).setText(format(locale, key, arguments)).setParseMode("HTML"));
    }

    public void edit(Long chatId, Integer messageId, Locale locale, String key, Object... arguments) {
        silent.execute(new EditMessageText().setText(format(locale, key, arguments)).setChatId(chatId).setMessageId(messageId));
    }


    public Optional<Message> sendTemp(Long id, Locale locale, String key, Object... arguments) {
        Optional<Message> message = silent.execute(new SendMessage().setChatId(id)
                .setText(format(locale, key, arguments))
                .setParseMode("HTML"));
        message.ifPresent(value -> toBeRemoved.add(value));
        return message;
    }

    public void sendTempClear(Long id, Locale locale, String key, Object... arguments) {
        clear();
        sendTemp(id, locale, key, arguments);
    }

    private void clear() {
        toBeRemoved.forEach(message -> silent.execute(new DeleteMessage(message.getChatId(), message.getMessageId())));
        toBeRemoved.clear();
    }

    public static String format(Locale locale, String key, Object... arguments) {
        try {
            return new MessageFormat(ResourceBundle.getBundle("messages/Nachrichten", locale).getString(key), locale).format(arguments);
        } catch (MissingResourceException e) {
            System.err.println("MISSING [" + key + "][" + locale.toString() + "][" + Arrays.stream(arguments).map(Object::toString).collect(Collectors.joining(",")) + "]");
            return "[" + key + "][" + locale.toString() + "][" + Arrays.stream(arguments).map(Object::toString).collect(Collectors.joining(",")) + "]";
        }
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

    public Optional<Message> sendRaw(Long chatId, String msg) {
        return silent.execute(new SendMessage().setChatId(chatId).setText(msg).setParseMode("HTML"));
    }

    public void sendImage(Long chatId, Locale locale, String key, String url, Object... arguments) {
        try {
            sender.sendPhoto(new SendPhoto().setChatId(chatId).setCaption(format(locale, key, arguments)).setParseMode("HTML").setPhoto(url));
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }

    public void sendImageToAll(Locale locale, String key, String url, Object... arguments) {
        listGroups.forEach(group ->
                sendImage(group, locale, key, url, arguments));
    }

    public Optional<Message> sendKeyboard(Long chatId, ReplyKeyboard keyboard, Locale locale, String key, Object... arguments) {
        return silent.execute(new SendMessage().setChatId(chatId).setText(format(locale, key, arguments)).setParseMode("HTML").setReplyMarkup(keyboard));

    }

    public void editKeyboard(Long chatId, Integer messageId, InlineKeyboardMarkup keyboard, Locale locale, String key, Object... arguments) {
        silent.execute(new EditMessageText().setText(format(locale, key, arguments)).setChatId(chatId).setMessageId(messageId).setReplyMarkup(keyboard));
    }

    public void removeKeyboard(Long chatId, Integer messageId) {
        silent.execute(new EditMessageReplyMarkup().setMessageId(messageId).setChatId(chatId).setReplyMarkup(null));
    }
}
