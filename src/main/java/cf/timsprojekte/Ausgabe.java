package cf.timsprojekte;

import cf.timsprojekte.db.Event;
import cf.timsprojekte.db.Nutzer;
import org.telegram.abilitybots.api.sender.MessageSender;
import org.telegram.abilitybots.api.sender.SilentSender;
import org.telegram.telegrambots.meta.api.methods.AnswerCallbackQuery;
import org.telegram.telegrambots.meta.api.methods.AnswerInlineQuery;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.send.SendPhoto;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.*;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.inlinequery.result.InlineQueryResult;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboard;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.io.InputStream;
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

    Ausgabe(int creatorId, List<Long> listGroups, SilentSender silent, Locale groupLocale, MessageSender sender) {
        this.owner = creatorId;
        this.listGroups = listGroups;
        this.silent = silent;
        this.groupLocale = groupLocale;
        this.sender = sender;
        toBeRemoved = new ArrayList<>();
    }

    void answerCallback(String id) {
        silent.execute(new AnswerCallbackQuery().setCallbackQueryId(id));
    }

    private void sendToAllGroups(String key, Locale locale, Object... arguments) {
        listGroups.forEach(group ->
                send(group, locale, key, arguments));
    }

    public void sendRawToAllGroups(String s) {
        listGroups.forEach(group -> {
            System.out.println(group);
            sendRaw(group, s);
        });
    }

    void sendToAllGroups(String key, Object... arguments) {
        sendToAllGroups(key, groupLocale, arguments);
    }

    Optional<Message> sendToGroup(Long chatId, String key, Object... arguments) {
        return send(chatId, groupLocale, key, arguments);
    }

    Optional<Message> send(Long chatId, Locale locale, String key, Object... arguments) {
        return silent.execute(new SendMessage().setChatId(chatId).setText(format(locale, key, arguments)).setParseMode("HTML"));
    }

    void edit(Long chatId, Integer messageId, Locale locale, String key, Object... arguments) {
        silent.execute(new EditMessageText().setText(format(locale, key, arguments)).setChatId(chatId).setMessageId(messageId));
    }

    void editCaption(Long chatId, Integer messageId, String text, InlineKeyboardMarkup replyMarkup) {
        silent.execute(new EditMessageCaption().setCaption(text).setChatId(chatId + "").setMessageId(messageId).setParseMode("HTML").setReplyMarkup(replyMarkup));
    }

    Optional<Message> sendTempRaw(Long chatId, String msg) {
        clear();
        Optional<Message> message = silent.execute(new SendMessage().setChatId(chatId).setText(msg).setParseMode("HTML"));
        message.ifPresent(value -> toBeRemoved.add(value));
        return message;
    }

    Optional<Message> sendTemp(Long id, Locale locale, String key, Object... arguments) {
        Optional<Message> message = silent.execute(new SendMessage().setChatId(id)
                .setText(format(locale, key, arguments))
                .setParseMode("HTML"));
        message.ifPresent(value -> toBeRemoved.add(value));
        return message;
    }

    void sendTempClear(Long id, Locale locale, String key, Object... arguments) {
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

    void sendOwnerGroupCheck(Long chatId, String title) {
        InlineKeyboardMarkup keyboard = InlineKeyboardFactory.build()
                .addRow(InlineKeyboardFactory.button("Hinzufügen", "admGroup_+_" + chatId))
                .addRow(InlineKeyboardFactory.button("Blockieren", "admGroup_-_" + chatId))
                .toMarkup();
        silent.execute(new SendMessage().setText("Die Gruppe \"" + title + "\" <i>(" + chatId + ")</i> benutzt den Bot")
                .setChatId(owner)
                .setParseMode("HTML")
                .setReplyMarkup(keyboard));
    }

    void sendOwnerEventCheck(Nutzer nutzer, Event event) {
        InlineKeyboardMarkup keyboard = InlineKeyboardFactory.build()
                .addRow(InlineKeyboardFactory.button("Hinzufügen", "admEvent_+_" + event.getId()))
                .addRow(InlineKeyboardFactory.button("Blockieren", "admEvent_-_" + event.getId()))
                .toMarkup();
        silent.execute(new SendMessage().setText("Der Nutzer \"" + nutzer.getUsername() + "\" beantragt ein Event\n<b>" + event.getName() + "</b>\n" + event.getDesc() + "\n\nPunkte: " + event.getPoints())
                .setChatId(owner)
                .setParseMode("HTML")
                .setReplyMarkup(keyboard));
    }

    void removeMessage(Long chatId, Integer messageId) {
        silent.execute(new DeleteMessage().setChatId(chatId).setMessageId(messageId));
    }

    Optional<Message> sendRaw(Long chatId, String msg) {
        return silent.execute(new SendMessage().setChatId(chatId).setText(msg).setParseMode("HTML"));
    }

    private void sendImage(Long chatId, Locale locale, String key, String url, Object... arguments) {
        try {
            sender.sendPhoto(new SendPhoto().setChatId(chatId).setCaption(format(locale, key, arguments)).setParseMode("HTML").setPhoto(url));
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }

    void sendImage(Long chatId, String name, InputStream photoStream) {
        try {
            sender.sendPhoto(new SendPhoto().setChatId(chatId).setPhoto(name, photoStream));
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }

    void sendImage(Long chatId, String name, InputStream photoStream, String caption, InlineKeyboardMarkup keyboardMarkup) {
        try {
            sender.sendPhoto(new SendPhoto().setChatId(chatId).setPhoto(name, photoStream).setCaption(caption).setReplyMarkup(keyboardMarkup));
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }

    public void sendTempImage(Long chatId, String name, InputStream photoStream) {
        try {
            clear();
            Message message = sender.sendPhoto(new SendPhoto().setChatId(chatId).setPhoto(name, photoStream));
            toBeRemoved.add(message);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }


    void sendImageToAll(Locale locale, String key, String url, Object... arguments) {
        listGroups.forEach(group ->
                sendImage(group, locale, key, url, arguments));
    }

    Optional<Message> sendKeyboard(Long chatId, ReplyKeyboard keyboard, Locale locale, String key, Object... arguments) {
        return silent.execute(new SendMessage().setChatId(chatId).setText(format(locale, key, arguments)).setParseMode("HTML").setReplyMarkup(keyboard));

    }

    void editKeyboard(Long chatId, Integer messageId, InlineKeyboardMarkup keyboard, Locale locale, String key, Object... arguments) {
        silent.execute(new EditMessageText().setText(format(locale, key, arguments)).setChatId(chatId).setMessageId(messageId).setReplyMarkup(keyboard));
    }

    void editKeyboard(Long chatId, Integer messageId, InlineKeyboardMarkup keyboard) {
        silent.execute(new EditMessageReplyMarkup().setChatId(chatId).setMessageId(messageId).setReplyMarkup(keyboard));
    }

    void removeKeyboard(Long chatId, Integer messageId) {
        silent.execute(new EditMessageReplyMarkup().setMessageId(messageId).setChatId(chatId).setReplyMarkup(null));
    }

    public void answerInlineQuery(String id, String nextOffset, Boolean personal, List<InlineQueryResult> results) {
        silent.execute(new AnswerInlineQuery().setPersonal(personal).setInlineQueryId(id).setNextOffset(nextOffset).setResults(results));
    }
}
