package cf.timsprojekte;

import org.apache.log4j.ConsoleAppender;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.log4j.PatternLayout;
import org.telegram.telegrambots.ApiContextInitializer;
import org.telegram.telegrambots.meta.TelegramBotsApi;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

public class Main {
    public static void main(String[] args) {
        ConsoleAppender console = new ConsoleAppender();
        String PATTERN = "%d [%p|%c|%C{1}] %m%n";
        console.setLayout(new PatternLayout(PATTERN));
        console.setThreshold(Level.FATAL);
        console.activateOptions();
        Logger.getRootLogger().addAppender(console);
        ApiContextInitializer.init();
        TelegramBotsApi botsApi = new TelegramBotsApi();
        Settings settings = new Settings();
        try {
            String token = settings.getBottoken();
            String name = settings.getBotname();
            if (token.isEmpty() || name.isEmpty()) {
                Logger.getRootLogger().log(Level.FATAL, "Kein Token oder Name gefunden");
                System.exit(42);
            }
            Bot bot = new Bot(token, name);
            UniqueBot.set(bot);
            botsApi.registerBot(bot);
        } catch (TelegramApiException e) {
            System.err.println(e);
        }
    }
}
