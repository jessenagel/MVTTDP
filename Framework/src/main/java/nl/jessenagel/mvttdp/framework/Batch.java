package nl.jessenagel.mvttdp.framework;

import java.util.ArrayList;
import java.util.List;

public class Batch {
    public final List<User> blockList;
    public TouristTime startTime, endTime;
    public int capacity;
    public Event event;
    public final List<User> visitors;

    public Batch() {
        this.visitors = new ArrayList<>();
        this.blockList = new ArrayList<>();
    }

    public int getCurrentCapacity() {
        int currentNumberOfVisitors = 0;
        for (User user : this.visitors) {
            currentNumberOfVisitors += user.groupSizePerEvent.get(this.event);
        }
        return capacity - currentNumberOfVisitors;
    }

    public boolean sufficientCapacityForGroup(User user) {
        return this.getCurrentCapacity() - user.groupSizePerEvent.get(this.event) >= 0 && this.event.sufficientConcurrentCapacityForGroup(this, user) && !this.event.blockList.contains(user) && !this.blockList.contains(user);
    }


    public boolean bookGroup(User user) {
        if (this.sufficientCapacityForGroup(user)) {
            this.visitors.add(user);
            user.schedule.add(this);
            this.event.updateConcurrentVisitors(this,user.groupSizePerEvent.get(this.event));
            return true;
        } else {
            return false;
        }
    }

    public int getCurrentVisitors() {
        int currentNumberOfVisitors = 0;
        for (User user : this.visitors) {
            currentNumberOfVisitors += user.groupSizePerEvent.get(this.event);
        }
        return currentNumberOfVisitors;
    }

    public boolean unbookGroup(User user) {
        try{
        this.visitors.remove(user);
        user.schedule.remove(this);}
        catch(NullPointerException e){
            System.err.println("Tried to unbook nonbooked events!");
            return false;
        }
        return true;
    }
}
