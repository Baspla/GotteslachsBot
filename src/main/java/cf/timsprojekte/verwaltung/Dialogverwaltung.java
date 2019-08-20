package cf.timsprojekte.verwaltung;

import cf.timsprojekte.Bot;
import cf.timsprojekte.DialogCallback;
import cf.timsprojekte.dialoge.Dialog;
import cf.timsprojekte.dialoge.RootDialog;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import org.telegram.telegrambots.meta.api.objects.Message;

import java.util.HashSet;
import java.util.Optional;

public class Dialogverwaltung {
    private HashSet<RootDialog> dialoge;
    private Bot bot;

    public Dialogverwaltung(Bot bot) {
        this.bot = bot;
        dialoge = new HashSet<>();
    }

    public void begin(Dialog dialog, Integer userId, Long chatId) {
        RootDialog root = getRootDialog(chatId, userId);
        root.destroy(true);
        root.addChild(dialog);
        dialog.showDialog(null);
    }

    private RootDialog getRootDialog(Long chatId, Integer userId) {
        Optional<RootDialog> opt = dialoge.stream().filter(dialog -> dialog.getChatId() == chatId && dialog.getUserId() == userId).findAny();
        if (!opt.isPresent()) {
            RootDialog root = new RootDialog(bot, chatId, userId);
            dialoge.add(root);
            return root;
        } else {
            return opt.get();
        }
    }

    public void onCallback(CallbackQuery query) {
        RootDialog root = getRootDialog(query.getMessage().getChatId(), query.getFrom().getId());
        root.recieveCallback(new DialogCallback(query));
    }

    public void reply(Message message) {
        getRootDialog(message.getChatId(), message.getFrom().getId()).reply(message);
    }
}
