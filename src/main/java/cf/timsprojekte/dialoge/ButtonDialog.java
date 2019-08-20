package cf.timsprojekte.dialoge;

import cf.timsprojekte.DialogCallback;
import cf.timsprojekte.InlineKeyboardFactory;
import org.telegram.telegrambots.meta.api.methods.AnswerCallbackQuery;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;

import javax.validation.constraints.NotNull;
import java.util.ArrayList;

public abstract class ButtonDialog extends Dialog {

    protected Dialog[] dialoge;
    private final String message;
    private int page;

    public ButtonDialog(@NotNull String identifier, @NotNull String messageTitle, String message, Dialog... dialoge) {
        super(identifier, messageTitle);
        this.dialoge = dialoge;
        this.message = message;
        page = 0;
        for (int i = 0; i < dialoge.length; i++)
            addChild(dialoge[i]);
    }

    @Override
    protected void processCallback(DialogCallback callback) {
        if (callback.getData().isEmpty())
            showDialog(callback.getQuery().getMessage());
        else if (callback.getData().size() == 2 && callback.getData().get(0).equalsIgnoreCase("page")) {
            try {
                page = Integer.valueOf(callback.getData().get(1));
            } catch (NumberFormatException e) {
                page = 0;
            }
            showDialog(callback.getQuery().getMessage());
        }
        getSender().execute(new AnswerCallbackQuery().setCallbackQueryId(callback.getQuery().getId()));
    }

    @Override
    public void showDialog(Message message) {
        InlineKeyboardFactory factory = InlineKeyboardFactory.build();
        ArrayList<InlineKeyboardButton>[] rows = new ArrayList[4];
        for (int i = 0; i < 4; i++)
            rows[i] = new ArrayList<>();

        if (dialoge.length > 8) {
            //PAGE MODE
            for (int i = (page * 6); i < dialoge.length && i < (page * 6) + 6; i++) {
                rows[i % 3].add(InlineKeyboardFactory.button(dialoge[i].getMessageTitle(), dialoge[i].getIdentifierChain()));
            }
            if (page != 0) {
                rows[3].add(InlineKeyboardFactory.button("<<", getIdentifierChain() + "#page#" + (page - 1)));
            }
            if (((dialoge.length - (dialoge.length % 6) / 6)) > page) {
                rows[3].add(InlineKeyboardFactory.button(">>", getIdentifierChain() + "#page#" + (page + 1)));
            }
        } else {
            //SINGLE MODE
            for (int i = 0; i < dialoge.length; i++) {
                rows[i % 4].add(InlineKeyboardFactory.button(dialoge[i].getMessageTitle(), dialoge[i].getIdentifierChain()));
            }
        }
        for (int i = 0; i < 4; i++)
            if (!rows[i].isEmpty()) factory.addRow(rows[i]);
        InlineKeyboardMarkup keyboard = factory.toMarkup();
        if (message == null) {
            SendMessage sm = new SendMessage().setText(this.message).setChatId(getChatId());
            if (dialoge.length > 0)
                sm = sm.setReplyMarkup(keyboard);
            getSender().execute(sm);
        } else {
            EditMessageText emt = new EditMessageText().setText(this.message).setChatId(getChatId()).setMessageId(message.getMessageId());
            if (dialoge.length > 0)
                emt = emt.setReplyMarkup(keyboard);
            getSender().execute(emt);
        }
    }
}
