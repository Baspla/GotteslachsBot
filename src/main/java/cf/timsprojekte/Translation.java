package cf.timsprojekte;

public enum Translation {
    canceled("canceled"), ////
    code_invalid("code.invalid"), ////
    code_moreArgs("code.moreArgs"), ////
    code_nonexistent("code.nonexistent"), ////
    code_removed("code.removed"), /////
    code_set("code.set"), ////
    code_taken("code.taken"), ////
    error("error"), ////
    group_accepted("group.accepted"), ////
    group_declined("group.declined"), ////
    group_requested("group.requested"), ////
    levelup_multi("levelup.multi"), ////
    levelup_single("levelup.single"), ////
    reward_jackpot("reward.jackpot"), ////
    shopItem_language_reward("shopItem.language.reward"), ////
    shopItem_language_reward_double("shopItem.language.reward.double"), ////
    stats_bot("stats.bot"), ////
    system_saved("system.saved"), ////
    system_saving("system.saving"), ////
    system_shutdown("system.shutdown"), ////
    user_entry_points("user.entry.points"), ////
    user_entry_votes("user.entry.votes"), ////
    user_language("user.language"), ////
    user_language_button("user.language.button"), ////
    user_language_none("user.language.none"),
    user_language_set("user.language.set"), ////
    user_main("user.main"), ////
    user_name("user.name"), ////
    user_namechange("user.namechange"), ////
    user_namechange_button("user.namechange.button"), ////
    user_namechange_changed("user.name.changed"), ////
    user_namechange_unchanged("user.namechange.unchanged"), ////
    user_namevotes("user.namevotes"), ////
    user_shop("user.shop"), ////
    user_shop_bought("user.shop.bought"), ////
    user_shop_button("user.shop.button"), ////
    user_shop_buy("user.shop.buy"), ////
    user_shop_cancel("user.shop.cancel"), ////
    user_shop_canceled("user.shop.canceled"), ////
    user_shop_entry("user.shop.entry"), ////
    user_shop_item("user.shop.item"), ////
    user_shop_nomoney("user.shop.nomoney"), ////
    user_titlename("user.titlename"), ////
    user_titlenamepoints("user.titlenamepoints"), ////
    vote_down("vote.down"), ////
    vote_super("vote.super"), ////
    vote_up("vote.up"), ////
    system_started("system.started"); ////

    private final String key;

    Translation(String key) {
        this.key = key;
    }

    public String getKey() {
        return key;
    }
}
