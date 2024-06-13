package nl.jessenagel.mvttdp.framework;

import java.util.HashMap;
import java.util.Map;

public class Location {
    public String name;
    public final Map<String, Event> events;
    public boolean isOvernightLocation;
    public double latitude,longitude;
    public Location(){
        events = new HashMap<>();
    }
}
