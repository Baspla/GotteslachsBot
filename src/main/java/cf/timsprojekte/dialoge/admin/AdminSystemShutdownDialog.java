package cf.timsprojekte.dialoge.admin;

import cf.timsprojekte.dialoge.ActionDialog;
import org.telegram.telegrambots.meta.api.objects.Message;

public class AdminSystemShutdownDialog extends ActionDialog {
    protected AdminSystemShutdownDialog() {
        super("admsysdown","Shutdown");
    }

    @Override
    protected void action(Message message) {
        System.exit(0);
    }
}
