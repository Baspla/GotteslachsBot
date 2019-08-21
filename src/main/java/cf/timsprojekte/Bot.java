package cf.timsprojekte;

import cf.timsprojekte.dialoge.admin.AdminMainDialog;
import cf.timsprojekte.dialoge.casino.CasinoDialog;
import cf.timsprojekte.dialoge.settings.SettingsDialog;
import cf.timsprojekte.dialoge.shop.ShopDialog;
import cf.timsprojekte.verwaltung.*;
import cf.timsprojekte.verwaltung.immutable.*;
import org.apache.log4j.*;
import org.telegram.abilitybots.api.bot.AbilityBot;
import org.telegram.abilitybots.api.objects.*;
import org.telegram.abilitybots.api.sender.SilentSender;
import org.telegram.abilitybots.api.util.AbilityUtils;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.send.SendPhoto;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.DeleteMessage;
import org.telegram.telegrambots.meta.api.objects.*;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.*;
import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.*;
import java.util.concurrent.TimeUnit;

import static org.telegram.abilitybots.api.objects.Locality.*;
import static org.telegram.abilitybots.api.objects.Privacy.ADMIN;
import static org.telegram.abilitybots.api.objects.Privacy.PUBLIC;

public class Bot extends AbilityBot {

    private static final int START_PUNKTE = 0;
    private static final int NACHRICHT_PUNKTE_MIN = 1;
    private static final int NACHRICHT_PUNKTE_MAX = 12;
    private static final int NACHRICHT_COOLDOWN_MIN = 1;
    private static final int NACHRICHT_COOLDOWN_MAX = 4;
    private static final int HONOR_POINTS = 10;
    private static final int JACKPOT = 7000;
    private static final int JACKPOT_MULTIPLYER = 10;
    private static final long LIKE_COOLDOWN = 3;
    private final Gruppenverwaltung gruppenverwaltung;
    public final Nutzerverwaltung nutzerverwaltung;
    public final Abzeichenverwaltung abzeichenverwaltung;
    private final Levelverwaltung levelverwaltung;
    private final Dialogverwaltung dialogverwaltung;
    private final Logger logger;
    public final Storeverwaltung storeverwaltung;
    public SilentSender silentPublic;
    private Statistikverwaltung statistikverwaltung;

    public int creatorId() {
        return 67025299;
    }

    @SuppressWarnings("unused")
    public Bot(String token, String name) {
        super(token, name);
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

        silentPublic = silent;


        dialogverwaltung = new Dialogverwaltung(this);
        gruppenverwaltung = new Gruppenverwaltung(db);
        abzeichenverwaltung = new Abzeichenverwaltung(db);
        nutzerverwaltung = new Nutzerverwaltung(db, this);
        levelverwaltung = new Levelverwaltung();
        statistikverwaltung = new Statistikverwaltung(db);
        storeverwaltung = new Storeverwaltung(db);
    }


    @SuppressWarnings("unused")
    public Ability cmdStats() {
        return Ability.builder()
                .name("stats")
                .privacy(PUBLIC)
                .locality(USER)
                .input(0)
                .action(ctx -> {
                    Benutzer benutzer = nutzerverwaltung.getBenutzer(ctx.user().getId());
                    if (benutzer == null) {
                        logger.debug("Unbekannter Nutzer wollte Stats abfragen");
                        silent.send("Du bist nicht im System.", ctx.chatId());
                    } else {
                        logger.debug(benutzer.getNutzername() + " hat seine Stats abgefragt.");
                        String abzeichen = abzeichenverwaltung.convertToString(nutzerverwaltung.getBenutzer(ctx.user().getId()).getAbzeichen());
                        silent.execute(new SendMessage(ctx.chatId(), "<b>Deine Stats:</b>\n" + toBenutzerStatsString(benutzer) + "\n<b>Deine Abzeichen:</b>\n" + abzeichen).setParseMode("HTML"));
                    }
                })
                .build();
    }

