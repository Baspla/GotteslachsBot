package cf.timsprojekte.dialoge.shop;

import cf.timsprojekte.DialogCallback;
import cf.timsprojekte.InlineKeyboardFactory;
import cf.timsprojekte.dialoge.BackDialog;
import cf.timsprojekte.dialoge.Dialog;
import cf.timsprojekte.verwaltung.immutable.StoreItem;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;

import java.util.ArrayList;

public class ShopItemDialog extends Dialog {
    private final StoreItem storeItem;
    private final ShopBuyDialog buy;
    private final BackDialog back;
    private boolean disabled = false;
    private String message;

    public ShopItemDialog(StoreItem storeItem) {
        super("item" + storeItem.getId(), storeItem.getValue() + " - " + storeItem.getName());
        this.storeItem = storeItem;
        message = "Willst du " + storeItem.getName() + " für " + storeItem.getValue() + " kaufen?\n\n" + storeItem.getDesc() + ((storeItem.getLimit() < 0) ? "" : "\n\nNoch verfügbar: " + storeItem.getLimit());
        buy = new ShopBuyDialog(storeItem);
        back = new BackDialog();
        addChild(buy);
        addChild(back);
        if (storeItem.getLimit() == 0)
            disabled = true;
    }

    @Override
    protected void processCallback(DialogCallback callback) {
        if (callback.getData().isEmpty())
            showDialog(callback.getQuery().getMessage());
    }

    @Override
    public void showDialog(Message message) {
        InlineKeyboardFactory factory = InlineKeyboardFactory.build();

        factory.addRow(InlineKeyboardFactory.button("Kaufen", buy.getIdentifierChain()));
        factory.addRow(InlineKeyboardFactory.button(back.getMessageTitle(), back.getIdentifierChain()));

        InlineKeyboardMarkup keyboard = factory.toMarkup();
        if (message == null) {
            SendMessage sm = new SendMessage().setText(this.message).setChatId(getChatId());
            if (!disabled)
                sm = sm.setReplyMarkup(keyboard);
            getSender().execute(sm);
        } else {
            EditMessageText emt = new EditMessageText().setText(this.message).setChatId(getChatId()).setMessageId(message.getMessageId());
            if (!disabled)
                emt = emt.setReplyMarkup(keyboard);
            getSender().execute(emt);
        }
    }
}
