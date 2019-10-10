package cf.timsprojekte;

import org.apache.log4j.*;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.telegram.telegrambots.ApiContextInitializer;

@SpringBootApplication
public class Main {
    public static void main(String[] args) {
        ConsoleAppender console = new ConsoleAppender();
        String PATTERN = "%d [%p|%c|%C{1}] %m%n";
        console.setLayout(new PatternLayout(PATTERN));
        console.setThreshold(Level.FATAL);
        console.activateOptions();
        Logger.getRootLogger().addAppender(console);
        if (System.getenv("gotteslachsbot_token") == null || System.getenv("gotteslachsbot_name") == null) {
            Logger.getRootLogger().fatal("Token oder Name fehlt");
            return;
        }
        ApiContextInitializer.init();
        SpringApplication.run(Bot.class, args);
    }

}