    @SuppressWarnings("unused")
    public Ability cmdStore() {
        return Ability.builder()
                .name("store")
                .privacy(PUBLIC)
                .locality(USER)
                .input(0)
                .action(ctx -> {
                    logger.debug("Store geöffnet");
                    dialogverwaltung.begin(new ShopDialog(), ctx.user().getId(), ctx.chatId());
                })
                .build();
    }

    @SuppressWarnings("unused")
    public Ability cmdGraph() {
        return Ability.builder()
                .name("graph")
                .privacy(PUBLIC)
                .locality(ALL)
                .input(0)
                .action(ctx -> {
                    if (ctx.arguments().length == 0) {
                        silent.send("/graph <text/sticker/photo/video/voice/week/day/honorIn/honorOut>", ctx.chatId());
                        return;
                    }
                    BufferedImage img;
                    if (ctx.firstArg().equalsIgnoreCase("text")) {
                        img = statistikverwaltung.generateTextPie();
                    } else if (ctx.firstArg().equalsIgnoreCase("sticker")) {
                        img = statistikverwaltung.generateStickerPie();
                    } else if (ctx.firstArg().equalsIgnoreCase("photo")) {
                        img = statistikverwaltung.generatePhotoPie();
                    } else if (ctx.firstArg().equalsIgnoreCase("video")) {
                        img = statistikverwaltung.generateVideoPie();
                    } else if (ctx.firstArg().equalsIgnoreCase("voice")) {
                        img = statistikverwaltung.generateVoicePie();
                    } else if (ctx.firstArg().equalsIgnoreCase("week")) {
                        img = statistikverwaltung.generateWeekBars();
                    } else if (ctx.firstArg().equalsIgnoreCase("day")) {
                        img = statistikverwaltung.generateTimeBars();
                    } else if (ctx.firstArg().equalsIgnoreCase("honorIn")) {
                        img = statistikverwaltung.generateLikeInPie();
                    } else if (ctx.firstArg().equalsIgnoreCase("honorOut")) {
                        img = statistikverwaltung.generateLikeOutPie();
                    } else {
                        return;
                    }
                    File out = new File("tg_temp.png");
                    try {
                        ImageIO.write(img, "png", out);
                        sender.sendPhoto(new SendPhoto().setChatId(ctx.chatId()).setPhoto(out));
                    } catch (IOException | TelegramApiException e) {
                        e.printStackTrace();
                    }
                })
                .build();
    }

    @SuppressWarnings("unused")
    public Ability cmdSettings() {
        return Ability.builder()
                .name("settings")
                .privacy(PUBLIC)
                .locality(USER)
                .input(0)
                .action(ctx -> {
                    logger.debug("Einstellungen geöffnet");
                    dialogverwaltung.begin(new SettingsDialog(), ctx.user().getId(), ctx.chatId());
                })
                .build();
    }

    @SuppressWarnings("unused")
    public Ability cmdGamble() {
        return Ability.builder()
                .name("gamble")
                .privacy(PUBLIC)
                .locality(USER)
                .input(0)
                .action(ctx -> {
                    logger.debug("Gamble geöffnet");
                    dialogverwaltung.begin(new CasinoDialog(), ctx.user().getId(), ctx.chatId());
                })
                .build();
    }

    @SuppressWarnings("unused")
    public Ability cmdTop() {
        return Ability.builder()
                .name("top")
                .privacy(PUBLIC)
                .locality(ALL)
                .input(0)
                .action(ctx -> {
                    logger.debug("Top abgefragt");
                    List<Benutzer> topList = nutzerverwaltung.getTopBenuzerList(10);
                    StringBuilder msg = new StringBuilder("Top-Liste:\n");
                    topList.forEach(entry -> msg.append(toBenutzerStatsString(entry)).append("\n")
                    );
                    silent.execute(new SendMessage(ctx.chatId(), msg.toString()).setParseMode("HTML"));
                })
                .build();
    }

