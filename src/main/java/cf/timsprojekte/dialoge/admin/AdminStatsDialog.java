package cf.timsprojekte.dialoge.admin;

import cf.timsprojekte.dialoge.BackDialog;
import cf.timsprojekte.dialoge.ButtonDialog;
import cf.timsprojekte.dialoge.DummyDialog;

public class AdminStatsDialog  extends ButtonDialog {
    public AdminStatsDialog() {
        super("admstats","Stats","Statistiken",new DummyDialog("TODO"),new BackDialog());
    }
}
