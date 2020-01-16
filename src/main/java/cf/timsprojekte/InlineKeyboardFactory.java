package cf.timsprojekte;

import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;

import java.util.*;

public class InlineKeyboardFactory {
    private ArrayList<List<InlineKeyboardButton>> rows;

    private InlineKeyboardFactory() {
        rows = new ArrayList<>();
    }

    static InlineKeyboardFactory build() {
        return new InlineKeyboardFactory();
    }

    public static InlineKeyboardButton button(String text, String data) {
        return new InlineKeyboardButton(text).setCallbackData(data);
    }

    InlineKeyboardFactory addRow(InlineKeyboardButton... buttons) {
        ArrayList<InlineKeyboardButton> buttonList = new ArrayList<>();
        Collections.addAll(buttonList, buttons);
        rows.add(buttonList);
        return this;
    }

    InlineKeyboardMarkup toMarkup() {
        return new InlineKeyboardMarkup().setKeyboard(rows);
    }

    InlineKeyboardFactory addRow(ArrayList<InlineKeyboardButton> buttons) {
        if (buttons == null) return this;
        if (buttons.size() <= 0) return this;
        rows.add(buttons);
        return this;
    }

    boolean hasRows() {
        return !rows.isEmpty();
    }
}
