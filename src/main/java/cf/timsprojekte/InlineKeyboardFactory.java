package cf.timsprojekte;

import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;

import java.util.ArrayList;
import java.util.List;

public class InlineKeyboardFactory {
    private ArrayList<List<InlineKeyboardButton>> rows;

    private InlineKeyboardFactory() {
        rows = new ArrayList<>();
    }

    public static InlineKeyboardFactory build() {
        return new InlineKeyboardFactory();
    }

    public static InlineKeyboardButton button(String text, String data) {
        return new InlineKeyboardButton(text).setCallbackData(data);
    }

    public InlineKeyboardFactory addRow(InlineKeyboardButton... buttons) {
        ArrayList<InlineKeyboardButton> buttonList = new ArrayList<>();
        for (int i = 0; i < buttons.length; i++) {
            buttonList.add(buttons[i]);
        }
        rows.add(buttonList);
        return this;
    }

    public InlineKeyboardMarkup toMarkup() {
        InlineKeyboardMarkup keyboard = new InlineKeyboardMarkup().setKeyboard(rows);
        return keyboard;
    }

    public InlineKeyboardFactory addRow(ArrayList<InlineKeyboardButton> buttons) {
        if (buttons == null) return this;
        if (buttons.size() <= 0) return this;
        rows.add(buttons);
        return this;
    }
}