    @SuppressWarnings("unused")
    public Ability cmdAusgabe() {
        return Ability.builder()
                .name("ausgabe")
                .privacy(ADMIN)
                .locality(GROUP)
                .input(0)
                .action(ctx -> {
                    logger.debug("Ausgabe Command");
                    Gruppe gruppe = gruppenverwaltung.getGruppe(ctx.chatId());
                    if (gruppe == null)
                        silent.send("Diese Gruppe ist nicht Whitelisted!", ctx.chatId());
                    else {
                        boolean switched = !gruppe.getIsAusgabe();
                        gruppenverwaltung.setGruppeIsAusgabe(gruppe, switched);
                        silent.send("Ausgabe in der Gruppe " + (switched ? "aktiviert." : "deaktiviert."), ctx.chatId());
                    }
                })
                .build();
    }

    @SuppressWarnings("unused")
    public Ability cmdIP() {
        return Ability.builder()
                .name("ip")
                .privacy(ADMIN)
                .locality(ALL)
                .input(0)
                .action(ctx -> {
                    logger.debug("IP Command");
                    URL whatsmyip;
                    try {
                        whatsmyip = new URL("http://checkip.amazonaws.com");
                    } catch (MalformedURLException e) {
                        e.printStackTrace();
                        return;
                    }
                    InputStream stream;
                    try {
                        stream = whatsmyip.openStream();
                    } catch (IOException e) {
                        e.printStackTrace();
                        return;
                    }
                    BufferedReader in = new BufferedReader(new InputStreamReader(stream));
                    try {
                        String ip = in.readLine();
                        silent.send("IP: " + ip, ctx.chatId());
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    try {
                        in.close();
                    } catch (IOException ex) {
                        ex.printStackTrace();
                    }
                })
                .build();
    }

    @SuppressWarnings("unused")
    public Ability cmdWhitelist() {
        return Ability.builder()
                .name("whitelist")
                .privacy(ADMIN)
                .locality(GROUP)
                .input(0)
                .action(ctx -> {
                    Gruppe gruppe = gruppenverwaltung.getGruppe(ctx.chatId());
                    if (gruppe != null) {
                        gruppenverwaltung.removeGruppe(gruppe);
                        silent.send("Von der Whitelist entfernt.", ctx.chatId());
                        logger.debug("Whitelist entfernt " + ctx.chatId());
                    } else {
                        gruppenverwaltung.createGruppe(new Gruppe(ctx.chatId(), false, 0));
                        silent.send("Zur Whitelist hinzugefügt.", ctx.chatId());
                        logger.debug("Whitelist hinzugefügt " + ctx.chatId());
                    }
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
                    dialogverwaltung.begin(new AdminMainDialog(), ctx.user().getId(), ctx.chatId());
                })
                .build();
    }

    @SuppressWarnings("unused")
    public Reply onReply() {
        return Reply.of(update -> {
            try {
                if (update.getMessage().getReplyToMessage().getFrom().equals(this.getMe())) {
                    dialogverwaltung.reply(update.getMessage());
                }
            } catch (TelegramApiException e) {
                e.printStackTrace();
            }
        }, Flag.MESSAGE, Flag.REPLY, AbilityUtils::isUserMessage);
    }


    @SuppressWarnings({"unused", "Duplicates"})
    public Reply onCallback() {
        return Reply.of(update -> {
            dialogverwaltung.onCallback(update.getCallbackQuery());
        }, Flag.CALLBACK_QUERY);
    }

    @SuppressWarnings("unused")
    public Reply onGroupMessage() {
        return Reply.of(update -> {
            Gruppe gruppe = gruppenverwaltung.getGruppe(update.getMessage().getChatId());
            if (gruppe == null) {
                silent.send("@TimMorgner /whitelist", update.getMessage().getChatId());
                logger.debug("Nicht Whitelisted " + update.getMessage().getChatId());
                return;
            }

            Statistik stat = statistikverwaltung.getStatistik(update.getMessage().getFrom().getId());
            if (update.getMessage().hasText()) {
                stat = statistikverwaltung.logText(stat);
            } else if (update.getMessage().hasSticker()) {
                stat = statistikverwaltung.logSticker(stat);
            } else if (update.getMessage().hasPhoto()) {
                stat = statistikverwaltung.logPhoto(stat);
            } else if (update.getMessage().hasVideo() || update.getMessage().hasVideoNote()) {
                stat = statistikverwaltung.logVideo(stat);
            } else if (update.getMessage().hasVoice()) {
                stat = statistikverwaltung.logVoice(stat);
            }
            Calendar cal = Calendar.getInstance();
            cal.setTime(new Date());
            switch (cal.get(Calendar.DAY_OF_WEEK)) {
                case Calendar.MONDAY:
                    stat = statistikverwaltung.logMonday(stat);
                    break;
                case Calendar.TUESDAY:
                    stat = statistikverwaltung.logTuesday(stat);
                    break;
                case Calendar.WEDNESDAY:
                    stat = statistikverwaltung.logWednesday(stat);
                    break;
                case Calendar.THURSDAY:
                    stat = statistikverwaltung.logThursday(stat);
                    break;
                case Calendar.FRIDAY:
                    stat = statistikverwaltung.logFriday(stat);
                    break;
                case Calendar.SATURDAY:
                    stat = statistikverwaltung.logSaturday(stat);
                    break;
                case Calendar.SUNDAY:
                    stat = statistikverwaltung.logSunday(stat);
                    break;
                default:
                    break;
            }
            int hour = cal.get(Calendar.HOUR_OF_DAY);
            if (hour < 5 || hour >= 21) {
                stat = statistikverwaltung.logNight(stat);
            } else if (hour < 10) {
                stat = statistikverwaltung.logMorning(stat);
            } else if (hour < 15) {
                stat = statistikverwaltung.logDay(stat);
            } else {
                stat = statistikverwaltung.logEvening(stat);
            }
            Benutzer benutzer = nutzerverwaltung.getBenutzer(update.getMessage().getFrom().getId());
            if (benutzer == null) {
                int[] abzeichen = new int[0];
                logger.debug("Created " + update.getMessage().getFrom().getId());
                benutzer = new Benutzer(update.getMessage().getFrom().getId(), START_PUNKTE, abzeichen, System.currentTimeMillis(), System.currentTimeMillis(), System.currentTimeMillis(), getFullName(update.getMessage().getFrom().getId()));
                nutzerverwaltung.createBenutzer(benutzer);
            }
            checkReward(benutzer);
            //checkLike
            if (update.getMessage().hasText() && update.getMessage().isReply()) {
                //Starts or ends with Emoji/+
                if (update.getMessage().getText().matches("^(\\u002b|\\u261d|\\ud83d\\udc46|\\ud83d\\udc4f|\\ud83d\\ude18|\\ud83d\\ude0d|\\ud83d\\udc4c|\\ud83d\\udc4d)+[\\s\\S]*") || update.getMessage().getText().matches("[\\s\\S]*(\\u002b|\\u261d|\\ud83d\\udc46|\\ud83d\\udc4f|\\ud83d\\ude18|\\ud83d\\ude0d|\\ud83d\\udc4c|\\ud83d\\udc4d)+$")) {
                    Benutzer replyTo = nutzerverwaltung.getBenutzer(update.getMessage().getReplyToMessage().getFrom().getId());
                    if (benutzer.getNextLike() <= System.currentTimeMillis()) {
                        if (replyTo != null && !replyTo.equals(benutzer)) {
                            if (benutzer.getPunkte() >= HONOR_POINTS) {
                                benutzer = nutzerverwaltung.setBenutzerNextLike(benutzer, TimeUnit.MINUTES.toMillis(LIKE_COOLDOWN));
                                benutzer = nutzerverwaltung.setBenutzerPunkte(benutzer, benutzer.getPunkte() - HONOR_POINTS);
                                replyTo = nutzerverwaltung.addBenutzerPunkte(replyTo, HONOR_POINTS);
                                statistikverwaltung.logLikeOut(statistikverwaltung.getStatistik(benutzer.getUserId()));
                                statistikverwaltung.logLikeIn(statistikverwaltung.getStatistik(replyTo.getUserId()));
                                ausgabeHTML("<a href=\"tg://user?id=" + benutzer.getUserId() + "\">" + benutzer.getNutzername() + "</a> (<code>" + benutzer.getPunkte() + "</code>) hat <a href=\"tg://user?id=" + replyTo.getUserId() + "\">" + replyTo.getNutzername() + "</a> (<code>" + replyTo.getPunkte() + "</code>) mit " + HONOR_POINTS + " Punkten geehrt.");
                            } else {
                                logger.debug("Zu wenige Punkte zum Liken");
                            }
                        }
                    }
                }
            }
        }, Flag.MESSAGE, update -> {
            Message m = update.getMessage();
            return (m.isSuperGroupMessage() || m.isGroupMessage()) && !m.isCommand() && (m.hasSticker() || m.hasAnimation() || m.hasAudio() || m.hasContact() || m.hasDocument() || m.hasLocation() || m.hasPhoto() || m.hasPoll() || m.hasText() || m.hasVideo() || m.hasVoice() || m.hasVideoNote()) && m.getPinnedMessage() == null && m.getDeleteChatPhoto() == null && m.getLeftChatMember() == null && m.getNewChatTitle() == null && m.getNewChatPhoto() == null;
        });
    }


    private void checkReward(Benutzer nutzer) {
        Benutzer benutzer = nutzer;
        long timeLeft = TimeUnit.MILLISECONDS.toSeconds(benutzer.getNextBelohnung() - System.currentTimeMillis());
        if (timeLeft <= 0) {
            int punkte = new Random(System.currentTimeMillis()).nextInt(NACHRICHT_PUNKTE_MAX + 1 - NACHRICHT_PUNKTE_MIN) + NACHRICHT_PUNKTE_MIN;
            punkte = checkJackpot(benutzer, punkte);
            logger.debug("Reward " + benutzer.getUserId() + " - " + punkte);
            statistikverwaltung.logRewards(statistikverwaltung.getStatistik(nutzer.getUserId()));
            benutzer = nutzerverwaltung.addBenutzerPunkte(benutzer, punkte);
            nutzerverwaltung.setBenutzerNextBelohnung(benutzer, System.currentTimeMillis() + TimeUnit.MINUTES.toMillis(new Random(System.currentTimeMillis()).nextInt(NACHRICHT_COOLDOWN_MAX + 1 - NACHRICHT_COOLDOWN_MIN) + NACHRICHT_COOLDOWN_MIN));
        } else {
            logger.debug("Zeit uebrig: " + timeLeft + " Sekunden");
        }
    }

    public void checkLevelUp(Benutzer benutzer, int altPkt, int neuPkt) {
        int alt = levelverwaltung.getLevel(altPkt);
        int neu = levelverwaltung.getLevel(neuPkt);
        if (alt < neu) {
            logger.debug("Level gestiegen: " + benutzer.getUserId() + " ( " + alt + " | " + neu + " )");
            if (neu - alt == 1) {
                ausgabeBild("smug.moe/smg/" + neu + ".png", benutzer.getNutzername() + " ist jetzt ein " + levelverwaltung.getTitleForLevel(neu) + "!");
            } else
                ausgabeBild("smug.moe/smg/" + neu + ".png", benutzer.getNutzername() + " ist jetzt ein " + levelverwaltung.getTitleForLevel(neu) + " und hat dabei " + (neu - alt - 1) + " Level übersprungen!");
        }
    }

    private void ausgabeBild(String bildUrl, String caption) {
        Set<Gruppe> ausgabegruppen = gruppenverwaltung.getAusgabegruppen();
        for (Gruppe g : ausgabegruppen) {
            try {
                if (g.getLastMessage() != 0)
                    silent.execute(new DeleteMessage(g.getChatId(), g.getLastMessage()));
                Message msg = sender.sendPhoto(new SendPhoto().setChatId(g.getChatId()).setPhoto(bildUrl).setCaption(caption));
                gruppenverwaltung.setGruppeLastMessage(g, msg.getMessageId());
            } catch (TelegramApiException e) {
                System.err.println(e);
            }
        }
    }

    public void ausgabe(String s) {
        Set<Gruppe> ausgabegruppen = gruppenverwaltung.getAusgabegruppen();
        for (Gruppe g : ausgabegruppen) {
            if (g.getLastMessage() != 0)
                silent.execute(new DeleteMessage(g.getChatId(), g.getLastMessage()));
            Optional<Message> msg = silent.send(s, g.getChatId());
            if (msg.isPresent())
                gruppenverwaltung.setGruppeLastMessage(g, msg.get().getMessageId());
        }
    }

    private void ausgabeHTML(String s) {
        Set<Gruppe> ausgabegruppen = gruppenverwaltung.getAusgabegruppen();
        for (Gruppe g : ausgabegruppen) {
            if (g.getLastMessage() != 0)
                silent.execute(new DeleteMessage(g.getChatId(), g.getLastMessage()));
            Optional<Message> msg = silent.execute(new SendMessage(g.getChatId(), s).setParseMode("HTML"));
            msg.ifPresent(message -> gruppenverwaltung.setGruppeLastMessage(g, message.getMessageId()));
        }
    }

    public void abzeichenVerliehen(Abzeichen abzeichen, ArrayList<Benutzer> selected) {
        String user = "";
        for (Benutzer benutzer : selected) {
            user = user + ", " + benutzer.getNutzername();
        }
        user = user.substring(2);
        ausgabe("Abzeichenverleihung - " + abzeichen.getName() + "\n" + abzeichen.getBeschreibung() + "\n\n" + abzeichen.getBelohnung() + " Punkte an die Ehrenmänner:\n" + user);
    }

    private int checkJackpot(Benutzer benutzer, int punkte) {
        if (new Random(System.currentTimeMillis()).nextInt(JACKPOT) == 0) {
            punkte = punkte * JACKPOT_MULTIPLYER;
            ausgabeBild("smug.moe/smg/" + (new Random().nextInt(58) + 1) + ".png", benutzer.getNutzername() + " hat den JACKPOT gezogen und " + punkte + " Punkte bekommen!");
        }
        return punkte;
    }

    private String toBenutzerStatsString(Benutzer benutzer) {
        return "<code>" + levelverwaltung.getLevel(benutzer.getPunkte()) + "</code> <b>" + levelverwaltung.getTitleForLevel(levelverwaltung.getLevel(benutzer.getPunkte())) + "</b> <a href=\"tg://user?id=" + benutzer.getUserId() + "\">" + benutzer.getNutzername() + "</a> (" + benutzer.getPunkte() + "/" + levelverwaltung.getGoal(levelverwaltung.getLevel(benutzer.getPunkte())) + ")";
    }

    private String getFullName(Integer id) {
        User user = (User) db.getMap("USERS").get(id);
        if (user == null) return "Unbekannt";
        return AbilityUtils.fullName(user);
    }

    private int stringToInt(String string) {
        int i = 0;
        try {
            i = Integer.parseInt(string);
        } catch (NumberFormatException ignored) {
        }
        return i;
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
