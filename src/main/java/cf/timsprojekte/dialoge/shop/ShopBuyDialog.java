package cf.timsprojekte.dialoge.shop;

import cf.timsprojekte.UniqueBot;
import cf.timsprojekte.dialoge.ActionDialog;
import cf.timsprojekte.verwaltung.immutable.Benutzer;
import cf.timsprojekte.verwaltung.immutable.StoreItem;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText;
import org.telegram.telegrambots.meta.api.objects.Message;

public class ShopBuyDialog extends ActionDialog {
    private StoreItem storeItem;

    public ShopBuyDialog(StoreItem storeItem) {
        super("buy" + storeItem.getId(), "Kaufen");
        this.storeItem = storeItem;
    }

    @Override
    protected void action(Message message) {
        String msg;
        Benutzer benutzer = UniqueBot.unique().nutzerverwaltung.getBenutzer(getUserId());
        if (storeItem.getLimit() == 0)
            msg = "Nichts mehr verfügbar.";
        else if (benutzer.getPunkte() >= storeItem.getValue()) {
            benutzer = UniqueBot.unique().nutzerverwaltung.removeBenutzerPunkte(benutzer, storeItem.getValue());
            if (storeItem.getLimit() > 0)
                storeItem = UniqueBot.unique().storeverwaltung.setStoreItemLimit(storeItem, storeItem.getLimit() - 1);
            UniqueBot.unique().ausgabe(benutzer.getNutzername() + " hat \"" + storeItem.getName() + "\" gekauft.");
            msg = "Du hast " + storeItem.getName() + " gekauft.";
        } else
            msg = "Nicht genügend Geld.";
        getSender().execute(new EditMessageText().setMessageId(message.getMessageId()).setChatId(message.getChatId()).setText(msg));
    }
}
