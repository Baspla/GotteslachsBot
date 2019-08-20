package cf.timsprojekte;

import org.telegram.telegrambots.meta.api.objects.CallbackQuery;

import java.util.ArrayList;
import java.util.Arrays;

public class DialogCallback {
    private String trace;
    private int depth;
    private CallbackQuery query;
    private ArrayList<String> dialogs;
    private ArrayList<String> data;

    public DialogCallback(CallbackQuery query) {
        this.query = query;
        this.depth = 0;
        this.trace = "";
        data = new ArrayList<>();
        dialogs = new ArrayList<>();
        String s = query.getData();
        String[] dataParts = s.split("#");
        if (dataParts.length == 0)
            return;
        if (dataParts.length > 1) {
            data.addAll(Arrays.asList(dataParts).subList(1, dataParts.length));
        }
        String[] dialogParts = dataParts[0].split("_");
        if (dialogParts.length == 0)
            return;
        dialogs.addAll(Arrays.asList(dialogParts));
    }

    public void incrementDepth() {
        depth++;
    }

    public void setDepth(int depth) {
        this.depth = depth;
    }

    public int getDepth() {
        return depth;
    }

    public CallbackQuery getQuery() {
        return query;
    }

    public ArrayList<String> getDialogs() {
        return dialogs;
    }

    public ArrayList<String> getData() {
        return data;
    }

    public String getTrace() {
        return trace;
    }

    public void addTrace(String s) {
        trace = trace + "->" + s;
    }

    @Override
    public String toString() {
        return "DialogCallback{" +
                "trace='" + trace + '\'' +
                ", depth=" + depth +
                ", query=" + query +
                ", dialogs=" + dialogs +
                ", data=" + data +
                '}';
    }
}
