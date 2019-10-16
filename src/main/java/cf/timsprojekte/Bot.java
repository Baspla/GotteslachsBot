package cf.timsprojekte;

import cf.timsprojekte.db.Nutzer;
import cf.timsprojekte.db.NutzerManager;
import kotlin.Pair;
import org.apache.log4j.*;
import org.mapdb.DBMaker;
import org.springframework.stereotype.Component;
import org.telegram.abilitybots.api.bot.AbilityBot;
import org.telegram.abilitybots.api.db.MapDBContext;
import org.telegram.abilitybots.api.objects.*;
import org.telegram.abilitybots.api.util.AbilityUtils;
import org.telegram.telegrambots.meta.TelegramBotsApi;
import org.telegram.telegrambots.meta.api.methods.groupadministration.LeaveChat;
import org.telegram.telegrambots.meta.api.objects.*;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.*;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.meta.exceptions.TelegramApiRequestException;

import javax.annotation.PostConstruct;
import java.io.IOException;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.*;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import static org.telegram.abilitybots.api.objects.Locality.ALL;
import static org.telegram.abilitybots.api.objects.Locality.USER;
import static org.telegram.abilitybots.api.objects.Privacy.ADMIN;
import static org.telegram.abilitybots.api.objects.Privacy.PUBLIC;

@Component
public class Bot extends AbilityBot {

    private static final int JACKPOT_CHANCE = 1000;
    private static final int SUPER_HONOR_MAX = 5;
    private static final int NACHRICHT_PUNKTE_MIN = 1;
    private static final int NACHRICHT_PUNKTE_MAX = 12;
    private static final int NACHRICHT_COOLDOWN_MIN = 1;
    private static final int NACHRICHT_COOLDOWN_MAX = 4;
    private static final int JACKPOT_MULTIPLIER = 10;
    private static final boolean ANNOUNCE_STARTUP = false;
    private final Logger logger;
    private List<Long> listGroups;
    private ArrayList<Long> uncheckedGroups;
    private Random random;
    private NutzerManager nutzermanager;
    private Ausgabe ausgabe;

    public int creatorId() {
        return 67025299;
    }

