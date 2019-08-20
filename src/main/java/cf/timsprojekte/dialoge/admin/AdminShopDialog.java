package cf.timsprojekte.dialoge.admin;

import cf.timsprojekte.dialoge.BackDialog;
import cf.timsprojekte.dialoge.ButtonDialog;

public class AdminShopDialog extends ButtonDialog {
    public AdminShopDialog() {
        super("admshop","Shop","Shop TODO",new AdminShopKevinDialog(),new BackDialog());
    }
}
