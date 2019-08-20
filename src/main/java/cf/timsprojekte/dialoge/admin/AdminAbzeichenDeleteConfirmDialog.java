package cf.timsprojekte.dialoge.admin;

import cf.timsprojekte.dialoge.BackDialog;
import cf.timsprojekte.dialoge.ButtonDialog;
import cf.timsprojekte.verwaltung.immutable.Abzeichen;

public class AdminAbzeichenDeleteConfirmDialog extends ButtonDialog {
    public AdminAbzeichenDeleteConfirmDialog(Abzeichen abzeichen) {
        super("confirm", "Bestätigen", "Willst du " + abzeichen.getName() + " wirklich löschen?", new AdminAbzeichenDeleteYesDialog(abzeichen), new BackDialog());
    }
}