    @SuppressWarnings("unused")
    public Bot() {
        super(System.getenv("gotteslachsbot_token"), System.getenv("gotteslachsbot_name"),
                new MapDBContext(DBMaker.fileDB(System.getenv("gotteslachsbot_name")).fileMmapEnableIfSupported().transactionEnable().make()));

        logger = LogManager.getLogger(Bot.class);
        FileAppender fileAppender = new FileAppender();
        fileAppender.setName("FileLogger");
        fileAppender.setFile("gotteslachs.tglog");
        String PATTERN = "%d | %-5p | %c{1} | %m%n";
        fileAppender.setLayout(new PatternLayout(PATTERN));
        fileAppender.setThreshold(Level.DEBUG);
        fileAppender.setAppend(true);
        fileAppender.activateOptions();
        logger.addAppender(fileAppender);
        ConsoleAppender consoleAppender = new ConsoleAppender();
        consoleAppender.setLayout(fileAppender.getLayout());
        consoleAppender.setThreshold(Level.DEBUG);
        consoleAppender.activateOptions();
        logger.addAppender(consoleAppender);

        listGroups = db.getList("GROUPS");
        random = new Random();
        uncheckedGroups = new ArrayList<>();
        Map<Integer, Nutzer> mapNutzer = db.getMap("NUTZER");
        nutzermanager = new NutzerManager(mapNutzer);
        ausgabe = new Ausgabe(creatorId(), listGroups, silent, new Locale("de", "DE"), sender);

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            logger.info("Shutdown Hook triggered");
            nutzermanager.saveNutzer();
            db.commit();
            try {
                db.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }));

        ScheduledExecutorService backupTask = Executors.newSingleThreadScheduledExecutor();
        backupTask.scheduleAtFixedRate(() -> {
            nutzermanager.saveNutzer();
            db.commit();
        }, 10, 10, TimeUnit.MINUTES);
    }

    @PostConstruct
    private void register() {
        try {
            new TelegramBotsApi().registerBot(this);
        } catch (TelegramApiRequestException e) {
            e.printStackTrace();
        }
        if (ANNOUNCE_STARTUP)
            ausgabe.sendToAllGroups("system.started");
    }

    @SuppressWarnings({"unused", "unchecked"})
    public Ability cmdStats() {
        return Ability.builder()
                .name("stats")
                .info("Nutzer-Stats")
                .flag(update -> nutzermanager.hasNutzer(AbilityUtils.getUser(update).getId()))
                .privacy(PUBLIC)
                .locality(USER)
                .input(0)
                .action(ctx -> {
                    Nutzer nutzer = nutzermanager.getNutzer(ctx.user());
                    ausgabe.send(ctx.chatId(), nutzer.getLocale(), "user.stats", nutzer.getUserId(), nutzer.getUsername(), nutzer.getPoints(), nutzer.getVotes(), nutzer.getLocale().toString());//TODO
                })
                .build();
    }

    @SuppressWarnings("unused")
    public Ability cmdStop() {
        return Ability.builder()
                .name("stop")
                .privacy(ADMIN)
                .locality(ALL)
                .input(0)
                .action(ctx -> {
                    ausgabe.send(ctx.chatId(), nutzermanager.getNutzer(ctx.user()).getLocale(), "system.shutdown");
                    nutzermanager.saveNutzer();
                    db.commit();
                    System.exit(0);
                })
                .build();
    }

    @SuppressWarnings("unused")
    public Ability cmdSave() {
        return Ability.builder()
                .name("save")
                .privacy(ADMIN)
                .locality(ALL)
                .input(0)
                .action(ctx -> {
                    Optional<Message> r = ausgabe.send(ctx.chatId(), nutzermanager.getNutzer(ctx.user()).getLocale(), "system.saving");
                    nutzermanager.saveNutzer();
                    db.commit();
                    r.ifPresent(message -> ausgabe.edit(message.getChatId(), message.getMessageId(), nutzermanager.getNutzer(ctx.user()).getLocale(), "system.saved"));
                })
                .build();
    }

    @SuppressWarnings("unused")
    public Ability cmdTopPoints() {
        return Ability.builder()
                .name("top")
                .info("Zeigt die Top Punkte Nutzer an")
                .privacy(PUBLIC)
                .locality(ALL)
                .input(0)
                .action(ctx -> {
                    logger.debug("Top Points abgefragt");
                    List<Nutzer> topList = nutzermanager.getNutzerListeTopPoints(10);
                    StringBuilder msg = new StringBuilder("Top-Liste:\n");
                    topList.forEach(entry -> msg.append(entry.getLinkedPointListEntry()).append("\n"));
                    ausgabe.sendRaw(ctx.chatId(), msg.toString());
                })
                .build();
    }

    @SuppressWarnings("unused")
    public Ability cmdTopRep() {
        return Ability.builder()
                .name("ehre")
                .info("Zeigt die Top Ehren Nutzer an")
                .privacy(PUBLIC)
                .locality(ALL)
                .input(0)
                .action(ctx -> {
                    logger.debug("Top Rep abgefragt");
                    List<Nutzer> topList = nutzermanager.getNutzerListeTopVotes(10);
                    StringBuilder msg = new StringBuilder("Top-Liste:\n");
                    topList.forEach(entry -> msg.append(entry.getLinkedVoteListEntry()).append("\n"));
                    ausgabe.sendRaw(ctx.chatId(), msg.toString());
                })
                .build();
    }

    @SuppressWarnings("unused")
    public Ability cmdAdmin() {
        return Ability.builder()
                .name("admin")
                .privacy(ADMIN)
                .locality(USER)
                .input(0)
                .action(ctx -> {
                    logger.debug("Admin Menu geoeffnet");
                    Nutzer nutzer = nutzermanager.getNutzer(ctx.user());
                    int levelVorher = nutzer.getLevel();
                    checkLevelUp(nutzer, levelVorher);
                })
                .build();
    }

    @SuppressWarnings("unused")
    public Ability cmdChances() {
        return Ability.builder()
                .name("chances")
                .privacy(ADMIN)
                .locality(USER)
                .input(0)
                .action(ctx -> {
                    logger.debug("Chances geoeffnet");
                    StringBuilder stringBuilder = new StringBuilder();
                    Pair<Integer, HashMap<Integer, Language>> m = Language.getLootMap();
                    m.component2().values().forEach((lang) -> stringBuilder.append(lang.title()).append(" | ").append((float) lang.rarity() / m.component1()).append("%\n"));
                    ausgabe.sendRaw(ctx.chatId(), stringBuilder.toString());
                })
                .build();
    }

    @SuppressWarnings("unused")
    public Reply onReply() {
        return Reply.of(update -> {
            try {
                Message message = update.getMessage();
                if (message.getReplyToMessage().getFrom().equals(this.getMe())) {
                    Nutzer nutzer = nutzermanager.getNutzer(message.getFrom());
                    if (nutzer.getState().equals(State.Name)) {
                        if (isUsernameValid(message.getText())) {
                            nutzer.setUsername(message.getText());
                            ausgabe.sendKeyboard(message.getChatId(), getSettingsKeyboard(nutzer.getLocale()), nutzer.getLocale(), "user.settings.name.changed", nutzer.getUsername());
                        } else {
                            ausgabe.sendKeyboard(message.getChatId(), getSettingsKeyboard(nutzer.getLocale()), nutzer.getLocale(), "user.settings.name.unchanged");
                        }
                    }
                }
            } catch (TelegramApiException e) {
                e.printStackTrace();
            }
        }, Flag.MESSAGE, Flag.REPLY, AbilityUtils::isUserMessage, isAbility().negate());
    }

    private Predicate<Update> isAbility() {
        return update -> update.hasMessage() &&
                update.getMessage().isCommand() &&
                abilities().keySet().stream().anyMatch(s -> s.toLowerCase().startsWith(update.getMessage().getText().substring(1).toLowerCase()));
    }

    @SuppressWarnings("unused")
    public Reply onUserMessage() {
        return Reply.of(update -> {
            Message message = update.getMessage();
            Nutzer nutzer = nutzermanager.getNutzer(message.getFrom());
            String msg = update.getMessage().getText();
            if (msg.equals(Ausgabe.format(nutzer.getLocale(), "user.settings.name.button"))) {
                nutzer.setState(State.Name);
                ausgabe.sendKeyboard(message.getChatId(), new ForceReplyKeyboard(), nutzer.getLocale(), "user.settings.name");
            } else if (msg.equals(Ausgabe.format(nutzer.getLocale(), "user.settings.shop.button"))) {
                nutzer.setState(State.Shop);
                InlineKeyboardMarkup keyboard = InlineKeyboardFactory.build().addRow(InlineKeyboardFactory.button(Ausgabe.format(nutzer.getLocale(), "user.shop.entry", Ausgabe.format(nutzer.getLocale(), ShopItem.LanguageBox.title()), ShopItem.LanguageBox.price()), ShopItem.LanguageBox.data())).toMarkup();
                ausgabe.sendKeyboard(message.getChatId(), keyboard, nutzer.getLocale(), "user.shop", nutzer.getPoints());
            } else if (msg.equals(Ausgabe.format(nutzer.getLocale(), "user.settings.event.button"))) {
                nutzer.setState(State.Event);
                ausgabe.sendKeyboard(message.getChatId(), new ReplyKeyboardRemove(), nutzer.getLocale(), "user.settings.event");
            } else if (msg.equals(Ausgabe.format(nutzer.getLocale(), "user.settings.language.button"))) {
                nutzer.setState(State.Language);
                if (nutzer.getLanguages().size() == 0)
                    ausgabe.send(message.getChatId(), nutzer.getLocale(), "user.settings.nolanguages");
                else {
                    InlineKeyboardMarkup keyboard = getLanguageKeyboard(nutzer, 0);
                    ausgabe.sendKeyboard(message.getChatId(), keyboard, nutzer.getLocale(), "user.settings.language");
                }
            } else {
                ausgabe.sendKeyboard(update.getMessage().getChatId(), getSettingsKeyboard(nutzer.getLocale()), nutzer.getLocale(), "user.settings");
            }
        }, Flag.MESSAGE, Flag.REPLY.negate(), AbilityUtils::isUserMessage, isAbility().negate());
    }

    private InlineKeyboardMarkup getLanguageKeyboard(Nutzer nutzer, int seite) {
        Set<Language> languages = nutzer.getLanguages();
        var ref = new Object() {
            boolean lineBreak = false;
            ArrayList<InlineKeyboardButton> row = new ArrayList<>();
        };
        InlineKeyboardFactory factory = InlineKeyboardFactory.build();
        languages.stream().sorted().limit(6 * (1 + seite)).skip(6 * seite).forEachOrdered(language -> {
            ref.row.add(InlineKeyboardFactory.button(language.title(), "lng_" + language.name()));
            if (ref.lineBreak) {
                factory.addRow(ref.row);
                ref.row = new ArrayList<>();
            }
            ref.lineBreak = !ref.lineBreak;
        });
        if (ref.lineBreak) {
            factory.addRow(ref.row);
            ref.row = new ArrayList<>();
        }
        if (seite > 0)
            ref.row.add(InlineKeyboardFactory.button("<<", "lngs_" + (seite - 1)));
        if (languages.size() > 6 * (1 + seite))
            ref.row.add(InlineKeyboardFactory.button(">>", "lngs_" + (seite + 1)));
        if (!ref.row.isEmpty())
            factory.addRow(ref.row);
        return factory.toMarkup();
    }

    private ReplyKeyboard getSettingsKeyboard(Locale locale) {
        return ReplyKeyboardFactory.build()
                .addRow(ReplyKeyboardFactory.button(Ausgabe.format(locale, "user.settings.name.button")), ReplyKeyboardFactory.button(Ausgabe.format(locale, "user.settings.shop.button")))
                .addRow(ReplyKeyboardFactory.button(Ausgabe.format(locale, "user.settings.event.button")), ReplyKeyboardFactory.button(Ausgabe.format(locale, "user.settings.language.button")))
                .toMarkup();
    }

    private boolean isUsernameValid(String username) {
        return username.matches("[A-Za-zäÄöÖüÜß]{2,}");
    }


    @SuppressWarnings("unused")
    public Reply onCallback() {
        return Reply.of(update -> {
            ausgabe.answerCallback(update.getCallbackQuery().getId());
            String data = update.getCallbackQuery().getData();
            Nutzer nutzer = nutzermanager.getNutzer(update.getCallbackQuery().getFrom());
            System.out.println("CALLBACK: " + data);
            if (data.startsWith("adm")) {
                onCallbackAdminGroup(update);
            } else if (data.startsWith("shop_")) {
                Arrays.stream(ShopItem.values()).filter(item -> data.startsWith(item.data())).findAny().ifPresent(shopItem -> {
                    if (shopItem.price() <= nutzer.getPoints()) {
                        if (data.endsWith("_buy")) {
                            ausgabe.removeKeyboard(update.getCallbackQuery().getMessage().getChatId(), update.getCallbackQuery().getMessage().getMessageId());
                            nutzer.removePoints(shopItem.price());
                            ausgabe.send(update.getCallbackQuery().getMessage().getChatId(), nutzer.getLocale(), "user.shop.bought", Ausgabe.format(nutzer.getLocale(), shopItem.title()), shopItem.price(), nutzer.getPoints());
                            shopItem.onBuy(nutzer, ausgabe, update.getCallbackQuery().getMessage().getChatId());
                        } else {
                            ausgabe.editKeyboard(update.getCallbackQuery().getMessage().getChatId(), update.getCallbackQuery().getMessage().getMessageId(), InlineKeyboardFactory.build()
                                            .addRow(InlineKeyboardFactory.button(Ausgabe.format(nutzer.getLocale(), "user.shop.buy"), shopItem.data() + "_buy"))
                                            .addRow(InlineKeyboardFactory.button(Ausgabe.format(nutzer.getLocale(), "user.shop.cancel"), "cancel_shop"))
                                            .toMarkup(),
                                    nutzer.getLocale(), "user.shop.item", Ausgabe.format(nutzer.getLocale(), shopItem.title()), shopItem.price(), nutzer.getPoints());
                        }
                    } else {
                        ausgabe.edit(update.getCallbackQuery().getMessage().getChatId(), update.getCallbackQuery().getMessage().getMessageId(), nutzer.getLocale(), "user.shop.nomoney", Ausgabe.format(nutzer.getLocale(), shopItem.title()), shopItem.price(), nutzer.getPoints());
                    }
                });
            } else if (data.equals("cancel_shop")) {
                ausgabe.removeKeyboard(update.getCallbackQuery().getMessage().getChatId(), update.getCallbackQuery().getMessage().getMessageId());
                ausgabe.send(update.getCallbackQuery().getMessage().getChatId(), nutzer.getLocale(), "user.shop.canceled");
            } else if (data.startsWith("lng_")) {
                Optional<Language> lang = nutzer.getLanguages().stream().filter(language -> language.name().equals(data.substring(4))).findAny();
                if (lang.isEmpty()) return;
                String[] splits = lang.get().data().split("_");
                nutzer.setLocale((splits.length > 0) ? splits[0] : null, (splits.length > 1) ? splits[1] : null, (splits.length > 2) ? splits[2] : null);
                ausgabe.editKeyboard(update.getCallbackQuery().getMessage().getChatId(), update.getCallbackQuery().getMessage().getMessageId(), null, nutzer.getLocale(), "user.settings.language.set", lang.get().title());
            } else if (data.startsWith("lngs_")) {
                int seite = Integer.parseInt(data.substring(5));
                ausgabe.editKeyboard(update.getCallbackQuery().getMessage().getChatId(), update.getCallbackQuery().getMessage().getMessageId(), getLanguageKeyboard(nutzer, seite), nutzer.getLocale(), "user.settings.language");
            }
        }, Flag.CALLBACK_QUERY);
    }

    private void onCallbackAdminGroup(Update update) {
        if (!update.getCallbackQuery().getFrom().getId().equals(creatorId())) return;
        String data = update.getCallbackQuery().getData();
        long chatId = Long.parseLong(data.substring(4));
        if (!uncheckedGroups.contains(chatId)) return;
        uncheckedGroups.remove(chatId);
        if (data.charAt(3) == '+') {
            listGroups.add(chatId);
            ausgabe.answerCallback(update.getCallbackQuery().getId());
            ausgabe.removeMessage(update.getCallbackQuery().getMessage().getChatId(), update.getCallbackQuery().getMessage().getMessageId());
            ausgabe.removeMessage(update.getCallbackQuery().getMessage().getChatId(), update.getCallbackQuery().getMessage().getMessageId());
            ausgabe.sendToGroup(chatId, "group.accepted");
        } else if (data.charAt(3) == '-') {
            ausgabe.sendToGroup(chatId, "group.declined");
            try {
                sender.execute(new LeaveChat().setChatId(chatId));
            } catch (TelegramApiException e) {
                e.printStackTrace();
            }
        }
    }

    @SuppressWarnings("unused")
    public Reply onGroupMessage() {
        return Reply.of(update -> {
            Message message = update.getMessage();
            Nutzer nutzer = nutzermanager.getNutzer(message.getFrom());

            //Prüft ob die Gruppe bestätigt ist
            if (isGroupUnhecked(message.getChat())) return;

            if (message.isReply())
                onGroupMessageReply(message, nutzer);

            //Prüft ob der Nutzer Punkte für die Nachticht erhält
            checkReward(nutzer);

        }, Flag.MESSAGE, update -> {
            Message m = update.getMessage();
            return (m.isSuperGroupMessage() || m.isGroupMessage()) && !m.isCommand() && (m.hasSticker() || m.hasAnimation() || m.hasAudio() || m.hasContact() || m.hasDocument() || m.hasLocation() || m.hasPhoto() || m.hasPoll() || m.hasText() || m.hasVideo() || m.hasVoice() || m.hasVideoNote()) && m.getPinnedMessage() == null && m.getDeleteChatPhoto() == null && m.getLeftChatMember() == null && m.getNewChatTitle() == null && m.getNewChatPhoto() == null;
        });
    }

    private void onGroupMessageReply(Message message, Nutzer nutzer) {
        Message replyTo = message.getReplyToMessage();
        if (!replyTo.getFrom().getBot()) {
            Nutzer ziel = nutzermanager.getNutzer(replyTo.getFrom());
            if (!ziel.equals(nutzer)) {
                //Prüft ob die Nachricht ein Upvote enthält
                if (!nutzer.hasCooldownUpvote() && startsOrEndsWith(message.getText(), "\\u002b|\\u261d|\\ud83d\\udc46|\\ud83d\\udc4f|\\ud83d\\ude18|\\ud83d\\ude0d|\\ud83d\\udc4c|\\ud83d\\udc4d|\\ud83d\\ude38")) {
                    ziel.addVote(1);
                    nutzer.setCooldownUpvote(5, ChronoUnit.MINUTES);
                    ausgabe.sendTempClear(message.getChatId(), nutzer.getLocale(), "vote.up", 1, ziel.getLinkedVotes(), nutzer.getLinkedVotes());
                }

                //Prüft ob die Nachricht ein Super-Upvote enthält
                if (!nutzer.hasCooldownSuperUpvote() && startsOrEndsWith(message.getText(), "\\u2764\\ufe0f|\\ud83d\\udc96|\\ud83e\\udde1|\\ud83d\\udc9b|\\ud83d\\udc9a|\\ud83d\\udc99|\\ud83d\\udc9c|\\ud83d\\udda4")) {
                    int points = 2 + random.nextInt(SUPER_HONOR_MAX - 1);
                    ziel.addVote(points);
                    nutzer.setCooldownSuperUpvote(5, ChronoUnit.HOURS);
                    ausgabe.sendTempClear(message.getChatId(), nutzer.getLocale(), "vote.super", points, ziel.getLinkedVotes(), nutzer.getLinkedVotes());
                }

                //Prüft ob die Nachricht ein Downvote enthält
                if (!nutzer.hasCooldownDownvote() && startsOrEndsWith(message.getText(), "\\u2639\\ufe0f|\\ud83d\\ude20|\\ud83d\\ude21|\\ud83e\\udd2c|\\ud83e\\udd2e|\\ud83d\\udca9|\\ud83d\\ude3e|\\ud83d\\udc4e|\\ud83d\\udc47")) {
                    ziel.removeVote(1);
                    nutzer.setCooldownDownvote(10, ChronoUnit.MINUTES);
                    ausgabe.sendTempClear(message.getChatId(), nutzer.getLocale(), "vote.down", 1, ziel.getLinkedVotes(), nutzer.getLinkedVotes());
                }
            }
        }
    }

    private void checkReward(Nutzer nutzer) {
        if (!nutzer.hasCooldownReward()) {
            Random r = new Random();
            int reward = (r.nextInt(NACHRICHT_PUNKTE_MAX) + NACHRICHT_PUNKTE_MIN);
            if (random.nextInt(JACKPOT_CHANCE) == 0) {
                reward = reward * JACKPOT_MULTIPLIER;
                ausgabe.sendToAllGroups("reward.jackpot", reward, nutzer.getLinkedUsername());
            }
            int levelVorher = nutzer.getLevel();
            nutzer.addPoints(reward);
            checkLevelUp(nutzer, levelVorher);
            nutzer.setCooldownReward(r.nextInt(NACHRICHT_COOLDOWN_MAX) + NACHRICHT_COOLDOWN_MIN, ChronoUnit.MINUTES);
        }
    }

    private boolean isGroupUnhecked(Chat groupChat) {
        if (!listGroups.contains(groupChat.getId())) {
            if (uncheckedGroups.contains(groupChat.getId())) return true;
            uncheckedGroups.add(groupChat.getId());
            ausgabe.sendOwnerGroupCheck(groupChat.getId(), groupChat.getTitle());
            ausgabe.sendToGroup(groupChat.getId(), "group.requested");
            return true;
        }
        return false;
    }

    private void checkLevelUp(Nutzer nutzer, int levelVorher) {
        if (levelVorher < nutzer.getLevel()) {
            if (levelVorher == nutzer.getLevel() - 1) {
                ausgabe.sendImageToAll(nutzer.getLocale(), "levelup.single", "smug.moe/smg/" + nutzer.getLevel() + ".png", nutzer.getLinkedUsername(), nutzer.getTitel());
            } else {
                ausgabe.sendImageToAll(nutzer.getLocale(), "levelup.multi", "smug.moe/smg/" + nutzer.getLevel() + ".png", nutzer.getLinkedUsername(), nutzer.getTitel(), nutzer.getLevel() - levelVorher - 1);
            }
        }
    }

    private boolean startsOrEndsWith(String text, String regex) {
        return (text.matches("^(" + regex + ")+[\\s\\S]*") || text.matches("[\\s\\S]*(" + regex + ")+$"));
    }

    /**
     * This is very important to override for . By default, any update that does not have a message will not pass through abilities.
     * To customize that, you can just override this global flag and make it return true at every update.
     * This way, the ability flags will be the only ones responsible for checking the update's validity.
     */
    @Override
    public boolean checkGlobalFlags(Update update) {
        return true;
    }
}