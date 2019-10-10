package cf.timsprojekte;

import cf.timsprojekte.db.Nutzer;
import cf.timsprojekte.db.NutzerManager;
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
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.meta.exceptions.TelegramApiRequestException;

import javax.annotation.PostConstruct;
import java.io.IOException;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.*;

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
    private static final boolean ANNOUNCE_STARTUP = true;
    private static final boolean ANNOUNCE_REWARDS = false;
    private final Logger logger;
    @SuppressWarnings({"FieldCanBeLocal", "unused"})
    private final Map<Integer, Nutzer> mapNutzer;
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
        mapNutzer = db.getMap("NUTZER");
        listGroups = db.getList("GROUPS");
        random = new Random();
        uncheckedGroups = new ArrayList<>();
        nutzermanager = new NutzerManager(mapNutzer, users());
        ausgabe = new Ausgabe(creatorId(), listGroups, silent, new Locale("de", "DE"), sender);
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            logger.info("ShutdownHook");
            nutzermanager.saveNutzer();
            db.commit();
            try {
                db.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }));
        ScheduledExecutorService backupTask = Executors.newSingleThreadScheduledExecutor();
        backupTask.scheduleAtFixedRate(db::commit, 10, 10, TimeUnit.MINUTES);
    }

    @PostConstruct
    private void register() {
        try {
            new TelegramBotsApi().registerBot(this);
        } catch (TelegramApiRequestException e) {
            e.printStackTrace();
        }
        if (ANNOUNCE_STARTUP)
            ausgabe.sendToAllGroups("system.started", null);
    }

    @SuppressWarnings({"unused", "unchecked"})
    public Ability cmdStats() {
        return Ability.builder()
                .name("stats")
                .flag(update -> nutzermanager.hasNutzer(AbilityUtils.getUser((Update) update).getId()))
                .privacy(PUBLIC)
                .locality(USER)
                .input(0)
                .action(ctx -> {
                    Nutzer nutzer = nutzermanager.getNutzer(ctx.user().getId());
                    ausgabe.send(ctx.chatId(), nutzer.getLocale(), "user.stats", nutzer.getUserId(), nutzer.getUsername(), nutzer.getPoints(), nutzer.getVotes(), nutzer.getLocale().toLanguageTag());//TODO
                })
                .build();
    }

    @SuppressWarnings("unused")
    public Ability cmdSettings() {
        return Ability.builder()
                .name("settings")
                .info("Einstellungen")
                .privacy(PUBLIC)
                .locality(USER)
                .input(0)
                .action(ctx -> {
                    logger.debug("Einstellungen geöffnet");
                    //TODO
                })
                .build();
    }

    @SuppressWarnings("unused")
    public Ability cmdStop() {
        return Ability.builder()
                .name("stop")
                .info("Hält den Bot an")
                .privacy(ADMIN)
                .locality(ALL)
                .input(0)
                .action(ctx -> {
                    ausgabe.send(ctx.chatId(), nutzermanager.getNutzer(ctx.user().getId()).getLocale(), "system.shutdown", null);
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
                .info("Speichert alle Änderungen")
                .privacy(ADMIN)
                .locality(ALL)
                .input(0)
                .action(ctx -> {
                    Optional<Message> r = ausgabe.send(ctx.chatId(), nutzermanager.getNutzer(ctx.user().getId()).getLocale(), "system.saving", null);
                    nutzermanager.saveNutzer();
                    db.commit();
                    r.ifPresent(message -> ausgabe.edit(message.getChatId(), message.getMessageId(), nutzermanager.getNutzer(ctx.user().getId()).getLocale(), "system.saved", null));
                })
                .build();
    }

    @SuppressWarnings("unused")
    public Ability cmdTopPoints() {
        return Ability.builder()
                .name("points")
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

    public Ability cmdAdmin() {
        return Ability.builder()
                .name("admin")
                .privacy(ADMIN)
                .locality(USER)
                .input(0)
                .action(ctx -> {
                    logger.debug("Admin Menu geoeffnet");
                })
                .build();
    }

    @SuppressWarnings("unused")
    public Reply onReply() {
        return Reply.of(update -> {
            try {
                if (update.getMessage().getReplyToMessage().getFrom().equals(this.getMe())) {
                    //TODO
                }
            } catch (TelegramApiException e) {
                e.printStackTrace();
            }
        }, Flag.MESSAGE, Flag.REPLY, AbilityUtils::isUserMessage);
    }


    @SuppressWarnings("unused")
    public Reply onCallback() {
        return Reply.of(update -> {
            String data = update.getCallbackQuery().getData();
            if (data.startsWith("adm+")) {
                if (!update.getCallbackQuery().getFrom().getId().equals(creatorId())) return;
                long chatId = Long.parseLong(data.substring(4));
                if (!uncheckedGroups.contains(chatId)) return;
                uncheckedGroups.remove(chatId);
                listGroups.add(chatId);
                ausgabe.answerCallback(update.getCallbackQuery().getId());
                ausgabe.removeMessage(update.getCallbackQuery().getMessage().getChatId(), update.getCallbackQuery().getMessage().getMessageId());
                ausgabe.sendToGroup(chatId, "group.accepted", null);
            } else if (data.startsWith("adm-")) {
                if (!update.getCallbackQuery().getFrom().getId().equals(creatorId())) return;
                long chatId = Long.parseLong(data.substring(4));
                if (!uncheckedGroups.contains(chatId)) return;
                uncheckedGroups.remove(chatId);
                ausgabe.sendToGroup(chatId, "group.declined", null);
                try {
                    sender.execute(new LeaveChat().setChatId(chatId));
                } catch (TelegramApiException e) {
                    e.printStackTrace();
                }
            }
        }, Flag.CALLBACK_QUERY);
    }

    @SuppressWarnings("unused")
    public Reply onGroupMessage() {
        return Reply.of(update -> {
            Message message = update.getMessage();
            Nutzer nutzer = nutzermanager.getNutzer(message.getFrom().getId());

            //Prüft ob die Gruppe bestätigt ist
            if (!listGroups.contains(message.getChatId())) {
                if (uncheckedGroups.contains(message.getChatId())) return;
                uncheckedGroups.add(message.getChatId());
                ausgabe.sendOwnerGroupCheck(message.getChatId(), message.getChat().getTitle());
                ausgabe.sendToGroup(message.getChatId(), "group.requested", null);
                return;
            }

            if (message.isReply()) {
                Message replyTo = message.getReplyToMessage();
                if (!replyTo.getFrom().getBot()) {
                    Nutzer ziel = nutzermanager.getNutzer(replyTo.getFrom().getId());
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
            //Prüft ob der Nutzer Punkte für die Nachticht erhält
            if (!nutzer.hasCooldownReward()) {
                Random r = new Random();
                int reward = (r.nextInt(NACHRICHT_PUNKTE_MAX) + NACHRICHT_PUNKTE_MIN);
                if (random.nextInt(JACKPOT_CHANCE) == 0) {
                    reward = reward * JACKPOT_MULTIPLIER;
                    ausgabe.sendToAllGroups("reward.jackpot", reward, nutzer.getLinkedUsername());
                }
                int levelVorher = nutzer.getLevel();
                nutzer.addPoints(reward);
                if (levelVorher < nutzer.getLevel()) {
                    if (levelVorher == nutzer.getLevel() - 1) {
                        ausgabe.sendImage(message.getChatId(), nutzer.getLocale(), "levelup.single", "smug.moe/smg/" + nutzer.getLevel() + ".png", nutzer.getLinkedUsername(), nutzer.getTitel());
                    } else {
                        ausgabe.sendImage(message.getChatId(), nutzer.getLocale(), "levelup.multi", "smug.moe/smg/" + nutzer.getLevel() + ".png", nutzer.getLinkedUsername(), nutzer.getTitel(), nutzer.getLevel() - levelVorher - 1);
                    }
                }
                nutzer.setCooldownReward(r.nextInt(NACHRICHT_COOLDOWN_MAX) + NACHRICHT_COOLDOWN_MIN, ChronoUnit.MINUTES);
            }

        }, Flag.MESSAGE, update -> {
            Message m = update.getMessage();
            return (m.isSuperGroupMessage() || m.isGroupMessage()) && !m.isCommand() && (m.hasSticker() || m.hasAnimation() || m.hasAudio() || m.hasContact() || m.hasDocument() || m.hasLocation() || m.hasPhoto() || m.hasPoll() || m.hasText() || m.hasVideo() || m.hasVoice() || m.hasVideoNote()) && m.getPinnedMessage() == null && m.getDeleteChatPhoto() == null && m.getLeftChatMember() == null && m.getNewChatTitle() == null && m.getNewChatPhoto() == null;
        });
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
