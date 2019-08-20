package cf.timsprojekte.dialoge.admin;

import cf.timsprojekte.UniqueBot;
import cf.timsprojekte.dialoge.ActionDialog;
import cf.timsprojekte.dialoge.Dialog;
import org.telegram.telegrambots.meta.api.objects.Message;

public class AdminShopKevinDialog extends ActionDialog {
    public AdminShopKevinDialog() {
        super("admkvshop","Kevin");
    }

    @Override
    protected void action(Message message) {
        UniqueBot.unique().storeverwaltung.createStoreItem(600,"Preview Version von \"Bobs Melancholy\"","Spiele schon jetzt die Preview zu \"Bobs Melancholy\"!",3);
    }
}

