package cf.timsprojekte;

import cf.timsprojekte.db.Nutzer;
import kotlin.Pair;

import java.util.HashMap;
import java.util.Random;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

public enum ShopItem {
    LanguageBox("language", "lng", 500, (data) -> {
        Pair<Integer, HashMap<Integer, Language>> pair = Language.getLootMap();
        HashMap<Integer, Language> lootMap = pair.component2();
        Integer total = pair.component1();
        int randomInt = new Random(System.currentTimeMillis()).nextInt(total);
        AtomicBoolean set = new AtomicBoolean(false);
        AtomicInteger result = new AtomicInteger(-1);
        lootMap.keySet().stream().sorted(Integer::compareTo).forEachOrdered(integer -> {
            if (randomInt < integer) set.set(true);
            if (!set.get()) result.set(integer);
        });
        if(result.get()==-1){
            data.getAusgabe().send(data.chatId, data.getNutzer().getLocale(), "error", "Kein Result?!");
        }
        Language lang = lootMap.get(result.get());
        if (data.getNutzer().getLanguages().contains(lang)) {
            data.getAusgabe().send(data.chatId, data.getNutzer().getLocale(), "shopItem.language.reward.double", lang.title());
        } else {
            data.getNutzer().addLanguage(lang);
            data.getAusgabe().send(data.chatId, data.getNutzer().getLocale(), "shopItem.language.reward", lang.title());
        }
    });

    private final Consumer<BuyData> onBuy;
    private final String data;
    private final String title;
    private final int price;

    ShopItem(String title, String data, int price, Consumer<BuyData> onBuy) {
        this.data = "shop_" + data;
        this.title = "shopItem." + title;
        this.price = price;
        this.onBuy = onBuy;
    }

    public String data() {
        return data;
    }

    public String title() {
        return title;
    }

    public void onBuy(Nutzer nutzer, Ausgabe ausgabe, Long chatId) {
        onBuy.accept(new BuyData(nutzer, ausgabe, chatId));
    }

    public int price() {
        return price;
    }

    private class BuyData {
        private final Nutzer nutzer;
        private final Ausgabe ausgabe;
        private final Long chatId;

        public BuyData(Nutzer nutzer, Ausgabe ausgabe, Long chatId) {
            this.nutzer = nutzer;
            this.ausgabe = ausgabe;
            this.chatId = chatId;
        }

        public Nutzer getNutzer() {
            return nutzer;
        }

        public Ausgabe getAusgabe() {
            return ausgabe;
        }

        public Long getChatId() {
            return chatId;
        }
    }
}
