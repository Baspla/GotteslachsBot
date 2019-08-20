package cf.timsprojekte.dialoge.admin;

import cf.timsprojekte.dialoge.ButtonDialog;
import cf.timsprojekte.dialoge.CloseDialog;

public class AdminMainDialog extends ButtonDialog {
    public AdminMainDialog() {
        super("admain","Admin","Admin main Message",new AdminNutzerDialog(),new AdminShopDialog(),new AdminStatsDialog(),new CloseDialog(),new AdminAbzeichenDialog(),new AdminSystemDialog());
    }
}
