package cf.timsprojekte.dialoge;

import cf.timsprojekte.Bot;
import cf.timsprojekte.DialogCallback;
import cf.timsprojekte.ReplyListener;
import org.telegram.abilitybots.api.sender.SilentSender;
import org.telegram.telegrambots.meta.api.objects.Message;

import javax.validation.constraints.NotNull;
import java.util.ArrayList;

public class RootDialog extends Dialog {
    private final long chatId;
    private final int userId;
    private final ArrayList<ReplyListener> listeners;
    private final Bot bot;

    public RootDialog(@NotNull Bot bot, @NotNull long chatId, @NotNull int userId) {
        super("root", "Root");
        this.bot = bot;
        this.userId = userId;
        this.chatId = chatId;
        listeners = new ArrayList<>();
    }

    public void registerReplyListener(ReplyListener listener) {
        listeners.add(listener);
    }

    public void unregisterReplyListener(ReplyListener listener) {
        listeners.remove(listener);
    }

    public SilentSender getSender() {
        return bot.silentPublic;
    }

    @Override
    public long getChatId() {
        return chatId;
    }

    @Override
    public int getUserId() {
        return userId;
    }

    @Override
    public void setParent(Dialog parent) {
    }

    @Override
    protected void processCallback(DialogCallback callback) {

    }

    @Override
    public String getIdChnIntern() {
        return "";
    }

    @Override
    public void showDialog(Message message) {

    }

    @Override
    public void destroy(boolean fromParent) {
        children.forEach(dialog -> dialog.destroy(true));
        children.clear();
    }

    public void reply(Message message) {
        System.out.println("ROOT");
        @SuppressWarnings("unchecked") ArrayList<ReplyListener> remove = new ArrayList();
        listeners.stream().filter(replyListener -> {
            return replyListener.listeningForMessageId() == message.getReplyToMessage().getMessageId();
        }).forEach(remove::add);
        for (ReplyListener r : remove) {
            r.onReply(message);
            unregisterReplyListener(r);
        }
    }
}
