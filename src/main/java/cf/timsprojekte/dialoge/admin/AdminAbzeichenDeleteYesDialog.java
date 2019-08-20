package cf.timsprojekte.dialoge.admin;

import cf.timsprojekte.UniqueBot;
import cf.timsprojekte.dialoge.ButtonDialog;
import cf.timsprojekte.dialoge.CloseDialog;
import cf.timsprojekte.verwaltung.immutable.Abzeichen;
import org.telegram.telegrambots.meta.api.objects.Message;

public class AdminAbzeichenDeleteYesDialog extends ButtonDialog {
    private final Abzeichen abzeichen;

    protected AdminAbzeichenDeleteYesDialog(Abzeichen abzeichen) {
        super("yes", "Ja",abzeichen.getName() + " wurde entfernt.",new CloseDialog());
        this.abzeichen = abzeichen;
    }

    @Override
    public void showDialog(Message message) {
        UniqueBot.unique().abzeichenverwaltung.removeAbzeichen(abzeichen);
        super.showDialog(message);
    }
}
