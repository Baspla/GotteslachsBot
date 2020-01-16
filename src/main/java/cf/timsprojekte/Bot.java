package cf.timsprojekte;

import cf.timsprojekte.db.*;
import cf.timsprojekte.db.Event;
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
import org.telegram.telegrambots.meta.api.objects.inlinequery.InlineQuery;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.*;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.meta.exceptions.TelegramApiRequestException;

import javax.annotation.PostConstruct;
import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.font.*;
import java.awt.image.*;
import java.io.*;
import java.text.AttributedCharacterIterator;
import java.text.AttributedString;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.List;
import java.util.concurrent.*;
import java.util.function.Predicate;

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
    private static final String EMOJI_YES = "Ja";
    private static final String EMOJI_NO = "Nein";

    //
    // VARIABLES
    //

    private final Logger logger;
    private final Runnable saveRunnable;
    private final PlaceManager placemanager;
    private final CodeManager codemanager;
    private final Runnable minecraftRunnable;
    private final Logger mcLogger;
    private List<Long> listGroups;
    private ArrayList<Long> uncheckedGroups;
    private Random random;
    private NutzerManager nutzermanager;
    private EventManager eventmanager;
    private Ausgabe ausgabe;
    private RiddleManager riddlemanager;
    private MCInfo info;

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
        mcLogger = LogManager.getLogger("mc");
        FileAppender mca = new FileAppender();
        mca.setName("McLogger");
        mca.setFile("mc.log");
        String PTR = "[%d] %m%n";
        mca.setLayout(new PatternLayout(PTR));
        mca.setThreshold(Level.INFO);
        mca.setAppend(true);
        mca.activateOptions();
        mcLogger.addAppender(mca);

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
        Map<Integer, Event> mapEvents = db.getMap("EVENTS");
        eventmanager = new EventManager(mapEvents);
        placemanager = new PlaceManager();
        Map<String, Riddle> mapRiddles = db.getMap("RIDDLES");
        riddlemanager = new RiddleManager(mapRiddles);
        ausgabe = new Ausgabe(creatorId(), listGroups, silent, new Locale("de", "DE"), sender);
        Map<String, String> mapCodes = db.getMap("CODES");
        codemanager = new CodeManager(mapCodes);

        saveRunnable = () -> {
            nutzermanager.saveNutzer();
            eventmanager.saveEvents();
            placemanager.savePlace();
            db.commit();
        };

        minecraftRunnable = () -> {
            MCInfo oldInfo = info;
            info = MCInfo.request("89.163.187.158", 25565);
            if (oldInfo != null) {
                ArrayList<MCPlayer> leaves = checkLeaves(oldInfo.getSamples(), info.getSamples());
                ArrayList<MCPlayer> joins = checkJoins(oldInfo.getSamples(), info.getSamples());
                System.out.println("Refresh");
                if (!leaves.isEmpty() || !joins.isEmpty()) {
                    for (MCPlayer player : joins) {
                        mcLogger.info(player.getName()+" ist beigetreten");
                    }
                    for (MCPlayer player : leaves) {
                        mcLogger.info(player.getName()+" ist gegangen");
                    }
                }
            }
        };

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            logger.info("Shutdown Hook triggered");
            saveRunnable.run();
            try {
                db.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }));

        ScheduledExecutorService minecraftTask = Executors.newSingleThreadScheduledExecutor();
        minecraftRunnable.run();
        minecraftTask.scheduleAtFixedRate(minecraftRunnable, 0, 30, TimeUnit.SECONDS);
        ScheduledExecutorService backupTask = Executors.newSingleThreadScheduledExecutor();
        backupTask.scheduleAtFixedRate(saveRunnable, 10, 10, TimeUnit.MINUTES);
    }

    private ArrayList<MCPlayer> checkJoins(ArrayList<MCPlayer> old, ArrayList<MCPlayer> now) {
        ArrayList<MCPlayer> copy = new ArrayList<>(now);
        copy.removeAll(old);
        return copy;
    }

    private ArrayList<MCPlayer> checkLeaves(ArrayList<MCPlayer> old, ArrayList<MCPlayer> now) {
        ArrayList<MCPlayer> copy = new ArrayList<>(old);
        copy.removeAll(now);
        return copy;
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

    //
    // COMMANDS
    //

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
                    if (info.isOnline())
                        msg = "<b>Minecraft " + info.getVersion() + "</b>\n" + (info.getModded() ? "(modded)\n" : "") + "<i>" + info.getMotd() + "</i>\n(" + info.getCurrentPlayers() + "/" + info.getMaximumPlayers() + ") " + info.getLatency() + "ms\n" + info.getSamplesFormatted();
                    else if (info.hasError()) {
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
                        ausgabe.sendTemp(ctx.chatId(), nutzer.getLocale(), "code.moreArgs");
                        return;
                    }
                    StringBuilder text = new StringBuilder();
                    for (int i = 1; i < ctx.arguments().length; i++) {
                        text.append((i == 1) ? "" : " ").append(ctx.arguments()[i]);
                    }
                    if (!codemanager.contains(ctx.firstArg())) {
                        if (!ctx.firstArg().matches("\\d+(-\\d+)?")) {
                            ausgabe.sendTemp(ctx.chatId(), nutzer.getLocale(), "code.invalid", ctx.firstArg());
                        } else {
                            ausgabe.send(ctx.chatId(), nutzer.getLocale(), "code.set", ctx.firstArg(), text.toString());
                            codemanager.add(ctx.firstArg(), text.toString());
                        }
                    } else {
                        ausgabe.sendTemp(ctx.chatId(), nutzer.getLocale(), "code.taken", ctx.firstArg());
                    }
                })
                .build();
    }

    public Ability cmdCodes() {
        return Ability.builder()
                .name("codes")
                .privacy(PUBLIC)
                .locality(ALL)
                .input(0)
                .action(ctx -> {
                    Nutzer nutzer = nutzermanager.getNutzer(ctx.user());
                    ausgabe.sendRaw(ctx.chatId(), codemanager.list());
                })
                .build();
    }

    public Ability cmdRemoveCode() {
        return Ability.builder()
                .name("rmcode")
                .privacy(ADMIN)
                .locality(ALL)
                .action(ctx -> {
                    Nutzer nutzer = nutzermanager.getNutzer(ctx.user());
                    if (ctx.arguments().length < 1) {
                        ausgabe.sendTemp(ctx.chatId(), nutzer.getLocale(), "code.moreArgs");
                        return;
                    }
                    if (codemanager.contains(ctx.firstArg())) {
                        ausgabe.sendTemp(ctx.chatId(), nutzer.getLocale(), "code.removed", ctx.firstArg());
                        codemanager.remove(ctx.firstArg());
                    } else {
                        ausgabe.sendTemp(ctx.chatId(), nutzer.getLocale(), "code.nonexistent", ctx.firstArg());
                    }
                })
                .build();
    }

    @SuppressWarnings({"unused", "unchecked"})
    public Ability cmdSet() {
        return Ability.builder()
                .name("set")
                .info("Nutzer-Stats")
                .flag(update -> nutzermanager.hasNutzer(AbilityUtils.getUser(update).getId()))
                .privacy(ADMIN)
                .locality(GROUP)
                .input(2)
                .action(ctx -> {
                    if (ctx.update().hasMessage()) {
                        if (ctx.update().getMessage().isReply()) {
                            Nutzer replyTo = nutzermanager.getNutzer(ctx.update().getMessage().getReplyToMessage().getFrom().getId());
                            int pts, votes;
                            try {
                                pts = Integer.parseInt(ctx.firstArg());
                                votes = Integer.parseInt(ctx.secondArg());
                            } catch (NumberFormatException e) {
                                return;
                            }
                            replyTo.setPoints(pts);
                            replyTo.setVotes(votes);
                            ausgabe.sendRaw(ctx.chatId(), replyTo.getUsername() + " auf " + pts + " Punkte und " + votes + " Ehre gesetzt.");
                        }
                    }
                    Nutzer nutzer = nutzermanager.getNutzer(ctx.user());
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
                    saveRunnable.run();
                    db.commit();
                    r.ifPresent(message -> ausgabe.edit(message.getChatId(), message.getMessageId(), nutzermanager.getNutzer(ctx.user()).getLocale(), "system.saved"));
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
                    eventmanager.loadEvents();
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
                    ausgabe.sendKeyboard(ctx.chatId(), getMainKeyboard(nutzer.getLocale()), nutzer.getLocale(), "canceled");
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
                    logger.debug("Top Points abgefragt");
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
                    logger.debug("r/place");
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
                .input(1)
                .action(ctx -> {
                    logger.debug("Admin Menu geoeffnet");
                    int points;
                    try {
                        points = Integer.valueOf(ctx.firstArg());
                    } catch (NumberFormatException e) {
                        return;
                    }
                    Nutzer nutzer = nutzermanager.getNutzer(ctx.user());
                    int levelVorher = nutzer.getLevel();
                    nutzer.addPoints(points);
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
                    } else if (nutzer.getState().equals(State.EventName)) {
                        onReplyEventName(message, nutzer);
                    } else if (nutzer.getState().equals(State.EventDesc)) {
                        onReplyEventDesc(message, nutzer);
                    } else if (nutzer.getState().equals(State.EventPts)) {
                        onReplyEventPoints(message, nutzer);
                    } else if (nutzer.getState().equals(State.Scan)) {
                        onReplyEventQR(message, nutzer);
                    }
                }
            } catch (TelegramApiException e) {
                e.printStackTrace();
            }
        }, Flag.MESSAGE, Flag.REPLY, AbilityUtils::isUserMessage, isAbility().negate());
    }

    private void onReplyEventQR(Message message, Nutzer nutzer) {
        if (message.hasPhoto()) {
            ausgabe.sendKeyboard(message.getChatId(), getMainKeyboard(nutzer.getLocale()), nutzer.getLocale(), "user.event.scanned");
            nutzer.setState(State.Default);
        }
    }

    private void onReplyEventPoints(Message message, Nutzer nutzer) {
        int i;
        try {
            i = Integer.valueOf(message.getText());
        } catch (NumberFormatException e) {
            ausgabe.send(message.getChatId(), nutzer.getLocale(), "error.int");
            return;
        }
        nutzer.setState(State.Default);
        Event event = eventmanager.createEvent(nutzer.getVar("eventName"), nutzer.getVar("eventDesc"), i, nutzer.getUserId());
        ausgabe.sendOwnerEventCheck(nutzer, event);
        ausgabe.sendKeyboard(message.getChatId(), getMainKeyboard(nutzer.getLocale()), nutzer.getLocale(), "user.event.request.sent");
    }

    private void onReplyEventDesc(Message message, Nutzer nutzer) {
        nutzer.setState(State.EventPts);
        nutzer.setVar("eventDesc", message.getText());
        ausgabe.sendKeyboard(message.getChatId(), new ForceReplyKeyboard(), nutzer.getLocale(), "user.event.request.pts");
    }

    private void onReplyEventName(Message message, Nutzer nutzer) {
        nutzer.setState(State.EventDesc);
        nutzer.setVar("eventName", message.getText());
        ausgabe.sendKeyboard(message.getChatId(), new ForceReplyKeyboard(), nutzer.getLocale(), "user.event.request.desc");
    }

    private void onReplyUsernameChange(Message message, Nutzer nutzer) {
        if (isUsernameValid(message.getText())) {
            nutzer.setUsername(message.getText());
            ausgabe.sendKeyboard(message.getChatId(), getMainKeyboard(nutzer.getLocale()), nutzer.getLocale(), "user.namechange.changed", nutzer.getUsername());
            nutzer.setState(State.Default);
        } else {
            ausgabe.sendKeyboard(message.getChatId(), getMainKeyboard(nutzer.getLocale()), nutzer.getLocale(), "user.namechange.unchanged");
        }
    }

    @SuppressWarnings("unused")
    public Reply onUserMessage() {
        return Reply.of(update -> {
            Message message = update.getMessage();
            Nutzer nutzer = nutzermanager.getNutzer(message.getFrom());
            String msg = update.getMessage().getText();
            if (msg.equals(Ausgabe.format(nutzer.getLocale(), "user.namechange.button"))) {
                onUserMessageNamechange(message, nutzer);
            } else if (msg.equals(Ausgabe.format(nutzer.getLocale(), "user.shop.button"))) {
                onUserMessageShop(message, nutzer);
            } else if (msg.equals(Ausgabe.format(nutzer.getLocale(), "user.event.button"))) {
                onUserMessageEvent(message, nutzer);
            } else if (msg.equals(Ausgabe.format(nutzer.getLocale(), "user.language.button"))) {
                onUserMessageLanguage(message, nutzer);
            } else if (msg.equals(Ausgabe.format(nutzer.getLocale(), "user.riddle.button"))) {
                onUserMessageRiddle(message, nutzer);
            } else {
                onUserMessageMain(message, nutzer);
            }
        }, Flag.MESSAGE, Flag.REPLY.negate(), AbilityUtils::isUserMessage, isAbility().negate());
    }

    private void onUserMessageMain(Message message, Nutzer nutzer) {
        ausgabe.sendKeyboard(message.getChatId(), getMainKeyboard(nutzer.getLocale()), nutzer.getLocale(), "user.main");
    }

    private void onUserMessageLanguage(Message message, Nutzer nutzer) {
        InlineKeyboardMarkup keyboard = getLanguageKeyboard(nutzer, 0);
        ausgabe.sendKeyboard(message.getChatId(), keyboard, nutzer.getLocale(), "user.language");
    }

    private void onUserMessageEvent(Message message, Nutzer nutzer) {
        InlineKeyboardMarkup keyboard = InlineKeyboardFactory.build()
                .addRow(InlineKeyboardFactory.button(Ausgabe.format(nutzer.getLocale(), "user.event.scan.button"), "scan"))
                .addRow(InlineKeyboardFactory.button(Ausgabe.format(nutzer.getLocale(), "user.event.request.button"), "registerEvent"))
                .addRow(InlineKeyboardFactory.button(Ausgabe.format(nutzer.getLocale(), "user.event.manage.button"), "listEvents_0"))
                .toMarkup();
        ausgabe.sendKeyboard(message.getChatId(), keyboard, nutzer.getLocale(), "user.event");
    }

    private void onUserMessageShop(Message message, Nutzer nutzer) {
        InlineKeyboardMarkup keyboard = InlineKeyboardFactory.build().addRow(InlineKeyboardFactory.button(Ausgabe.format(nutzer.getLocale(), "user.shop.entry", Ausgabe.format(nutzer.getLocale(), ShopItem.LanguageBox.title()), ShopItem.LanguageBox.price()), "shop_" + ShopItem.LanguageBox.data())).toMarkup();
        ausgabe.sendKeyboard(message.getChatId(), keyboard, nutzer.getLocale(), "user.shop", nutzer.getPoints());
    }

    private void onUserMessageNamechange(Message message, Nutzer nutzer) {
        nutzer.setState(State.Name);
        ausgabe.sendKeyboard(message.getChatId(), new ForceReplyKeyboard(), nutzer.getLocale(), "user.namechange");
    }

    private void onUserMessageRiddle(Message message, Nutzer nutzer) {
        InlineKeyboardMarkup keyboard = InlineKeyboardFactory.build()
                .addRow(InlineKeyboardFactory.button(Ausgabe.format(nutzer.getLocale(), "user.riddle.request.button"), "registerRiddle"))
                .addRow(InlineKeyboardFactory.button(Ausgabe.format(nutzer.getLocale(), "user.riddle.manage.button"), "listRiddles_0"))
                .toMarkup();
        ausgabe.sendKeyboard(message.getChatId(), keyboard, nutzer.getLocale(), "user.riddle");
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
            logger.debug("Callback von " + nutzer.getUsername() + " mit " + data);
            if (parts.length > 0) {
                switch (parts[0]) {
                    case "none":
                        ausgabe.answerCallback(query.getId());
                        break;
                    case "cancel_shop":
                        onCallbackCancelShop(query, nutzer, message);
                        break;
                    case "scan":
                        onCallbackEventScan(query, nutzer, message);
                        break;
                    case "registerEvent":
                        onCallbackRequestEvent(query, nutzer, message);
                        break;
                    case "registerRiddle":
                        onCallbackRequestRiddle(query, nutzer, message);
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
                        case "riddleEdit":
                            onCallbackEditRiddle(query, nutzer, message, parts[1]);
                            break;
                        case "eventEdit":
                            onCallbackEditEvent(query, nutzer, message, parts[1]);
                            break;
                        case "eventEditQR":
                            onCallbackEditEventQR(query, nutzer, message, parts[1]);
                            break;
                        case "eventEditUsers":
                            onCallbackEditEventUsers(query, nutzer, message, parts[1]);
                            break;
                        case "eventEditShowQR":
                            onCallbackEditEventShowQR(query, nutzer, message, parts[1]);
                            break;
                        case "eventEditFinished":
                            onCallbackEditEventFinished(query, nutzer, message, parts[1]);
                            break;
                        case "eventEditAnnounce":
                            onCallbackEditEventAnnounce(query, nutzer, message, parts[1]);
                            break;
                        case "eventEditGroup":
                            onCallbackEditEventGroup(query, nutzer, message, parts[1]);
                            break;
                        case "listLangs":
                            onCallbackLanguagesPage(query, nutzer, message, parts[1]);
                            break;
                        case "listEvents":
                            onCallbackEventsPage(query, nutzer, message, parts[1]);
                            break;
                        case "listRiddles":
                            onCallbackRiddlesPage(query, nutzer, message, parts[1]);
                            break;
                    }
                    if (parts.length > 2) {
                        switch (parts[0]) {
                            case "admGroup":
                                onCallbackAdminGroup(query, message, parts[1], parts[2]);
                                break;
                            case "admEvent":
                                onCallbackAdminEvent(query, message, parts[1], parts[2]);
                                break;
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
        System.out.println(t);
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


    private void onCallbackEditEventShowQR(CallbackQuery query, Nutzer nutzer, Message message, String part) {
        ausgabe.answerCallback(query.getId());
        Optional<Event> opt = eventmanager.getEvent(Integer.valueOf(part));
        if (opt.isEmpty()) return;
        Event event = opt.get();
        ausgabe.removeKeyboard(message.getChatId(), message.getMessageId());
    }

    private void onCallbackEditEventAnnounce(CallbackQuery query, Nutzer nutzer, Message message, String part) {
        ausgabe.answerCallback(query.getId());
        Optional<Event> opt = eventmanager.getEvent(Integer.valueOf(part));
        if (opt.isEmpty()) return;
        Event event = opt.get();
        ausgabe.removeKeyboard(message.getChatId(), message.getMessageId());
    }

    private void onCallbackRequestEvent(CallbackQuery query, Nutzer nutzer, Message message) {
        ausgabe.answerCallback(query.getId());
        nutzer.setState(State.EventName);
        ausgabe.removeKeyboard(message.getChatId(), message.getMessageId());
        ausgabe.sendKeyboard(message.getChatId(), new ForceReplyKeyboard(), nutzer.getLocale(), "user.event.request.name");
    }

    private void onCallbackRequestRiddle(CallbackQuery query, Nutzer nutzer, Message message) {
        //TODO
    }

    private void onCallbackEventScan(CallbackQuery query, Nutzer nutzer, Message message) {
        ausgabe.answerCallback(query.getId());
        nutzer.setState(State.Scan);
        ausgabe.removeKeyboard(message.getChatId(), message.getMessageId());
        ausgabe.sendKeyboard(message.getChatId(), new ForceReplyKeyboard(), nutzer.getLocale(), "user.event.scan");
    }

    private void onCallbackEventsPage(CallbackQuery query, Nutzer nutzer, Message message, String part) {
        ausgabe.answerCallback(query.getId());
        int seite = Integer.parseInt(part);
        ausgabe.editKeyboard(message.getChatId(), message.getMessageId(), getEventsKeyboard(nutzer, seite), nutzer.getLocale(), "user.event.list");
    }

    private void onCallbackRiddlesPage(CallbackQuery query, Nutzer nutzer, Message message, String part) {
        ausgabe.answerCallback(query.getId());
        int seite = Integer.parseInt(part);
        ausgabe.editKeyboard(message.getChatId(), message.getMessageId(), getRiddlesKeyboard(nutzer, seite), nutzer.getLocale(), "user.riddle.list");
    }

    private void onCallbackLanguagesPage(CallbackQuery query, Nutzer nutzer, Message message, String part) {

        ausgabe.answerCallback(query.getId());
        int seite = Integer.parseInt(part);
        ausgabe.editKeyboard(message.getChatId(), message.getMessageId(), getLanguageKeyboard(nutzer, seite), nutzer.getLocale(), "user.language");
    }

    private void onCallbackEditEventGroup(CallbackQuery query, Nutzer nutzer, Message message, String part) {
        ausgabe.answerCallback(query.getId());
        Optional<Event> opt = eventmanager.getEvent(Integer.valueOf(part));
        if (opt.isEmpty()) return;
        Event event = opt.get();
        event.setGroupJoin(!event.isGroupJoin());
        ausgabe.editKeyboard(message.getChatId(), message.getMessageId(), getEventManageKeyboard(event, nutzer.getLocale()));
    }

    private void onCallbackEditEventFinished(CallbackQuery query, Nutzer nutzer, Message message, String part) {
        ausgabe.answerCallback(query.getId());
        Optional<Event> opt = eventmanager.getEvent(Integer.valueOf(part));
        if (opt.isEmpty()) return;
        Event event = opt.get();
        ausgabe.removeKeyboard(message.getChatId(), message.getMessageId());

    }

    private void onCallbackEditEventUsers(CallbackQuery query, Nutzer nutzer, Message message, String part) {
        ausgabe.answerCallback(query.getId());
        Optional<Event> opt = eventmanager.getEvent(Integer.valueOf(part));
        if (opt.isEmpty()) return;
        Event event = opt.get();
        ausgabe.removeKeyboard(message.getChatId(), message.getMessageId());

    }

    private void onCallbackEditEventQR(CallbackQuery query, Nutzer nutzer, Message message, String part) {
        ausgabe.answerCallback(query.getId());
        Optional<Event> opt = eventmanager.getEvent(Integer.valueOf(part));
        if (opt.isEmpty()) return;
        Event event = opt.get();
        System.out.println(event.isQr());
        event.setQr(!event.isQr());
        System.out.println(event.isQr());
        ausgabe.editKeyboard(message.getChatId(), message.getMessageId(), getEventManageKeyboard(event, nutzer.getLocale()));
    }

    private void onCallbackEditEvent(CallbackQuery query, Nutzer nutzer, Message message, String part) {
        ausgabe.answerCallback(query.getId());
        Optional<Event> opt = eventmanager.getEvent(Integer.valueOf(part));
        if (opt.isEmpty()) return;
        Event event = opt.get();
        ausgabe.editKeyboard(message.getChatId(), message.getMessageId(), getEventManageKeyboard(event, nutzer.getLocale()), nutzer.getLocale(), "user.event.manage", event.isAccepted());
    }


    private void onCallbackEditRiddle(CallbackQuery query, Nutzer nutzer, Message message, String part) {
        //TODO
        ausgabe.answerCallback(query.getId());
        Optional<Riddle> opt = riddlemanager.getRiddle(part);
        if (opt.isEmpty()) return;
        Riddle riddle = opt.get();

        //TODO
        //ausgabe.editKeyboard(message.getChatId(), message.getMessageId(), getRiddleManageKeyboard(riddle, nutzer.getLocale()), nutzer.getLocale(), "user.riddle.manage", riddle.isAccepted());
    }

    private void onCallbackLanguage(CallbackQuery query, Nutzer nutzer, Message message, String part) {
        ausgabe.answerCallback(query.getId());
        Optional<Language> lang = nutzer.getLanguages().stream().filter(language -> language.name().equals(part)).findAny();
        if (lang.isEmpty()) return;
        String[] splits = lang.get().data().split("_");
        nutzer.setLocale((splits.length > 0) ? splits[0] : null, (splits.length > 1) ? splits[1] : null, (splits.length > 2) ? splits[2] : null);
        ausgabe.editKeyboard(message.getChatId(), message.getMessageId(), null, nutzer.getLocale(), "user.language.set", lang.get().title());
    }

    private void onCallbackCancelShop(CallbackQuery query, Nutzer nutzer, Message message) {
        ausgabe.answerCallback(query.getId());
        ausgabe.removeKeyboard(message.getChatId(), message.getMessageId());
        ausgabe.send(message.getChatId(), nutzer.getLocale(), "user.shop.canceled");
    }

    private void onCallbackShop(CallbackQuery query, Nutzer nutzer, Message message, String[] parts) {
        ausgabe.answerCallback(query.getId());
        Arrays.stream(ShopItem.values()).filter(item -> parts[1].equals(item.data())).findAny().ifPresent(shopItem -> {
            if (shopItem.price() <= nutzer.getPoints()) {
                if (parts.length > 2) {
                    if (parts[2].equals("buy")) {
                        ausgabe.removeKeyboard(message.getChatId(), message.getMessageId());
                        nutzer.removePoints(shopItem.price());
                        ausgabe.send(message.getChatId(), nutzer.getLocale(), "user.shop.bought", Ausgabe.format(nutzer.getLocale(), shopItem.title()), shopItem.price(), nutzer.getPoints());
                        shopItem.onBuy(nutzer, ausgabe, message.getChatId());
                    }
                } else {
                    ausgabe.editKeyboard(message.getChatId(), message.getMessageId(), InlineKeyboardFactory.build()
                                    .addRow(InlineKeyboardFactory.button(Ausgabe.format(nutzer.getLocale(), "user.shop.buy"), "shop_" + shopItem.data() + "_buy"))
                                    .addRow(InlineKeyboardFactory.button(Ausgabe.format(nutzer.getLocale(), "user.shop.cancel"), "cancel_shop"))
                                    .toMarkup(),
                            nutzer.getLocale(), "user.shop.item", Ausgabe.format(nutzer.getLocale(), shopItem.title()), shopItem.price(), nutzer.getPoints());
                }
            } else {
                ausgabe.edit(message.getChatId(), message.getMessageId(), nutzer.getLocale(), "user.shop.nomoney", Ausgabe.format(nutzer.getLocale(), shopItem.title()), shopItem.price(), nutzer.getPoints());
            }
        });
    }

    private void onCallbackAdminEvent(CallbackQuery query, Message message, String part1, String part2) {
        ausgabe.answerCallback(query.getId());
        if (!query.getFrom().getId().equals(creatorId())) return;
        int eventId = Integer.parseInt(part2);
        Optional<Event> optEvent = eventmanager.getEvent(eventId);
        if (optEvent.isEmpty()) return;
        Event event = optEvent.get();
        if (part1.equals("+")) {
            ausgabe.removeMessage(message.getChatId(), message.getMessageId());
            event.setAccepted(true);
            ausgabe.send((long) event.getCreator(), nutzermanager.getNutzer(event.getCreator()).getLocale(), "user.event.accepted");
        } else if (part1.equals("-")) {
            ausgabe.removeMessage(message.getChatId(), message.getMessageId());
            eventmanager.removeEvent(eventId);
            ausgabe.send((long) event.getCreator(), nutzermanager.getNutzer(event.getCreator()).getLocale(), "user.event.declined");
        }
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
            ausgabe.sendToGroup(chatId, "group.accepted");
        } else if (part1.equals("-")) {
            ausgabe.sendToGroup(chatId, "group.declined");
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
            factory.addRow(InlineKeyboardFactory.button(Ausgabe.format(nutzer.getLocale(), "user.language.none"), "none"));
        return factory.toMarkup();
    }

    @SuppressWarnings("Duplicates")
    private InlineKeyboardMarkup getEventsKeyboard(Nutzer nutzer, int seite) {
        var ref = new Object() {
            boolean lineBreak = false;
            ArrayList<InlineKeyboardButton> row = new ArrayList<>();
        };
        List<Event> userEvents = eventmanager.getEventListe(nutzer.getUserId());
        InlineKeyboardFactory factory = InlineKeyboardFactory.build();
        userEvents.stream().sorted().limit(6 * (1 + seite)).skip(6 * seite).forEachOrdered(event -> {
            ref.row.add(InlineKeyboardFactory.button(event.getName(), "eventEdit_" + event.getId()));
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
            ref.row.add(InlineKeyboardFactory.button("<<", "listEvents_" + (seite - 1)));
        if (userEvents.size() > 6 * (1 + seite))
            ref.row.add(InlineKeyboardFactory.button(">>", "listEvents_" + (seite + 1)));
        if (!ref.row.isEmpty())
            factory.addRow(ref.row);
        if (!factory.hasRows())
            factory.addRow(InlineKeyboardFactory.button(Ausgabe.format(nutzer.getLocale(), "user.event.list.none"), "none"));
        return factory.toMarkup();
    }

    @SuppressWarnings("Duplicates")
    private InlineKeyboardMarkup getRiddlesKeyboard(Nutzer nutzer, int seite) {
        var ref = new Object() {
            boolean lineBreak = false;
            ArrayList<InlineKeyboardButton> row = new ArrayList<>();
        };
        List<Riddle> userRiddles = riddlemanager.getRiddleListe(nutzer.getUserId());
        InlineKeyboardFactory factory = InlineKeyboardFactory.build();
        userRiddles.stream().sorted().limit(6 * (1 + seite)).skip(6 * seite).forEachOrdered(riddle -> {
            ref.row.add(InlineKeyboardFactory.button(riddle.getTitel(), "riddleEdit_" + riddle.getCode()));
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
            ref.row.add(InlineKeyboardFactory.button("<<", "listRiddles_" + (seite - 1)));
        if (userRiddles.size() > 6 * (1 + seite))
            ref.row.add(InlineKeyboardFactory.button(">>", "listRiddles_" + (seite + 1)));
        if (!ref.row.isEmpty())
            factory.addRow(ref.row);
        if (!factory.hasRows())
            factory.addRow(InlineKeyboardFactory.button(Ausgabe.format(nutzer.getLocale(), "user.riddle.list.none"), "none"));
        return factory.toMarkup();
    }

    private InlineKeyboardMarkup getEventManageKeyboard(Event event, Locale locale) {
        return InlineKeyboardFactory.build()
                .addRow(InlineKeyboardFactory.button(Ausgabe.format(locale, "user.event.manage.qr.button", event.isQr() ? EMOJI_YES : EMOJI_NO), "eventEditQR_" + event.getId()), InlineKeyboardFactory.button(Ausgabe.format(locale, "user.event.manage.group.button", event.isGroupJoin() ? EMOJI_YES : EMOJI_NO), "eventEditGroup_" + event.getId()))
                .addRow(InlineKeyboardFactory.button(Ausgabe.format(locale, "user.event.manage.showQR.button"), "eventEditShowQR_" + event.getId()), InlineKeyboardFactory.button(Ausgabe.format(locale, "user.event.manage.announce.button"), "eventEditAnnounce_" + event.getId()))
                .addRow(InlineKeyboardFactory.button(Ausgabe.format(locale, "user.event.manage.users.button"), "eventEditUsers_" + event.getId()))
                .addRow(InlineKeyboardFactory.button(Ausgabe.format(locale, "user.event.manage.finish.button"), "eventEditFinished_" + event.getId()))
                .toMarkup();
    }


    private ReplyKeyboard getMainKeyboard(Locale locale) {
        return ReplyKeyboardFactory.build()
                .addRow(ReplyKeyboardFactory.button(Ausgabe.format(locale, "user.namechange.button")), ReplyKeyboardFactory.button(Ausgabe.format(locale, "user.riddle.button")))
                .addRow(ReplyKeyboardFactory.button(Ausgabe.format(locale, "user.event.button")), ReplyKeyboardFactory.button(Ausgabe.format(locale, "user.language.button")))
                .addRow(ReplyKeyboardFactory.button(Ausgabe.format(locale, "user.shop.button")))
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
                ausgabe.sendToAllGroups("reward.jackpot", reward, nutzer.getLinkedUsername());
            }
            int levelVorher = nutzer.getLevel();
            nutzer.addPoints(reward);
            checkLevelUp(nutzer, levelVorher);
            nutzer.setCooldownReward(r.nextInt(NACHRICHT_COOLDOWN_MAX) + NACHRICHT_COOLDOWN_MIN, ChronoUnit.MINUTES);
        }
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

    private boolean checkGroup(Chat groupChat) {
        if (!listGroups.contains(groupChat.getId())) {
            if (uncheckedGroups.contains(groupChat.getId())) return true;
            uncheckedGroups.add(groupChat.getId());
            ausgabe.sendOwnerGroupCheck(groupChat.getId(), groupChat.getTitle());
            ausgabe.sendToGroup(groupChat.getId(), "group.requested");
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