package nl.jessenagel.tourism.framework;

import java.util.ArrayList;
import java.util.List;

public class TouristType {
    public String name;
    public List<String> bonusEvents;
    public List<Event> baseRanking;
    public double probability = 0.0;

    public TouristType() {
        bonusEvents = new ArrayList<>();
        baseRanking = new ArrayList<>();
    }
}
