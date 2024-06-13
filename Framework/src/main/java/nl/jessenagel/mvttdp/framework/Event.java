package nl.jessenagel.mvttdp.framework;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Event {
    public String name;
    public Location entrance, exit;
    public final List<Batch> batches;
    public int capacity, batchCapacity;
    public boolean singleBatch;
    public TouristTime length, first, last, every;
    public final List<User> blockList;

    public final Map<Integer, Integer> concurrentVisitors;
    public Event() {
        this.batches = new ArrayList<>();
        this.blockList = new ArrayList<>();
        this.concurrentVisitors = new HashMap<>();
        for(TouristTime touristTime : TouristTime.getAllTimesBetween(TouristConstants.openTime,TouristConstants.closeTime)){
            this.concurrentVisitors.put(touristTime.toMinutes(),0);
        }

    }

    public List<Batch> getBatchesAtTime(TouristTime time) {
        List<Batch> batchesAtTime = new ArrayList<>();
        for (Batch batch : this.batches) {
            if (TouristTime.leq(batch.startTime, time) && TouristTime.greater(batch.endTime, time)) {
                batchesAtTime.add(batch);
            }
        }
        return batchesAtTime;
    }

    public int getCapacityRestOfDay(TouristTime time) {
        int capacity = 0;
        for (Batch batch : this.batches) {
            if (TouristTime.geq(batch.startTime, time)) {
                capacity += batch.getCurrentCapacity();
            }
        }
        return capacity;
    }

    List<Batch> getConcurrentBatches(Batch batch) {
        List<Batch> concurrentBatches = new ArrayList<>();
        for (Batch batchCompare : this.batches) {
            if ((TouristTime.geq(batchCompare.startTime, batch.startTime) && TouristTime.less(batchCompare.startTime, batch.endTime)) || (TouristTime.greater(batchCompare.endTime, batch.startTime) && TouristTime.leq(batchCompare.endTime, batch.endTime))) {
                concurrentBatches.add(batchCompare);
            }
        }
        return concurrentBatches;
    }

    public boolean sufficientConcurrentCapacityForGroup(Batch batch, User user) {
        int sum = 0;
        for (Batch batch2 : this.getConcurrentBatches(batch)) {
            sum += batch2.getCurrentVisitors();
        }
        return sum + user.groupSizePerEvent.get(this) <= this.capacity;
    }


    public double getCapacityFromTill(TouristTime currentTime, TouristTime startTime) {
        int capacity = 0;
        for (Batch batch : this.batches) {
            if (TouristTime.geq(batch.startTime, currentTime) && TouristTime.leq(batch.startTime, startTime)) {
                capacity += batch.getCurrentCapacity();

            }
        }
        return capacity;
    }

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

    public List<Batch> getBlocked(List<Batch> batches, TouristTime start, TouristTime end) {
        List<Batch> blocked = new ArrayList<>();
        for (Batch batch : batches) {
            if (TouristTime.geq(batch.startTime, start) && TouristTime.leq(batch.startTime, end)) {
                blocked.add(batch);
            }
        }
        return blocked;
    }

    public void updateConcurrentVisitors(Batch batch, Integer groupSize) {
        for(TouristTime time : TouristTime.getAllTimesBetween(batch.startTime,batch.endTime)){
            this.concurrentVisitors.put(time.toMinutes(),this.concurrentVisitors.get(time.toMinutes())+groupSize);
        }

    }
}
