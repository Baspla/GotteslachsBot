package cf.timsprojekte.dialoge.admin;

import cf.timsprojekte.dialoge.BackDialog;
import cf.timsprojekte.dialoge.ButtonDialog;

public class AdminSystemDialog extends ButtonDialog {
    public AdminSystemDialog() {
        super("admsys", "System", "Systemsteuerung",new AdminSystemShutdownDialog(), new BackDialog());
    }
}
