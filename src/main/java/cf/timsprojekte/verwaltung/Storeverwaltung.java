package cf.timsprojekte.verwaltung;

import cf.timsprojekte.verwaltung.immutable.Abzeichen;
import cf.timsprojekte.verwaltung.immutable.StoreItem;
import org.telegram.abilitybots.api.db.DBContext;

import javax.validation.constraints.NotNull;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.Random;
import java.util.Set;

public class Storeverwaltung {
    private final Set<StoreItem> storeItemSet;

    public Storeverwaltung(DBContext db) {
        storeItemSet = db.getSet("StoreItems");
    }


    public StoreItem getStoreItem(long storeItemId) {
        Optional<StoreItem> storeItem = storeItemSet.stream().filter(b -> b.getId() == storeItemId).findAny();
        return storeItem.orElse(null);
    }

    public void createStoreItem(int value, String name, String desc, int limit) {
        Random rnd = new Random(System.currentTimeMillis());
        int rndInt;
        do {
            rndInt = Math.abs(rnd.nextInt());
        } while (isStoreItemIdUsed(rndInt));
        StoreItem storeItem = new StoreItem(rndInt, value, name, desc, limit);
        storeItemSet.add(storeItem);
    }

    public void removeStoreItem(StoreItem storeItem) {
        storeItemSet.remove(storeItem);
    }

    private boolean isStoreItemIdUsed(int rndInt) {
        return storeItemSet.stream().anyMatch(abzeichen -> abzeichen.getId() == rndInt);
    }

    public StoreItem setStoreItemValue(@NotNull StoreItem abzeichen, int value) {
        return replace(abzeichen, new StoreItem(abzeichen.getId(), value, abzeichen.getName(), abzeichen.getDesc(), abzeichen.getLimit()));
    }

    public StoreItem setStoreItemName(@NotNull StoreItem abzeichen, String name) {
        return replace(abzeichen, new StoreItem(abzeichen.getId(), abzeichen.getValue(), name, abzeichen.getDesc(), abzeichen.getLimit()));
    }

    public StoreItem setStoreItemDesc(@NotNull StoreItem abzeichen, String desc) {
        return replace(abzeichen, new StoreItem(abzeichen.getId(), abzeichen.getValue(), abzeichen.getName(), desc, abzeichen.getLimit()));
    }

    public StoreItem setStoreItemLimit(@NotNull StoreItem abzeichen, int limit) {
        return replace(abzeichen, new StoreItem(abzeichen.getId(), abzeichen.getValue(), abzeichen.getName(), abzeichen.getDesc(), limit));
    }

    public StoreItem replace(@NotNull StoreItem vorher, @NotNull StoreItem nachher) {
        if (storeItemSet.remove(vorher)) {
            if (storeItemSet.add(nachher)) {
                return nachher;
            }
            storeItemSet.add(vorher);
        }
        return vorher;
    }

    public List<StoreItem> getStoreItemList() {
        ArrayList<StoreItem> list = new ArrayList<>(storeItemSet);
        list.removeIf(storeItem -> storeItem.getLimit() == 0);
        list.sort(Comparator.comparingInt(StoreItem::getId));
        return list;
    }
}
