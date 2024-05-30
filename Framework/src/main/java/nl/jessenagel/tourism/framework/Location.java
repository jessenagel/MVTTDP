package nl.jessenagel.tourism.framework;

import java.util.HashMap;
import java.util.Map;

public class Location {
    public String name;
    public Map<String, Event> events;
    public boolean isOvernightLocation;
    public boolean isEvent;
    public double latitude,longitude;
    public Location(){
        events = new HashMap();
    }
}
