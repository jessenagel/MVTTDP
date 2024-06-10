package nl.jessenagel.tourism.checks;

import nl.jessenagel.tourism.framework.*;

import java.util.ArrayList;
import java.util.List;

public class Feasibility {
    public static boolean feasibilityChecker(Area area) {
        //Check that trips are reachable and don't overlap
        for (User user : area.users) {
            List<Event> visitedEvents = new ArrayList<>();
            for (int i = 0; i < user.schedule.size(); i++) {
                Batch batch = user.schedule.get(i);
                if(visitedEvents.contains(batch.event)){
                    System.err.println("INFEASIBLE: Event visited twice: " + batch.event.name + " by user " + user.name);
                    return false;
                }
                visitedEvents.add(batch.event);
                if (i == 0) {
                    if (TouristTime.greater(user.startTime.increaseBy(area.travelTimes.get(user.start).get(batch.event.entrance)), batch.startTime)) {
                        System.err.println("INFEASIBLE: The first event cannot be reached in time from the starting location.");
                        return false;
                    }
                    continue;
                }
                Batch previousBatch = user.schedule.get(i - 1);
                if (TouristTime.greater(previousBatch.endTime.increaseBy(area.travelTimes.get(previousBatch.event.exit).get(batch.event.entrance)), batch.startTime)) {
                    System.err.println("INFEASIBLE: The nth event cannot be reached in time from the n-1th event. n = " + i);
                    System.err.println("Current event: " + batch.event.name);
                    batch.startTime.print();
                    System.err.println("Previous event:"+ previousBatch.event.name);
                    previousBatch.endTime.print();
                    System.err.println("Travel time:");
                    area.travelTimes.get(previousBatch.event.exit).get(batch.event.entrance).print();
                    System.err.println(user.name);
                    return false;
                }
                if (i == user.schedule.size() - 1) {
                    if (TouristTime.greater(batch.endTime.increaseBy(area.travelTimes.get(batch.event.exit).get(user.end)), user.endTime)) {
                        System.err.println("INFEASIBLE: The final location cannot be reached in time from the last event.");
                        return false;
                    }
                }
            }
        }
        for (Event event : area.events.values()) {
            for (int t = 0; t <= 1440; t++) {
                int sum = 0;
                for (Batch batch : event.getBatchesAtTime(new TouristTime("0:0:" + t))) {
                    sum+= batch.getCurrentVisitors();
                }
                if(sum > event.capacity){
                    System.err.println("INFEASIBLE: the concurrent capacity at event " + event.name + " is exceed by scheduling " + sum + " users while the capacity is " + event.capacity + " at time:");
                    new TouristTime("0:0:" + t).print();
                    return false;
                }
            }
        }
        for(Event event : area.events.values()){
            for(Batch batch : event.batches){
                if(batch.getCurrentVisitors() > batch.capacity){
                    System.err.println("INFEASIBLE: the capacity of a batch at event " + event.name + " is exceed. This batch has " + batch.getCurrentVisitors() + " visitors and only " + batch.capacity + " capacity. The batch starts at:");
                    batch.startTime.print();
                    return false;
                }
            }
        }
        return true;
    }
}
