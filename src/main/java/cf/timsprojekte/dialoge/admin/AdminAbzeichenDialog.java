package cf.timsprojekte.dialoge.admin;

import cf.timsprojekte.dialoge.BackDialog;
import cf.timsprojekte.dialoge.ButtonDialog;

public class AdminAbzeichenDialog extends ButtonDialog {
    public AdminAbzeichenDialog() {
        super("admabz", "Abzeichen", "TODO Abzeichen msg", new AdminAbzeichenCreateDialog(), new AdminAbzeichenDeleteDialog(), new BackDialog());
    }
}
