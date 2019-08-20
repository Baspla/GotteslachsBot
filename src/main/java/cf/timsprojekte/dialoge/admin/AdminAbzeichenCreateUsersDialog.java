package cf.timsprojekte.dialoge.admin;

import cf.timsprojekte.DialogCallback;
import cf.timsprojekte.InlineKeyboardFactory;
import cf.timsprojekte.dialoge.Dialog;
import cf.timsprojekte.verwaltung.Nutzerverwaltung;
import cf.timsprojekte.verwaltung.immutable.Abzeichen;
import cf.timsprojekte.verwaltung.immutable.Benutzer;
import cf.timsprojekte.UniqueBot;
import org.telegram.telegrambots.meta.api.methods.AnswerCallbackQuery;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;

import java.util.ArrayList;
import java.util.List;

public class AdminAbzeichenCreateUsersDialog extends Dialog {
    private final String titel;
    private final int reward;
    private final String description;
    private Integer page;
    private ArrayList<Benutzer> selected;

    public AdminAbzeichenCreateUsersDialog(String titel, String description, int reward) {
        super("abzusr", "Nutzer");
        this.titel = titel;
        this.reward = reward;
        this.description = description;
        page = 0;
        selected = new ArrayList<>();
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
        } else if (callback.getData().size() == 2 && callback.getData().get(0).equalsIgnoreCase("user")) {
            try {
                Nutzerverwaltung n = UniqueBot.unique().nutzerverwaltung;
                if (n == null) return;
                int id = Integer.parseInt(callback.getData().get(1));
                Benutzer benutzer = n.getBenutzer(id);
                if (benutzer != null) {
                    if (selected.contains(benutzer)) {
                        selected.remove(benutzer);
                    } else {
                        selected.add(benutzer);
                    }
                    showDialog(callback.getQuery().getMessage());
                }
            } catch (NumberFormatException ignored) {
            }
        } else if (callback.getData().size() == 1 && callback.getData().get(0).equalsIgnoreCase("done")) {
            //DONE
            if (selected.isEmpty()) {
                getSender().execute(new AnswerCallbackQuery().setCallbackQueryId(callback.getQuery().getId()).setText("Wähle mindestens eine Person aus"));
                return;
            }
            Abzeichen abzeichen = UniqueBot.unique().abzeichenverwaltung.createAbzeichen(reward, titel, description);
            Nutzerverwaltung n = UniqueBot.unique().nutzerverwaltung;
            selected.forEach(benutzer -> n.addBenutzerAbzeichen(n.getBenutzer(benutzer.getUserId()), abzeichen));
            UniqueBot.unique().abzeichenVerliehen(abzeichen, selected);
            getSender().execute(new EditMessageText().setChatId(callback.getQuery().getMessage().getChatId()).setMessageId(callback.getQuery().getMessage().getMessageId())
                    .setText("Abzeichen erstellt.\n" + titel + "\n" + reward + " Punkte\n" + description)
                    .setReplyMarkup(null));
        }
    }

    @SuppressWarnings("Duplicates")
    @Override
    public void showDialog(Message message) {
        List<Benutzer> benutzer = UniqueBot.unique().nutzerverwaltung.getBenuzerList();
        InlineKeyboardFactory factory = InlineKeyboardFactory.build();
        ArrayList<InlineKeyboardButton>[] rows = new ArrayList[4];
        for (int i = 0; i < 4; i++)
            rows[i] = new ArrayList<>();

        if (benutzer.size() > 7) {
            //PAGE MODE
            for (int i = (page * 6); i < benutzer.size() && i < (page * 6) + 6; i++) {
                rows[i % 3].add(InlineKeyboardFactory.button(((selected.contains(benutzer.get(i))) ? "\u2714 " : "\u274C ") + benutzer.get(i).getNutzername(), getIdentifierChain() + "#user#" + benutzer.get(i).getUserId()));
            }
            if (page != 0) {
                rows[3].add(InlineKeyboardFactory.button("<<", getIdentifierChain() + "#page#" + (page - 1)));
            }
            rows[3].add(InlineKeyboardFactory.button("Fertig", getIdentifierChain() + "#done"));
            if (((benutzer.size() - (benutzer.size() % 6)) / 6) > page) {
                rows[3].add(InlineKeyboardFactory.button(">>", getIdentifierChain() + "#page#" + (page + 1)));
            }
        } else {
            //SINGLE MODE
            for (int i = 0; i < benutzer.size(); i++) {
                rows[i % 4].add(InlineKeyboardFactory.button(((selected.contains(benutzer.get(i))) ? "\u2714 " : "\u274C ") + benutzer.get(i).getNutzername(), getIdentifierChain() + "#user#" + benutzer.get(i).getUserId()));
            }
            rows[3].add(InlineKeyboardFactory.button("Fertig", getIdentifierChain() + "#done"));
        }
        for (int i = 0; i < 4; i++)
            if (!rows[i].isEmpty()) factory.addRow(rows[i]);
        InlineKeyboardMarkup keyboard = factory.toMarkup();
        String txt = "Wähle aus welche Nutzer das Abzeichen erhalten";
        if (message == null) {
            SendMessage sm = new SendMessage().setText(txt).setChatId(getChatId());
            if (benutzer.size() > 0)
                sm = sm.setReplyMarkup(keyboard);
            getSender().execute(sm);
        } else {
            EditMessageText emt = new EditMessageText().setText(txt).setChatId(getChatId()).setMessageId(message.getMessageId());
            if (benutzer.size() > 0)
                emt = emt.setReplyMarkup(keyboard);
            getSender().execute(emt);
        }
    }
}
