package cf.timsprojekte;

import cf.timsprojekte.db.*;
import cf.timsprojekte.minecraft.MinecraftManager;
import kotlin.Pair;
import org.apache.log4j.*;
import org.mapdb.DBMaker;
import org.springframework.stereotype.Component;
import org.telegram.abilitybots.api.bot.AbilityBot;
import org.telegram.abilitybots.api.db.MapDBContext;
import org.telegram.abilitybots.api.objects.*;
import org.telegram.abilitybots.api.util.AbilityUtils;
import org.telegram.telegrambots.meta.TelegramBotsApi;
import org.telegram.telegrambots.meta.api.methods.GetFile;
import org.telegram.telegrambots.meta.api.methods.GetUserProfilePhotos;
import org.telegram.telegrambots.meta.api.methods.groupadministration.LeaveChat;
import org.telegram.telegrambots.meta.api.objects.*;
import org.telegram.telegrambots.meta.api.objects.inlinequery.InlineQuery;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.*;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.meta.exceptions.TelegramApiRequestException;

import javax.annotation.PostConstruct;
import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.font.*;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.*;
import java.io.*;
import java.io.File;
import java.text.AttributedCharacterIterator;
import java.text.AttributedString;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.List;
import java.util.concurrent.*;
import java.util.function.Predicate;

import static cf.timsprojekte.Translation.*;
import static org.telegram.abilitybots.api.objects.Locality.*;
import static org.telegram.abilitybots.api.objects.Privacy.ADMIN;
import static org.telegram.abilitybots.api.objects.Privacy.PUBLIC;

@Component
public class Bot extends AbilityBot {

    //
    // CONSTANTS
    //

    private static final int JACKPOT_CHANCE = 1000;
    private static final int SUPER_HONOR_MAX = 5;
    private static final int NACHRICHT_PUNKTE_MIN = 1;
    private static final int NACHRICHT_PUNKTE_MAX = 12;
    private static final int NACHRICHT_COOLDOWN_MIN = 1;
    private static final int NACHRICHT_COOLDOWN_MAX = 4;
    private static final int JACKPOT_MULTIPLIER = 10;
    private static final boolean ANNOUNCE_STARTUP = false;

    //
    // VARIABLES
    //

    private final Logger logger;
    private final Runnable saveRunnable;
    private final PlaceManager placemanager;
    private final CodeManager codemanager;
    private final MinecraftManager mcmanager;
    private List<Long> listGroups;
    private ArrayList<Long> uncheckedGroups;
    private Random random;
    private NutzerManager nutzermanager;
    private Ausgabe ausgabe;

    //
    // SETUP
    //

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
        fileAppender.setThreshold(Level.INFO);
        fileAppender.setAppend(true);
        fileAppender.activateOptions();
        logger.addAppender(fileAppender);
        ConsoleAppender consoleAppender = new ConsoleAppender();
        consoleAppender.setLayout(fileAppender.getLayout());
        consoleAppender.setThreshold(Level.ERROR);
        consoleAppender.activateOptions();
        logger.addAppender(consoleAppender);

        listGroups = db.getList("GROUPS");
        random = new Random();
        uncheckedGroups = new ArrayList<>();
        Map<Integer, Nutzer> mapNutzer = db.getMap("NUTZER");
        nutzermanager = new NutzerManager(mapNutzer);
        placemanager = new PlaceManager();
        ausgabe = new Ausgabe(creatorId(), listGroups, silent, new Locale("de", "DE"), sender);
        Map<String, String> mapCodes = db.getMap("CODES");
        codemanager = new CodeManager(mapCodes);

        mcmanager = new MinecraftManager();

