package cf.timsprojekte.db;

import me.xdrop.fuzzywuzzy.FuzzySearch;
import org.telegram.telegrambots.meta.api.objects.inlinequery.inputmessagecontent.InputTextMessageContent;
import org.telegram.telegrambots.meta.api.objects.inlinequery.result.InlineQueryResult;
import org.telegram.telegrambots.meta.api.objects.inlinequery.result.InlineQueryResultArticle;

import java.util.*;

public class CodeManager {

    private static final int PAGE_SIZE = 5;
    private final Map<String, String> db;

    public CodeManager(Map<String, String> db) {
        this.db = db;
    }

    public List<InlineQueryResult> match(String text, int page) {

        List<InlineQueryResult> results = new ArrayList<>();
        Map.Entry[] entrys = new Map.Entry[db.size()];
        db.entrySet().toArray(entrys);
        for (int i = 0; i < entrys.length; i++) {
            int keyRatio = FuzzySearch.partialRatio(entrys[i].getKey().toString().toLowerCase(), text.toLowerCase());
            int valueRatio = FuzzySearch.partialRatio(entrys[i].getValue().toString().toLowerCase(), text.toLowerCase());
            if (90 < keyRatio || 80 < valueRatio)
                results.add(new InlineQueryResultArticle()
                        .setTitle((String) entrys[i].getKey())
                        .setDescription((String) entrys[i].getValue())
                        .setInputMessageContent(
                                new InputTextMessageContent().setMessageText("<b>" + (String) entrys[i].getKey() + "</b>\n" + (String) entrys[i].getValue()).setParseMode("HTML")
                        ).setId(String.valueOf((entrys[i].getKey() + (String) entrys[i].getValue()).hashCode())));
        }
        List<InlineQueryResult> results2 = new ArrayList<>();
        for (int i = page * PAGE_SIZE; i < results.size() && i < (page + 1) * PAGE_SIZE; i++) {
            results2.add(results.get(i));
        }
        return results2;
    }

    public void add(String key, String value) {
        db.put(key, value);
    }

    public boolean contains(String key) {
        return db.containsKey(key);
    }

    public void remove(String key) {
        db.remove(key);
    }

    public String list() {
        StringBuilder s = new StringBuilder();
        db.forEach((s1, s2) -> {
            s.append(", ").append(s1);
        });
        return s.toString().substring(2);
    }
}
