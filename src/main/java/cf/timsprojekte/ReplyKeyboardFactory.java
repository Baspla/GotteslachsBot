package cf.timsprojekte;

import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.*;

import java.lang.reflect.Array;
import java.util.*;
import java.util.stream.Collectors;

public class ReplyKeyboardFactory {


    private ArrayList<List<KeyboardButton>> rows;
    private Boolean oneTime;
    private Boolean resize;
    private Boolean selective;

    public ReplyKeyboardFactory() {
        rows = new ArrayList<>();
    }

    public static ReplyKeyboardFactory build() {
        return new ReplyKeyboardFactory();
    }

    public static KeyboardButton button(String text) {
        return new KeyboardButton(text);
    }

    public ReplyKeyboardFactory addRow(KeyboardButton... buttons) {
        rows.add(Arrays.asList(buttons));
        return this;
    }

    public ReplyKeyboardMarkup toMarkup() {
        List<KeyboardRow> markupRows = rows.stream().map(row -> {
            KeyboardRow keyboard = new KeyboardRow();
            keyboard.addAll(row);
            return keyboard;
        }).collect(Collectors.toList());
        return new ReplyKeyboardMarkup().setKeyboard(markupRows).setOneTimeKeyboard(oneTime).setResizeKeyboard(resize).setSelective(selective);
    }

    public ReplyKeyboardFactory addRow(List<KeyboardButton> buttons) {
        if (buttons == null) return this;
        if (buttons.size() <= 0) return this;
        rows.add(buttons);
        return this;
    }

    public ReplyKeyboardFactory setOneTime(Boolean oneTime) {
        this.oneTime = oneTime;
        return this;
    }

    public ReplyKeyboardFactory setResize(Boolean resize) {
        this.resize = resize;
        return this;
    }

    public ReplyKeyboardFactory setSelective(Boolean selective) {
        this.selective = selective;
        return this;
    }
}
