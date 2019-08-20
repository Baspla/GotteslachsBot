package cf.timsprojekte.dialoge.shop;

import cf.timsprojekte.UniqueBot;
import cf.timsprojekte.dialoge.BackDialog;
import cf.timsprojekte.dialoge.ButtonDialog;
import cf.timsprojekte.dialoge.Dialog;
import cf.timsprojekte.verwaltung.immutable.StoreItem;

import java.util.List;

public class ShopDialog extends ButtonDialog {

    public ShopDialog() {
        super("shop", "Shop", "TODO Shop", new BackDialog());
        List<StoreItem> list = UniqueBot.unique().storeverwaltung.getStoreItemList();
        Dialog[] newArray = new Dialog[dialoge.length + list.size()];
        for (int i = 0; i < dialoge.length; i++) {
            newArray[i] = dialoge[i];
        }
        for (int i = dialoge.length; i < dialoge.length + list.size(); i++) {
            newArray[i] = new ShopItemDialog(list.get(i-dialoge.length ));
            addChild(newArray[i]);
        }
        dialoge = newArray;
    }
}
