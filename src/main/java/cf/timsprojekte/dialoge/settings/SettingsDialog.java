package cf.timsprojekte.dialoge.settings;

import cf.timsprojekte.dialoge.ButtonDialog;

public class SettingsDialog extends ButtonDialog {
    public SettingsDialog() {
        super("settings", "Einstellungen", "Einstellungen", new SettingsNameDialog());
    }
}