        saveRunnable = () -> {
            nutzermanager.saveNutzer();
            placemanager.savePlace();
            db.commit();
        };


        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            logger.info("Shutdown Hook ausgeführt");
            saveRunnable.run();
            try {
                db.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }));
        ScheduledExecutorService backupTask = Executors.newSingleThreadScheduledExecutor();
        backupTask.scheduleAtFixedRate(saveRunnable, 10, 10, TimeUnit.MINUTES);
    }

    @PostConstruct
    private void register() {
        try {
            new TelegramBotsApi().registerBot(this);
        } catch (TelegramApiRequestException e) {
            e.printStackTrace();
        }
        if (ANNOUNCE_STARTUP)
            ausgabe.sendToAllGroups(system_started);
    }

    //
    // COMMANDS
    //

    @SuppressWarnings({"unused", "unchecked"})
    public Ability cmdServer() {
        return Ability.builder()
                .name("server")
                .info("MC Server")
                .privacy(PUBLIC)
                .locality(ALL)
                .input(0)
                .action(ctx -> {
                    String msg;
                    if (mcmanager.info.isOnline())
                        msg = "<b>Minecraft " + mcmanager.info.getVersion() + "</b>\n" + (mcmanager.info.getModded() ? "(modded)\n" : "") + "<i>" + mcmanager.info.getMotd() + "</i>\n(" + mcmanager.info.getCurrentPlayers() + "/" + mcmanager.info.getMaximumPlayers() + ") " + mcmanager.info.getLatency() + "ms\n" + mcmanager.info.getSamplesFormatted();
                    else if (mcmanager.info.hasError()) {
                        msg = "Fehlerhafte Antwort des Servers...";
                    } else {
                        msg = "Der Server ist nicht erreichbar...";
                    }
                    ausgabe.sendRaw(ctx.chatId(), msg);
                })
                .build();
    }

    public Ability cmdCode() {
        return Ability.builder()
                .name("code")
                .privacy(PUBLIC)
                .locality(ALL)
                .action(ctx -> {
                    Nutzer nutzer = nutzermanager.getNutzer(ctx.user());
                    if (ctx.arguments().length < 2) {
                        ausgabe.sendTemp(ctx.chatId(), nutzer.getLocale(), code_moreArgs);
                        return;
                    }
                    StringBuilder text = new StringBuilder();
                    for (int i = 1; i < ctx.arguments().length; i++) {
                        text.append((i == 1) ? "" : " ").append(ctx.arguments()[i]);
                    }
                    if (!codemanager.contains(ctx.firstArg())) {
                        if (!ctx.firstArg().matches("\\d+(-\\d+)?")) {
                            ausgabe.sendTemp(ctx.chatId(), nutzer.getLocale(), code_invalid, ctx.firstArg());
                        } else {
                            ausgabe.send(ctx.chatId(), nutzer.getLocale(), code_set, ctx.firstArg(), text.toString());
                            codemanager.add(ctx.firstArg(), text.toString());
                            logger.info("Code "+ctx.firstArg()+" von "+nutzer.getUsername()+" hinzugefügt. \""+text.toString()+"\"");
                        }
                    } else {
                        ausgabe.sendTemp(ctx.chatId(), nutzer.getLocale(), code_taken, ctx.firstArg());
                    }
                })
                .build();
    }

    public Ability cmdCodes() {
        return Ability.builder()
                .name("codelist")
                .privacy(PUBLIC)
                .locality(ALL)
                .input(0)
                .action(ctx -> {
                    Nutzer nutzer = nutzermanager.getNutzer(ctx.user());
                    ausgabe.sendRaw(ctx.chatId(), codemanager.list());
                })
                .build();
    }

    public Ability cmdStats() {
        return Ability.builder()
                .name("stats")
                .privacy(PUBLIC)
                .locality(ALL)
                .input(0)
                .action(ctx -> {
                    Nutzer nutzer = nutzermanager.getNutzer(ctx.user());
                    User target = ctx.user();
                    if (ctx.update().getMessage().isReply()) {
                        target = ctx.update().getMessage().getReplyToMessage().getFrom();
                    }
                    if (target.getBot()) {
                        ausgabe.send(ctx.chatId(), nutzer.getLocale(), stats_bot);
                        return;
                    }
                    try {
                        BufferedImage photo = generateStatsImage(target);
                        ByteArrayOutputStream os = new ByteArrayOutputStream();
                        ImageIO.write(photo, "png", os);
                        ausgabe.sendImage(ctx.chatId(), "stats", new ByteArrayInputStream(os.toByteArray()));
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                })
                .build();
    }

    public Ability cmdRemoveCode() {
        return Ability.builder()
                .name("removecode")
                .privacy(ADMIN)
                .locality(ALL)
                .action(ctx -> {
                    Nutzer nutzer = nutzermanager.getNutzer(ctx.user());
                    if (ctx.arguments().length < 1) {
                        ausgabe.sendTemp(ctx.chatId(), nutzer.getLocale(), code_moreArgs);
                        return;
                    }
                    if (codemanager.contains(ctx.firstArg())) {
                        ausgabe.sendTemp(ctx.chatId(), nutzer.getLocale(), code_removed, ctx.firstArg());
                        codemanager.remove(ctx.firstArg());
                    } else {
                        ausgabe.sendTemp(ctx.chatId(), nutzer.getLocale(), code_nonexistent, ctx.firstArg());
                    }
                })
                .build();
    }

    @SuppressWarnings({"unused", "unchecked"})
    public Ability cmdAddPoints() {
        return Ability.builder()
                .name("addPoints")
                .info("Punkte hinzufügen")
                .flag(update -> nutzermanager.hasNutzer(AbilityUtils.getUser(update).getId()))
                .privacy(ADMIN)
                .locality(GROUP)
                .input(1)
                .action(ctx -> {
                    if (ctx.update().hasMessage()) {
                        if (ctx.update().getMessage().isReply()) {
                            Nutzer replyTo = nutzermanager.getNutzer(ctx.update().getMessage().getReplyToMessage().getFrom().getId());
                            int pts;
                            try {
                                pts = Integer.parseInt(ctx.firstArg());
                            } catch (NumberFormatException e) {
                                return;
                            }
                            int vorher = replyTo.getLevel();
                            replyTo.addPoints(pts);
                            checkLevelUp(replyTo, vorher);
                            //TODO Announce
                        }
                    }
                })
                .build();
    }

    @SuppressWarnings({"unused", "unchecked"})
    public Ability cmdAddVotes() {
        return Ability.builder()
                .name("addVotes")
                .info("Votes hinzufügen")
                .flag(update -> nutzermanager.hasNutzer(AbilityUtils.getUser(update).getId()))
                .privacy(ADMIN)
                .locality(GROUP)
                .input(1)
                .action(ctx -> {
                    if (ctx.update().hasMessage()) {
                        if (ctx.update().getMessage().isReply()) {
                            Nutzer replyTo = nutzermanager.getNutzer(ctx.update().getMessage().getReplyToMessage().getFrom().getId());
                            int votes;
                            try {
                                votes = Integer.parseInt(ctx.firstArg());
                            } catch (NumberFormatException e) {
                                return;
                            }
                            replyTo.addVote(votes);
                            //TODO Announce
                        }
                    }
                })
                .build();
    }

    @SuppressWarnings({"unused", "unchecked"})
    public Ability cmdSetPoints() {
        return Ability.builder()
                .name("setPoints")
                .info("Punkte setzen")
                .flag(update -> nutzermanager.hasNutzer(AbilityUtils.getUser(update).getId()))
                .privacy(ADMIN)
                .locality(GROUP)
                .input(1)
                .action(ctx -> {
                    if (ctx.update().hasMessage()) {
                        if (ctx.update().getMessage().isReply()) {
                            Nutzer replyTo = nutzermanager.getNutzer(ctx.update().getMessage().getReplyToMessage().getFrom().getId());
                            int pts;
                            try {
                                pts = Integer.parseInt(ctx.firstArg());
                            } catch (NumberFormatException e) {
                                return;
                            }
                            int vorher = replyTo.getLevel();
                            replyTo.setPoints(pts);
                            checkLevelUp(replyTo, vorher);
                            //TODO Announce
                        }
                    }
                })
                .build();
    }

    @SuppressWarnings({"unused", "unchecked"})
    public Ability cmdSetVotes() {
        return Ability.builder()
                .name("setVotes")
                .info("Votes setzen")
                .flag(update -> nutzermanager.hasNutzer(AbilityUtils.getUser(update).getId()))
                .privacy(ADMIN)
                .locality(GROUP)
                .input(1)
                .action(ctx -> {
                    if (ctx.update().hasMessage()) {
                        if (ctx.update().getMessage().isReply()) {
                            Nutzer replyTo = nutzermanager.getNutzer(ctx.update().getMessage().getReplyToMessage().getFrom().getId());
                            int votes;
                            try {
                                votes = Integer.parseInt(ctx.firstArg());
                            } catch (NumberFormatException e) {
                                return;
                            }
                            replyTo.setVotes(votes);
                            //TODO Announce
                        }
                    }
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
                    ausgabe.send(ctx.chatId(), nutzermanager.getNutzer(ctx.user()).getLocale(), system_shutdown);
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
                    Optional<Message> r = ausgabe.send(ctx.chatId(), nutzermanager.getNutzer(ctx.user()).getLocale(), system_saving);
                    saveRunnable.run();
                    db.commit();
                    r.ifPresent(message -> ausgabe.edit(message.getChatId(), message.getMessageId(), nutzermanager.getNutzer(ctx.user()).getLocale(), system_saved));
                })
                .build();
    }

    @SuppressWarnings("unused")
    public Ability cmdLoad() {
        return Ability.builder()
                .name("load")
                .privacy(ADMIN)
                .locality(ALL)
                .input(0)
                .action(ctx -> {
                    nutzermanager.loadNutzer();

                })
                .build();
    }

    @SuppressWarnings("unused")
    public Ability cmdCancel() {
        return Ability.builder()
                .name("cancel")
                .privacy(PUBLIC)
                .locality(USER)
                .input(0)
                .action(ctx -> {
                    Nutzer nutzer = nutzermanager.getNutzer(ctx.user());
                    ausgabe.sendKeyboard(ctx.chatId(), getMainKeyboard(nutzer.getLocale()), nutzer.getLocale(), canceled);
                    nutzer.setState(State.Default);
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
                    List<Nutzer> topList = nutzermanager.getNutzerListeTopPoints(10);
                    StringBuilder msg = new StringBuilder("Top-Liste:\n");
                    topList.forEach(entry -> msg.append(entry.getLinkedPointListEntry()).append("\n"));
                    ausgabe.sendRaw(ctx.chatId(), msg.toString());
                })
                .build();
    }

    @SuppressWarnings("unused")
    public Ability cmdPlace() {
        return Ability.builder()
                .name("place")
                .info("r/place")
                .privacy(PUBLIC)
                .locality(ALL)
                .input(0)
                .action(ctx -> {
                    Nutzer nutzer = nutzermanager.getNutzer(ctx.user());
                    if (ctx.arguments().length == 0) {
                        //Ausgabe
                        ausgabe.sendImage(ctx.chatId(), "place", placemanager.getImageStream());
                    } else if (ctx.arguments().length > 2) {
                        int x, y;
                        try {
                            x = Integer.valueOf(ctx.firstArg());
                            y = Integer.valueOf(ctx.secondArg());
                        } catch (NumberFormatException e) {
                            ausgabe.sendTempRaw(ctx.chatId(), "Unbekannte Koordinaten.");
                            return;
                        }
                        if (x >= placemanager.getWidth() || y >= placemanager.getHeight() || x < 0 || y < 0) {
                            ausgabe.sendTempRaw(ctx.chatId(), "Koordinaten ausserhalb des Feldes.");
                            return;
                        }
                        Color color;
                        if (ctx.arguments().length == 3) {
                            //Eingabe Farbe
                            switch (ctx.thirdArg().toLowerCase()) {
                                case "rot":
                                    color = Color.RED;
                                    break;
                                case "grün":
                                    color = Color.GREEN;
                                    break;
                                case "blau":
                                    color = Color.BLUE;
                                    break;
                                case "gelb":
                                    color = Color.YELLOW;
                                    break;
                                case "schwarz":
                                    color = Color.BLACK;
                                    break;
                                case "weiß":
                                    color = Color.WHITE;
                                    break;
                                case "grau":
                                    color = Color.GRAY;
                                    break;
                                case "cyan":
                                    color = Color.CYAN;
                                    break;
                                case "magenta":
                                    color = Color.MAGENTA;
                                    break;
                                case "hellgrau":
                                    color = Color.LIGHT_GRAY;
                                    break;
                                default:
                                    ausgabe.sendTempRaw(ctx.chatId(), "Unbekannte Farbe.\n(rot/grün/blau/gelb/schwarz/weiß/grau/cyan/magenta/hellgrau)");
                                    return;
                            }
                        } else if (ctx.arguments().length == 5) {
                            try {
                                int r = Integer.parseInt(ctx.arguments()[2]);
                                int g = Integer.parseInt(ctx.arguments()[3]);
                                int b = Integer.parseInt(ctx.arguments()[4]);
                                color = new Color(r, g, b);
                            } catch (NumberFormatException e) {
                                ausgabe.sendTempRaw(ctx.chatId(), "Unbekannter Farbcode.");
                                return;
                            }
                            //Eingabe Farbcode
                        } else {
                            ausgabe.sendTempRaw(ctx.chatId(), "Unbekannte Farbe");
                            return;
                        }
                        if (!nutzer.hasCooldownPlace()) {
                            placemanager.setPixel(x, y, color);
                            ausgabe.sendTempRaw(ctx.chatId(), "Pixel gesetzt");
                            nutzer.setCooldownPlace(1, ChronoUnit.MINUTES);
                        } else {
                            ausgabe.sendTempRaw(ctx.chatId(), "Du kannst im Moment keine Pixel setzen");
                        }
                    } else {
                        ausgabe.sendTempRaw(ctx.chatId(), "/place x y farbe");
                    }
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
                    List<Nutzer> topList = nutzermanager.getNutzerListeTopVotes(10);
                    StringBuilder msg = new StringBuilder("Top-Liste:\n");
                    topList.forEach(entry -> msg.append(entry.getLinkedVoteListEntry()).append("\n"));
                    ausgabe.sendRaw(ctx.chatId(), msg.toString());
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
                    StringBuilder stringBuilder = new StringBuilder();
                    Pair<Integer, HashMap<Integer, Language>> m = Language.getLootMap();
                    m.component2().values().forEach((lang) -> stringBuilder.append(lang.title()).append(" | ").append((float) lang.rarity() / m.component1()).append("%\n"));
                    ausgabe.sendRaw(ctx.chatId(), stringBuilder.toString());
                })
                .build();
    }

    //
    // REPLYS
    //

    @SuppressWarnings("unused")
    public Reply onReply() {
        return Reply.of(update -> {
            try {
                Message message = update.getMessage();
                if (message.getReplyToMessage().getFrom().equals(this.getMe())) {
                    Nutzer nutzer = nutzermanager.getNutzer(message.getFrom());
                    if (nutzer.getState().equals(State.Name)) {
                        onReplyUsernameChange(message, nutzer);
                    }
                }
            } catch (TelegramApiException e) {
                e.printStackTrace();
            }
        }, Flag.MESSAGE, Flag.REPLY, AbilityUtils::isUserMessage, isAbility().negate());
    }

    private void onReplyUsernameChange(Message message, Nutzer nutzer) {
        if (isUsernameValid(message.getText())) {
            nutzer.setUsername(message.getText());
            ausgabe.sendKeyboard(message.getChatId(), getMainKeyboard(nutzer.getLocale()), nutzer.getLocale(), user_namechange_changed, nutzer.getUsername());
            nutzer.setState(State.Default);
        } else {
            ausgabe.sendKeyboard(message.getChatId(), getMainKeyboard(nutzer.getLocale()), nutzer.getLocale(), user_namechange_unchanged);
        }
    }

    @SuppressWarnings("unused")
    public Reply onUserMessage() {
        return Reply.of(update -> {
            Message message = update.getMessage();
            Nutzer nutzer = nutzermanager.getNutzer(message.getFrom());
            String msg = update.getMessage().getText();
            if (msg.equals(Ausgabe.format(nutzer.getLocale(), user_namechange_button))) {
                onUserMessageNamechange(message, nutzer);
            } else if (msg.equals(Ausgabe.format(nutzer.getLocale(), user_shop_button))) {
                onUserMessageShop(message, nutzer);
            } else if (msg.equals(Ausgabe.format(nutzer.getLocale(), user_language_button))) {
                onUserMessageLanguage(message, nutzer);
            } else {
                onUserMessageMain(message, nutzer);
            }
        }, Flag.MESSAGE, Flag.REPLY.negate(), AbilityUtils::isUserMessage, isAbility().negate());
    }

    private void onUserMessageMain(Message message, Nutzer nutzer) {
        ausgabe.sendKeyboard(message.getChatId(), getMainKeyboard(nutzer.getLocale()), nutzer.getLocale(), user_main);
    }

    private void onUserMessageLanguage(Message message, Nutzer nutzer) {
        InlineKeyboardMarkup keyboard = getLanguageKeyboard(nutzer, 0);
        ausgabe.sendKeyboard(message.getChatId(), keyboard, nutzer.getLocale(), user_language);
    }

    private void onUserMessageShop(Message message, Nutzer nutzer) {
        InlineKeyboardMarkup keyboard = InlineKeyboardFactory.build().addRow(InlineKeyboardFactory.button(Ausgabe.format(nutzer.getLocale(), user_shop_entry, Ausgabe.formatRaw(nutzer.getLocale(), ShopItem.LanguageBox.title()), ShopItem.LanguageBox.price()), "shop_" + ShopItem.LanguageBox.data())).toMarkup();
        ausgabe.sendKeyboard(message.getChatId(), keyboard, nutzer.getLocale(), user_shop, nutzer.getPoints());
    }

    private void onUserMessageNamechange(Message message, Nutzer nutzer) {
        nutzer.setState(State.Name);
        ausgabe.sendKeyboard(message.getChatId(), new ForceReplyKeyboard(), nutzer.getLocale(), user_namechange);
    }

    public Reply onInlineQuery() {
        return Reply.of(update -> {
            InlineQuery query = update.getInlineQuery();
            String text = (query.hasQuery()) ? query.getQuery() : "";
            String offset = query.getOffset();
            int page;
            try {
                page = Integer.parseInt(offset);
            } catch (NumberFormatException e) {
                page = 0;
            }
            ausgabe.answerInlineQuery(query.getId(), String.valueOf(page + 1), false,
                    codemanager.match(text, page));
        }, Flag.INLINE_QUERY);
    }


    @SuppressWarnings({"unused", "Duplicates"})
    public Reply onCallback() {
        return Reply.of(update -> {
            CallbackQuery query = update.getCallbackQuery();
            String data = query.getData();
            String[] parts = data.split("_");
            Nutzer nutzer = nutzermanager.getNutzer(update.getCallbackQuery().getFrom());
            Message message = update.getCallbackQuery().getMessage();
            if (parts.length > 0) {
                switch (parts[0]) {
                    case "none":
                        ausgabe.answerCallback(query.getId());
                        break;
                    case "cancel_shop":
                        onCallbackCancelShop(query, nutzer, message);
                        break;
                    case "justDeep":
                        onCallbackJustDeep(query, nutzer, message);
                        break;
                    case "justCheap":
                        onCallbackJustCheap(query, nutzer, message);
                        break;
                }
                if (parts.length > 1) {
                    switch (parts[0]) {
                        case "shop":
                            onCallbackShop(query, nutzer, message, parts);
                            break;
                        case "lng":
                            onCallbackLanguage(query, nutzer, message, parts[1]);
                            break;
                        case "listLangs":
                            onCallbackLanguagesPage(query, nutzer, message, parts[1]);
                            break;
                    }
                    if (parts.length > 2) {
                        if ("admGroup".equals(parts[0])) {
                            onCallbackAdminGroup(query, message, parts[1], parts[2]);
                        }
                    }
                }
            }
        }, Flag.CALLBACK_QUERY);
    }

    private void onCallbackJustCheap(CallbackQuery query, Nutzer nutzer, Message message) {
        onCallbackJust(query, nutzer, message, -1);
    }

    private void onCallbackJustDeep(CallbackQuery query, Nutzer nutzer, Message message) {
        onCallbackJust(query, nutzer, message, 1);
    }

    private void onCallbackJust(CallbackQuery query, Nutzer nutzer, Message message, int rating) {
        ausgabe.answerCallback(query.getId());
        String t = message.getCaption();
        if (t == null) t = "";
        int ratingInt = 0;
        t = t.substring(t.indexOf("(") + 1, t.length() - 1);
        try {
            ratingInt = Integer.parseInt(t);
        } catch (NumberFormatException e) {
        }
        ratingInt += rating;
        String ratingBool = "Ausgeglichen";
        if (ratingInt > 0) {
            ratingBool = "Deep";
        } else if (ratingInt < 0) {
            ratingBool = "Cheap";
        }
        ausgabe.editCaption(query.getMessage().getChatId(), query.getMessage().getMessageId(), "Deep or Cheap? <b>" + ratingBool + "</b> (" + ratingInt + ")", getDeepOrCheapKeyboard());
    }

    private void onCallbackLanguagesPage(CallbackQuery query, Nutzer nutzer, Message message, String part) {

        ausgabe.answerCallback(query.getId());
        int seite = Integer.parseInt(part);
        ausgabe.editKeyboard(message.getChatId(), message.getMessageId(), getLanguageKeyboard(nutzer, seite), nutzer.getLocale(), user_language);
    }

    private void onCallbackLanguage(CallbackQuery query, Nutzer nutzer, Message message, String part) {
        ausgabe.answerCallback(query.getId());
        Optional<Language> lang = nutzer.getLanguages().stream().filter(language -> language.name().equals(part)).findAny();
        if (lang.isEmpty()) return;
        String[] splits = lang.get().data().split("_");
        nutzer.setLocale((splits.length > 0) ? splits[0] : null, (splits.length > 1) ? splits[1] : null, (splits.length > 2) ? splits[2] : null);
        ausgabe.editKeyboard(message.getChatId(), message.getMessageId(), null, nutzer.getLocale(), user_language_set, lang.get().title());
    }

    private void onCallbackCancelShop(CallbackQuery query, Nutzer nutzer, Message message) {
        ausgabe.answerCallback(query.getId());
        ausgabe.removeKeyboard(message.getChatId(), message.getMessageId());
        ausgabe.send(message.getChatId(), nutzer.getLocale(), user_shop_canceled);
    }

    private void onCallbackShop(CallbackQuery query, Nutzer nutzer, Message message, String[] parts) {
        ausgabe.answerCallback(query.getId());
        Arrays.stream(ShopItem.values()).filter(item -> parts[1].equals(item.data())).findAny().ifPresent(shopItem -> {
            if (shopItem.price() <= nutzer.getPoints()) {
                if (parts.length > 2) {
                    if (parts[2].equals("buy")) {
                        ausgabe.removeKeyboard(message.getChatId(), message.getMessageId());
                        nutzer.removePoints(shopItem.price());
                        ausgabe.send(message.getChatId(), nutzer.getLocale(), user_shop_bought, Ausgabe.formatRaw(nutzer.getLocale(), shopItem.title()), shopItem.price(), nutzer.getPoints());
                        shopItem.onBuy(nutzer, ausgabe, message.getChatId());
                        logger.info(nutzer.getUsername()+" hat "+shopItem.name()+" gekauft");
                    }
                } else {
                    ausgabe.editKeyboard(message.getChatId(), message.getMessageId(), InlineKeyboardFactory.build()
                                    .addRow(InlineKeyboardFactory.button(Ausgabe.format(nutzer.getLocale(), user_shop_buy), "shop_" + shopItem.data() + "_buy"))
                                    .addRow(InlineKeyboardFactory.button(Ausgabe.format(nutzer.getLocale(), user_shop_cancel), "cancel_shop"))
                                    .toMarkup(),
                            nutzer.getLocale(), user_shop_item, Ausgabe.formatRaw(nutzer.getLocale(), shopItem.title()), shopItem.price(), nutzer.getPoints());
                }
            } else {
                ausgabe.edit(message.getChatId(), message.getMessageId(), nutzer.getLocale(), user_shop_nomoney, Ausgabe.formatRaw(nutzer.getLocale(), shopItem.title()), shopItem.price(), nutzer.getPoints());
            }
        });
    }

    private void onCallbackAdminGroup(CallbackQuery query, Message message, String part1, String part2) {
        ausgabe.answerCallback(query.getId());
        if (!query.getFrom().getId().equals(creatorId())) return;
        long chatId = Long.parseLong(part2);
        if (!uncheckedGroups.contains(chatId)) return;
        uncheckedGroups.remove(chatId);
        if (part1.equals("+")) {
            listGroups.add(chatId);
            ausgabe.removeMessage(message.getChatId(), message.getMessageId());
            ausgabe.sendToGroup(chatId, group_accepted);
        } else if (part1.equals("-")) {
            ausgabe.sendToGroup(chatId, group_declined);
            ausgabe.removeMessage(message.getChatId(), message.getMessageId());
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
            if (checkGroup(message.getChat())) return;

            if (message.isReply())
                onGroupMessageReply(message, nutzer);

            //Prüft ob der Nutzer Punkte für die Nachticht erhält
            checkReward(nutzer);
            if (message.hasText() && message.getText().toLowerCase().startsWith("wenn"))
                onGroupMessageJustThings(message);

        }, Flag.MESSAGE, update -> {
            Message m = update.getMessage();
            return (m.isSuperGroupMessage() || m.isGroupMessage()) && !m.isCommand() && (m.hasSticker() || m.hasAnimation() || m.hasAudio() || m.hasContact() || m.hasDocument() || m.hasLocation() || m.hasPhoto() || m.hasPoll() || m.hasText() || m.hasVideo() || m.hasVoice() || m.hasVideoNote()) && m.getPinnedMessage() == null && m.getDeleteChatPhoto() == null && m.getLeftChatMember() == null && m.getNewChatTitle() == null && m.getNewChatPhoto() == null;
        });
    }

    private void onGroupMessageJustThings(Message message) {
        try {
            BufferedImage photo = generateJustThingsImage(message.getText(), AbilityUtils.shortName(message.getFrom()));
            ByteArrayOutputStream os = new ByteArrayOutputStream();
            ImageIO.write(photo, "jpeg", os);
            ausgabe.sendImage(message.getChatId(), "justThings", new ByteArrayInputStream(os.toByteArray()), "Deep or Cheap?", getDeepOrCheapKeyboard());
        } catch (IOException e) {
            e.printStackTrace();
        }
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
                    ausgabe.sendTempClear(message.getChatId(), nutzer.getLocale(), vote_up, 1, ziel.getLinkedVotes(), nutzer.getLinkedVotes());
                }

                //Prüft ob die Nachricht ein Super-Upvote enthält
                if (!nutzer.hasCooldownSuperUpvote() && startsOrEndsWith(message.getText(), "\\u2764\\ufe0f|\\ud83d\\udc96|\\ud83e\\udde1|\\ud83d\\udc9b|\\ud83d\\udc9a|\\ud83d\\udc99|\\ud83d\\udc9c|\\ud83d\\udda4")) {
                    int points = 2 + random.nextInt(SUPER_HONOR_MAX - 1);
                    ziel.addVote(points);
                    nutzer.setCooldownSuperUpvote(5, ChronoUnit.HOURS);
                    ausgabe.sendTempClear(message.getChatId(), nutzer.getLocale(), vote_super, points, ziel.getLinkedVotes(), nutzer.getLinkedVotes());
                }

                //Prüft ob die Nachricht ein Downvote enthält
                if (!nutzer.hasCooldownDownvote() && startsOrEndsWith(message.getText(), "\\u2639\\ufe0f|\\ud83d\\ude20|\\ud83d\\ude21|\\ud83e\\udd2c|\\ud83e\\udd2e|\\ud83d\\udca9|\\ud83d\\ude3e|\\ud83d\\udc4e|\\ud83d\\udc47")) {
                    ziel.removeVote(1);
                    nutzer.setCooldownDownvote(10, ChronoUnit.MINUTES);
                    ausgabe.sendTempClear(message.getChatId(), nutzer.getLocale(), vote_down, 1, ziel.getLinkedVotes(), nutzer.getLinkedVotes());
                }
            }
        }
    }

    //
    // KEYBOARDS
    //

    private InlineKeyboardMarkup getDeepOrCheapKeyboard() {
        return InlineKeyboardFactory.build().addRow(InlineKeyboardFactory.button("\uD83D\uDC4D", "justDeep"), InlineKeyboardFactory.button("\uD83D\uDC4E", "justCheap")).toMarkup();
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
            ref.row.add(InlineKeyboardFactory.button("<<", "listLangs_" + (seite - 1)));
        if (languages.size() > 6 * (1 + seite))
            ref.row.add(InlineKeyboardFactory.button(">>", "listLangs_" + (seite + 1)));
        if (!ref.row.isEmpty())
            factory.addRow(ref.row);
        if (!factory.hasRows())
            factory.addRow(InlineKeyboardFactory.button(Ausgabe.format(nutzer.getLocale(), user_language_none), "none"));
        return factory.toMarkup();
    }


    private ReplyKeyboard getMainKeyboard(Locale locale) {
        return ReplyKeyboardFactory.build()
                .addRow(ReplyKeyboardFactory.button(Ausgabe.format(locale, user_namechange_button)), ReplyKeyboardFactory.button(Ausgabe.format(locale, user_language_button)))
                .addRow(ReplyKeyboardFactory.button(Ausgabe.format(locale, user_shop_button)))
                .toMarkup();
    }

    //
    // CHECKS
    //

    private void checkReward(Nutzer nutzer) {
        if (!nutzer.hasCooldownReward()) {
            Random r = new Random();
            int reward = (r.nextInt(NACHRICHT_PUNKTE_MAX) + NACHRICHT_PUNKTE_MIN);
            if (random.nextInt(JACKPOT_CHANCE) == 0) {
                reward = reward * JACKPOT_MULTIPLIER;
                ausgabe.sendToAllGroups(reward_jackpot, reward, nutzer.getLinkedUsername());
            }
            int levelVorher = nutzer.getLevel();
            nutzer.addPoints(reward);
            checkLevelUp(nutzer, levelVorher);
            nutzer.setCooldownReward(r.nextInt(NACHRICHT_COOLDOWN_MAX) + NACHRICHT_COOLDOWN_MIN, ChronoUnit.MINUTES);
        }
    }

    private void checkLevelUp(Nutzer nutzer, int levelVorher) {
        if (levelVorher < nutzer.getLevel()) {
            logger.info(nutzer.getUsername()+" ist aufgestiegen +"+(nutzer.getLevel()-levelVorher)+" -> "+nutzer.getLevel());
            if (levelVorher == nutzer.getLevel() - 1) {
                ausgabe.sendImageToAll(nutzer.getLocale(), levelup_single, "smug.moe/smg/" + nutzer.getLevel() + ".png", nutzer.getLinkedUsername(), nutzer.getTitel());
            } else {
                ausgabe.sendImageToAll(nutzer.getLocale(), levelup_multi, "smug.moe/smg/" + nutzer.getLevel() + ".png", nutzer.getLinkedUsername(), nutzer.getTitel(), nutzer.getLevel() - levelVorher - 1);
            }
        }
    }

    private boolean checkGroup(Chat groupChat) {
        if (!listGroups.contains(groupChat.getId())) {
            if (uncheckedGroups.contains(groupChat.getId())) return true;
            uncheckedGroups.add(groupChat.getId());
            ausgabe.sendOwnerGroupCheck(groupChat.getId(), groupChat.getTitle());
            ausgabe.sendToGroup(groupChat.getId(), group_requested);
            return true;
        }
        return false;
    }

    //
    // UTILITY
    //

    private boolean isUsernameValid(String username) {
        return username.matches("[A-Za-zäÄöÖüÜß]{2,}");
    }

    private Predicate<Update> isAbility() {
        return update -> update.hasMessage() &&
                update.getMessage().isCommand() &&
                abilities().keySet().stream().anyMatch(s -> update.getMessage().getText().substring(1).toLowerCase().startsWith(s.toLowerCase()));
    }

    private boolean startsOrEndsWith(String text, String regex) {
        return (text.matches("^(" + regex + ")+[\\s\\S]*") || text.matches("[\\s\\S]*(" + regex + ")+$"));
    }


    @SuppressWarnings("DuplicatedCode")
    private BufferedImage generateStatsImage(User target) throws IOException {
        Nutzer nutzer = nutzermanager.getNutzer(target);
        String vorname = target.getFirstName();
        if(vorname==null)vorname="";
        String nachname = target.getLastName();
        if(nachname==null)nachname="";
        String tgname = target.getUserName();
        if(tgname==null)tgname="";
        String sprache = "Standard Deutsch";
        for (Language lang : Language.values()) {
            if (lang.locale().equals(nutzer.getLocale())) {
                sprache = lang.title();
            }
        }
        Integer punkte = nutzer.getPoints();
        Integer level = nutzer.getLevel();
        String username = nutzer.getUsername();
        String titel = nutzer.getTitel();
        Set<Language> sprachen = nutzer.getLanguages();
        Integer votes = nutzer.getVotes();
        // System.out.println(vorname + "\n" + nachname + "\n" + tgname + "\n" + sprache + "\n" + punkte + "\n" + level + "\n" + username + "\n" + titel + "\n" + sprachen.size() + "\n" + votes);
        BufferedImage bild = getProfilePicture(target.getId());
        int width = 800;
        int height = 350;
        int padding = 25;
        int bildSize = height - (padding * 2);
        int infoSize = (width - (padding * 3)) - bildSize;
        int bildTextPadding = 15;
        int bildTextBGMargin = 5;
        float bildTextSize = 12;

        float usernameTextSize = 42;
        float levelTextSize = 30;
        float punkteTextSize = 36;
        float karmaTextSize = 36;
        float paketeTextSize = 26;

        Color c1 = new Color(114, 0, 65);
        Color c2 = new Color(78, 0, 211);
        Color cGray = new Color(0, 0, 0, 80);
        Color cText = new Color(200, 200, 200);
        Color cTag = new Color(200, 200, 200,80);
        Font font = new Font("Arial Nova Light", Font.PLAIN, 30);
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        Graphics2D g2d = (Graphics2D) image.getGraphics();
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                RenderingHints.VALUE_ANTIALIAS_ON);
        //DRAW HINTERGRUND
        g2d.setPaint(new GradientPaint(0, 0, c1, width, height, c2));
        g2d.fillRect(0, 0, width, height);
        //DRAW BILD
        if (bild != null) {
            g2d.drawImage(makeRoundedCorner(bild, 50), 25, 25, 300, 300, null);
        } else {
            g2d.setColor(cGray);
            g2d.fillRoundRect(25, 25, 300, 300, 50, 50);
        }
        //DRAW BILD TEXT MIT HINTERGRUND
        String bildText = (vorname + " " + nachname + " " + ((!tgname.isEmpty()) ? "@" : "") + tgname).strip();
        g2d.setFont(scaleFont(bildText, bildSize - (bildTextPadding * 2), bildTextSize, g2d));
        g2d.setColor(cGray);
        g2d.fillRoundRect(padding + bildTextPadding-bildTextBGMargin,height - padding - bildTextPadding-g2d.getFontMetrics().getAscent()-bildTextBGMargin,g2d.getFontMetrics().stringWidth(bildText)+(bildTextBGMargin*2),g2d.getFontMetrics().getAscent()+g2d.getFontMetrics().getDescent()+(bildTextBGMargin*2),5,5);
        g2d.setColor(cText);
        g2d.drawString(bildText, padding + bildTextPadding, height - padding - bildTextPadding);
        //DRAW TEXTS
        int linepos = padding;

        linepos += drawStatsText(g2d, username
                , cText, infoSize, (padding * 2) + bildSize, usernameTextSize, linepos);

        drawStatsText(g2d, "Level " + level + " - " + titel
                , cText, infoSize, (padding * 2) + bildSize, levelTextSize, linepos);

        linepos = padding + (bildSize / 3);

        linepos += drawStatsText(g2d, punkte + " Punkt"+(punkte!=1?"e":"")
                , cText, infoSize, (padding * 2) + bildSize, punkteTextSize, linepos);

        drawStatsText(g2d, votes + " Karma"
                , cText, infoSize, (padding * 2) + bildSize, karmaTextSize, linepos);

        linepos = padding + ((bildSize / 3) * 2);

        linepos += drawStatsText(g2d, "Sprache: " + sprache
                , cText, infoSize, (padding * 2) + bildSize, paketeTextSize, linepos);

        drawStatsText(g2d, sprachen.size() + " Sprachpaket"+(sprachen.size()!=1?"e":"")
                , cText, infoSize, (padding * 2) + bildSize, paketeTextSize, linepos);

        g2d.setColor(cTag);
        g2d.setFont(g2d.getFont().deriveFont(12f));
        String str="by "+getBotUsername();
        g2d.drawString(str,(width-g2d.getFontMetrics().stringWidth(str))-5,height-5);

        return image;
    }

    private BufferedImage getProfilePicture(Integer id) {
        BufferedImage bild = null;
        try {
            List<List<PhotoSize>> photos = sender.execute(new GetUserProfilePhotos().setUserId(id).setLimit(1)).getPhotos();
            if (!photos.isEmpty()) {
                List<PhotoSize> currentPhoto = photos.get(0);
                PhotoSize img = currentPhoto.get(currentPhoto.size() - 1);
                File file = sender.downloadFile(getFilePath(img));
                bild = ImageIO.read(file);
            }
        } catch (TelegramApiException | IOException e) {
            e.printStackTrace();
        }
        return bild;
    }


    @SuppressWarnings("DuplicatedCode")
    private BufferedImage generateJustThingsImage(String text, String name) throws IOException {
        InputStream bgImage = getClass().getClassLoader().getResourceAsStream("images/bg" + (new Random().nextInt(4) + 1) + ".jpg");
        BufferedImage image = (bgImage == null) ? new BufferedImage(100, 100, BufferedImage.TYPE_INT_RGB) : ImageIO.read(bgImage);
        Graphics2D g2d = (Graphics2D) image.getGraphics();
        g2d.setColor(new Color(0, 0, 0, 100));
        g2d.fillRect(0, 0, image.getWidth(), image.getHeight());
        g2d.setColor(Color.WHITE);
        g2d.setFont(new Font("Comic Sans MS", Font.BOLD, 30));
        AttributedString as = new AttributedString(text);
        as.addAttribute(TextAttribute.FONT, g2d.getFont());
        AttributedCharacterIterator aci = as.getIterator();
        FontRenderContext frc = g2d.getFontRenderContext();
        LineBreakMeasurer lbm = new LineBreakMeasurer(aci, frc);
        int abstand = 40;
        float x = abstand;
        float y = abstand;
        float width = image.getWidth() - abstand - abstand;
        while (lbm.getPosition() < aci.getEndIndex()) {
            TextLayout textLayout = lbm.nextLayout(width);
            y += textLayout.getAscent();
            if (image.getHeight() - abstand - abstand > y)
                textLayout.draw(g2d, x, y);
            y += textLayout.getDescent() + textLayout.getLeading();
            x = abstand;
        }


        g2d.setFont(new Font("Comic Sans MS", Font.BOLD, 20));
        drawCenteredText(g2d, "#Just" + name + "Things", image.getHeight() - abstand / 2, image);
        return image;
    }

    public String getFilePath(PhotoSize photo) {
        Objects.requireNonNull(photo);

        if (photo.hasFilePath()) { // If the file_path is already present, we are done!
            return photo.getFilePath();
        } else { // If not, let find it
            // We create a GetFile method and set the file_id from the photo
            GetFile getFileMethod = new GetFile();
            getFileMethod.setFileId(photo.getFileId());
            try {
                // We execute the method using AbsSender::execute method.
                org.telegram.telegrambots.meta.api.objects.File file = sender.execute(getFileMethod);
                // We now have the file_path
                return file.getFilePath();
            } catch (TelegramApiException e) {
                e.printStackTrace();
            }
        }

        return null; // Just in case
    }

    private int drawStatsText(Graphics2D g2d, String text, Color cText, int width, int x, float textSize, int linepos) {
        return drawStatsText(g2d, text, cText, width, x, textSize, linepos, false);
    }

    private int drawStatsText(Graphics2D g2d, String text, Color cText, int width, int x, float textSize, int linepos, boolean useBaseline) {
        g2d.setColor(cText);
        g2d.setFont(scaleFont(text, width, textSize, g2d));
        g2d.drawString(text, x, linepos + (useBaseline ? 0 : g2d.getFontMetrics().getHeight()));
        return g2d.getFontMetrics().getHeight();
    }

    public Font scaleFont(String text, int desired_width, float fontSize, Graphics2D g) {
        Font font = g.getFont().deriveFont(fontSize);
        int width = g.getFontMetrics(font).stringWidth(text);
        float newFontSize = ((float) desired_width / (float) width) * fontSize;
        return g.getFont().deriveFont(Math.min(fontSize, newFontSize));
    }

    public static BufferedImage makeRoundedCorner(BufferedImage image, int cornerRadius) {
        int w = image.getWidth();
        int h = image.getHeight();
        BufferedImage output = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);

        Graphics2D g2 = output.createGraphics();
        g2.setComposite(AlphaComposite.Src);
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setColor(Color.WHITE);
        g2.fill(new RoundRectangle2D.Float(0, 0, w, h, cornerRadius, cornerRadius));

        g2.setComposite(AlphaComposite.SrcAtop);
        g2.drawImage(image, 0, 0, null);

        g2.dispose();

        return output;
    }

    private static void drawCenteredText(Graphics2D g2d, String text, int y, BufferedImage image) {
        g2d.drawString(text, (image.getWidth() - g2d.getFontMetrics().stringWidth(text)) / 2, y);
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