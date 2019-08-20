package cf.timsprojekte;

import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardRow;

import java.util.ArrayList;

public class ReplyKeyboardFactory {

    public ReplyKeyboardMarkup toMarkup() {
        ReplyKeyboardMarkup keyboard = new ReplyKeyboardMarkup().setKeyboard(rows);
        return keyboard;
    }

    private ArrayList<KeyboardRow> rows;

    public ReplyKeyboardFactory() {
        rows = new ArrayList<>();
    }

    public ReplyKeyboardFactory addRow(KeyboardButton... buttons) {
        KeyboardRow row = new KeyboardRow();
        for (int i = 0; i < buttons.length; i++) {
            row.add(buttons[i]);
        }
        rows.add(row);
        return this;
    }
}
