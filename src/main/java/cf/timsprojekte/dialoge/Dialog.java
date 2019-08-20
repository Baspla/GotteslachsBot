package cf.timsprojekte.dialoge;

import cf.timsprojekte.DialogCallback;
import cf.timsprojekte.ReplyListener;
import org.telegram.abilitybots.api.sender.SilentSender;
import org.telegram.telegrambots.meta.api.objects.Message;

import javax.validation.constraints.NotNull;
import java.util.ArrayList;
import java.util.Optional;

public abstract class Dialog {

    private final String identifier;
    private final String messageTitle;
    private Dialog parent;
    final ArrayList<Dialog> children;


    protected Dialog( @NotNull String identifier, @NotNull String messageTitle) {
        this.identifier = identifier;
        this.messageTitle = messageTitle;
        children = new ArrayList<>();
    }

    public Dialog getParent() {
        return parent;
    }

    public void setParent(Dialog parent) {
        this.parent = parent;
    }

    public String getIdentifier() {
        return identifier;
    }

    public String getMessageTitle() {
        return messageTitle;
    }

    public void recieveCallback(DialogCallback callback) {
        if (callback.getDialogs().size() < callback.getDepth()) {
            System.err.println("Ungültige Dialog tiefe");
        } else if (callback.getDialogs().size() == callback.getDepth()) {
            processCallback(callback);
        } else {
            String nextDialog = callback.getDialogs().get(callback.getDepth());
            Optional<Dialog> optDialog = children.stream().filter(dia -> dia.getIdentifier().equalsIgnoreCase(nextDialog)).findAny();
            if (optDialog.isPresent()) {
                callback.incrementDepth();
                callback.addTrace(messageTitle);
                optDialog.get().recieveCallback(callback);
            } else {
                System.err.println("Dialog Kind nicht gefunden");
            }
        }
    }

    public void registerReplyListener(ReplyListener listener) {
        if (parent != null)
            parent.registerReplyListener(listener);
    }

    public void unregisterReplyListener(ReplyListener listener) {
        if (parent != null)
            parent.unregisterReplyListener(listener);
    }

    public SilentSender getSender() {
        if (parent != null)
            return parent.getSender();
        return null;
    }

    public long getChatId() {
        if (parent != null)
            return parent.getChatId();
        return 0;
    }

    public int getUserId() {
        if (parent != null)
            return parent.getUserId();
        return 0;
    }


    public void addChild(Dialog dialog) {
        if (dialog.getParent() != null) return;
        children.add(dialog);
        dialog.setParent(this);
    }

    public void removeChild(Dialog dialog) {
        children.remove(dialog);
        dialog.setParent(null);
    }

    public void destroy(boolean fromParent) {
        if (!fromParent) {
            parent.destroy(false);
        }else{
            children.forEach(dialog -> dialog.destroy(true));
            children.clear();
        }
    }

    protected abstract void processCallback(DialogCallback callback);

    public String getIdentifierChain() {
        String s = getIdChnIntern();
        return s.substring(0, s.length() - 1);
    }

    public String getIdChnIntern() {
        return parent.getIdChnIntern() + identifier + "_";
    }

    public abstract void showDialog(Message message);
}
