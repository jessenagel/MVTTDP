package nl.jessenagel.mvttdp.framework;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
/**
 * This class represents an Event in the system.
 * An Event has a name, entrance and exit Locations, a list of Batches, and various other properties.
 */
public class Event {
    public String name;
    public Location entrance, exit;
    public final List<Batch> batches;
    public int capacity, batchCapacity;
    public boolean singleBatch;
    public TouristTime length, first, last, every;
    public final List<User> blockList;

    public final Map<Integer, Integer> concurrentVisitors;
    /**
     * Constructor for the Event class.
     * Initializes the batches, blockList, and concurrentVisitors lists.
     * Also populates the concurrentVisitors map with all times between open and close times.
     */
    public Event() {
        this.batches = new ArrayList<>();
        this.blockList = new ArrayList<>();
        this.concurrentVisitors = new HashMap<>();
        for(TouristTime touristTime : TouristTime.getAllTimesBetween(TouristConstants.openTime,TouristConstants.closeTime)){
            this.concurrentVisitors.put(touristTime.toMinutes(),0);
        }

    }
    /**
     * Returns a list of Batches that are active at the given time.
     *
     * @param time The time to check for active batches.
     * @return A list of Batches that are active at the given time.
     */
    public List<Batch> getBatchesAtTime(TouristTime time) {
        List<Batch> batchesAtTime = new ArrayList<>();
        for (Batch batch : this.batches) {
            if (TouristTime.leq(batch.startTime, time) && TouristTime.greater(batch.endTime, time)) {
                batchesAtTime.add(batch);
            }
        }
        return batchesAtTime;
    }


    /**
     * Returns the remaining capacity of the event for the rest of the day from the given time.
     *
     * @param time The time from which to check the remaining capacity.
     * @return The remaining capacity of the event for the rest of the day.
     */
    public int getCapacityRestOfDay(TouristTime time) {
        int capacity = 0;
        for (Batch batch : this.batches) {
            if (TouristTime.geq(batch.startTime, time)) {
                capacity += batch.getCurrentCapacity();
            }
        }
        return capacity;
    }
    /**
     * Returns a list of batches that are concurrent with the given batch.
     *
     * @param batch The batch to check for concurrency.
     * @return A list of batches that are concurrent with the given batch.
     */
    List<Batch> getConcurrentBatches(Batch batch) {
        List<Batch> concurrentBatches = new ArrayList<>();
        for (Batch batchCompare : this.batches) {
            if ((TouristTime.geq(batchCompare.startTime, batch.startTime) && TouristTime.less(batchCompare.startTime, batch.endTime)) || (TouristTime.greater(batchCompare.endTime, batch.startTime) && TouristTime.leq(batchCompare.endTime, batch.endTime))) {
                concurrentBatches.add(batchCompare);
            }
        }
        return concurrentBatches;
    }
    /**
     * Checks if there is sufficient concurrent capacity for the given user's group in the given batch.
     *
     * @param batch The batch to check for capacity.
     * @param user The user whose group's capacity needs to be checked.
     * @return True if there is sufficient capacity, false otherwise.
     */
    public boolean sufficientConcurrentCapacityForGroup(Batch batch, User user) {
        int sum = 0;
        for (Batch batch2 : this.getConcurrentBatches(batch)) {
            sum += batch2.getCurrentVisitors();
        }
        return sum + user.groupSizePerEvent.get(this) <= this.capacity;
    }

    /**
     * Returns the capacity of the event from the current time till the given start time.
     *
     * @param currentTime The current time.
     * @param startTime The start time till which to check the capacity.
     * @return The capacity of the event from the current time till the given start time.
     */
    public double getCapacityFromTill(TouristTime currentTime, TouristTime startTime) {
        int capacity = 0;
        for (Batch batch : this.batches) {
            if (TouristTime.geq(batch.startTime, currentTime) && TouristTime.leq(batch.startTime, startTime)) {
                capacity += batch.getCurrentCapacity();

            }
        }
        return capacity;
    }
    /**
     * Calculates the fraction of the given list of batches that are blocked between the given start and end times.
     *
     * @param batches The list of batches.
     * @param start The start time.
     * @param end The end time.
     * @return The fraction of the given list of batches that are blocked between the given start and end times.
     */
    public double calculateFraction(List<Batch> batches, TouristTime start, TouristTime end) {
        List<Batch> blocked = new ArrayList<>();
        for (Batch batch : batches) {
            if (TouristTime.geq(batch.startTime, start) && TouristTime.leq(batch.startTime, end)) {
                blocked.add(batch);
            }
        }
        if (batches.isEmpty()) {
            return 0.0;
        }
        return (double) blocked.size() / (double) batches.size();
    }
    /**
     * Returns a list of batches from the given list that are blocked between the given start and end times.
     *
     * @param batches The list of batches.
     * @param start The start time.
     * @param end The end time.
     * @return A list of batches from the given list that are blocked between the given start and end times.
     */
    public List<Batch> getBlocked(List<Batch> batches, TouristTime start, TouristTime end) {
        List<Batch> blocked = new ArrayList<>();
        for (Batch batch : batches) {
            if (TouristTime.geq(batch.startTime, start) && TouristTime.leq(batch.startTime, end)) {
                blocked.add(batch);
            }
        }
        return blocked;
    }
    /**
     * Updates the number of concurrent visitors for each time between the start and end times of the given batch.
     *
     * @param batch The batch whose start and end times are to be used.
     * @param groupSize The size of the group to add to the number of concurrent visitors.
     */
    public void updateConcurrentVisitors(Batch batch, Integer groupSize) {
        for(TouristTime time : TouristTime.getAllTimesBetween(batch.startTime,batch.endTime)){
            this.concurrentVisitors.put(time.toMinutes(),this.concurrentVisitors.get(time.toMinutes())+groupSize);
        }

    }
    /**
     * Returns the next batch with capacity that starts at or after the given time.
     *
     * @param time The time from which to check for the next batch with capacity.
     * @return The next batch with capacity that starts at or after the given time, or null if no such batch exists.
     */
    public Batch getNextBatchWithCapacity(TouristTime time,User user){
        for(Batch batch : this.batches){
            if(TouristTime.geq(batch.startTime,time) && batch.sufficientCapacityForGroup(user)){
                return batch;
            }
        }
        return null;
    }
}

