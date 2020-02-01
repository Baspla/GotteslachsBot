package cf.timsprojekte.minecraft;

import org.apache.log4j.*;

import java.util.ArrayList;
import java.util.concurrent.*;

public class MinecraftManager {

    private final Runnable minecraftRunnable;
    private final Logger mcLogger;
    public MCInfo info;

    public MinecraftManager(){

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


        minecraftRunnable = () -> {
            MCInfo oldInfo = info;
            info = MCInfo.request("89.163.187.158", 25565);
            if (oldInfo != null) {
                ArrayList<MCPlayer> leaves = checkLeaves(oldInfo.getSamples(), info.getSamples());
                ArrayList<MCPlayer> joins = checkJoins(oldInfo.getSamples(), info.getSamples());
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

        ScheduledExecutorService minecraftTask = Executors.newSingleThreadScheduledExecutor();
        minecraftRunnable.run();
        minecraftTask.scheduleAtFixedRate(minecraftRunnable, 0, 30, TimeUnit.SECONDS);
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
}
