package cf.timsprojekte.dialoge.admin;

import cf.timsprojekte.DialogCallback;
import cf.timsprojekte.InlineKeyboardFactory;
import cf.timsprojekte.dialoge.BackDialog;
import cf.timsprojekte.dialoge.Dialog;
import cf.timsprojekte.verwaltung.Abzeichenverwaltung;
import cf.timsprojekte.verwaltung.immutable.Abzeichen;
import cf.timsprojekte.UniqueBot;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;

import java.util.ArrayList;
import java.util.List;

public class AdminAbzeichenDeleteDialog extends Dialog {
    private final BackDialog back;
    private Integer page;

    protected AdminAbzeichenDeleteDialog() {
        super("abzdel", "Löschen");
        back = new BackDialog();
        addChild(back);
    }


    @SuppressWarnings("Duplicates")
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
        } else if (callback.getData().size() == 2 && callback.getData().get(0).equalsIgnoreCase("abz")) {
            try {
                Abzeichenverwaltung a = UniqueBot.unique().abzeichenverwaltung;
                if (a == null) return;
                int id = Integer.parseInt(callback.getData().get(1));
                Abzeichen abzeichen = a.getAbzeichen(id);
                if (abzeichen != null) {
                    AdminAbzeichenDeleteConfirmDialog dialog = new AdminAbzeichenDeleteConfirmDialog(abzeichen);
                    addChild(dialog);
                    dialog.showDialog(callback.getQuery().getMessage());
                }
            } catch (NumberFormatException ignored) {
            }
        }
    }

    @SuppressWarnings("Duplicates")
    @Override
    public void showDialog(Message message) {
        List<Abzeichen> abzeichen = UniqueBot.unique().abzeichenverwaltung.getAbzeichenList();
        InlineKeyboardFactory factory = InlineKeyboardFactory.build();
        ArrayList<InlineKeyboardButton>[] rows = new ArrayList[4];
        for (int i = 0; i < 4; i++)
            rows[i] = new ArrayList<>();

        if (abzeichen.size() > 7) {
            //PAGE MODE
            for (int i = (page * 6); i < abzeichen.size() && i < (page * 6) + 6; i++) {
                rows[i % 3].add(InlineKeyboardFactory.button(abzeichen.get(i).getName(), getIdentifierChain() + "#abz#" + abzeichen.get(i).getAbzeichenId()));
            }
            if (page != 0) {
                rows[3].add(InlineKeyboardFactory.button("<<", getIdentifierChain() + "#page#" + (page - 1)));
            }
            rows[3].add(InlineKeyboardFactory.button(back.getMessageTitle(), back.getIdentifierChain()));
            if (((abzeichen.size() - (abzeichen.size() % 6)) / 6) > page) {
                rows[3].add(InlineKeyboardFactory.button(">>", getIdentifierChain() + "#page#" + (page + 1)));
            }
        } else {
            //SINGLE MODE
            for (int i = 0; i < abzeichen.size(); i++) {
                rows[i % 4].add(InlineKeyboardFactory.button(abzeichen.get(i).getName(), getIdentifierChain() + "#abz#" + abzeichen.get(i).getAbzeichenId()));
            }
            rows[3].add(InlineKeyboardFactory.button(back.getMessageTitle(), back.getIdentifierChain()));
        }
        for (int i = 0; i < 4; i++)
            if (!rows[i].isEmpty()) factory.addRow(rows[i]);
        InlineKeyboardMarkup keyboard = factory.toMarkup();
        String txt = "Welches Abzeichen soll entfernt werden?";
        if (message == null) {
            SendMessage sm = new SendMessage().setText(txt).setChatId(getChatId());
            sm = sm.setReplyMarkup(keyboard);
            getSender().execute(sm);
        } else {
            EditMessageText emt = new EditMessageText().setText(txt).setChatId(getChatId()).setMessageId(message.getMessageId());
            emt = emt.setReplyMarkup(keyboard);
            getSender().execute(emt);
        }
    }
}
