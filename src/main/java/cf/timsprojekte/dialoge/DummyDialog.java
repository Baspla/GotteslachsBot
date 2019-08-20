package cf.timsprojekte.dialoge;

import cf.timsprojekte.DialogCallback;
import org.telegram.telegrambots.meta.api.methods.AnswerCallbackQuery;
import org.telegram.telegrambots.meta.api.objects.Message;

public class DummyDialog extends Dialog {
    public DummyDialog(String msg) {
        super("dummy" + msg.toLowerCase(), msg);
    }

    @Override
    protected void processCallback(DialogCallback callback) {
        getSender().execute(new AnswerCallbackQuery().setCallbackQueryId(callback.getQuery().getId()));
    }

    @Override
    public void showDialog(Message message) {

    }
}
