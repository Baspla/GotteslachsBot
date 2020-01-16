package cf.timsprojekte.db;

import java.util.*;
import java.util.stream.Collectors;

public class EventManager {
    private final Map<Integer, Event> map;
    private final Map<Integer, Event> db;


    public EventManager(Map<Integer, Event> db) {
        this.db = db;
        this.map = new HashMap<>(db);
    }

    public void saveEvents() {
        db.putAll(map);
    }

    public void loadEvents() {
        map.putAll(db);
    }

    public Optional<Event> getEvent(int eventId) {
        return Optional.ofNullable(map.get(eventId));
    }

    public Event createEvent(String name, String desc, int points, int creator) {
        Random r = new Random();
        int rndInt;
        do {
            rndInt = r.nextInt();
        } while (map.keySet().contains(rndInt));
        Event event = new Event(name, desc, points, rndInt, creator);
        map.put(rndInt, event);
        return event;
    }

    public List<Event> getEventListe() {
        return new ArrayList<>(map.values());
    }

    public List<Event> getPendingEventListe() {
        return map.values().stream().filter(event -> !event.isAccepted()).collect(Collectors.toList());
    }

    public boolean hasEvent(Integer eventId) {
        return map.containsKey(eventId);
    }

    public void removeEvent(int eventId) {
        map.remove(eventId);
    }

    public List<Event> getEventListe(Integer userId) {
        return map.values().stream().filter(event -> event.getCreator() == userId).collect(Collectors.toList());
    }
}
