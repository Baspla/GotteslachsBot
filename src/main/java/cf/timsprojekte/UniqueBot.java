package cf.timsprojekte;

public class UniqueBot {
    public static void set(Bot botn) {
        bot = botn;
    }

    private static Bot bot;

    public static Bot unique() {
        return bot;
    }
}
