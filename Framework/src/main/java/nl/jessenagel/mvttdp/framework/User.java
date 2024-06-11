package nl.jessenagel.mvttdp.framework;

import java.util.*;
public class User{
    public int groupSize;
    public Map<Event, Integer> groupSizePerEvent;
    public String name;
    public TouristTime endTime;
    public boolean isScheduled;
    public ArrayList<Event> assignment;
    public double runTime;
    public int priority;
    public double happiness;
    public Location start, end;
    public List<Event> wishList;
    public  Map<Event,Double> scoreFunction;
    public List<Batch> schedule;
    public  TouristTime startTime;
    public  TouristTime queryTime;
    public  Event startEvent, endEvent;

    public Map<Event,List<Batch>> nonBlockedBatches;
    public User(){
        this.scoreFunction = new HashMap<>();
        this.groupSizePerEvent = new HashMap<>();
        this.startTime = new TouristTime("0:7:00");
        this.endTime = new TouristTime("0:23:59");
        this.happiness = 0.0;
        this.schedule = new ArrayList<>();
        this.assignment = new ArrayList<>();
        this.nonBlockedBatches = new HashMap<>();
    }

    public void printSchedule() {
        System.out.println(this.name) ;
        for(Batch batch : this.schedule){
            System.out.println(batch.event.name + " from: ");
            batch.startTime.print();
            System.out.println("to: ");
            batch.endTime.print();
            System.out.println("Place on wishlist: " + this.wishList.indexOf(batch.event));
        }
    }

    public void sortSchedule() {
        schedule.sort((o1, o2) -> {
            if (TouristTime.greater(o1.startTime, o2.startTime)) {
                return 1;
            }
            if (TouristTime.less(o1.startTime, o2.startTime)) {
                return -1;
            }
            return 0;
        });
    }
}
