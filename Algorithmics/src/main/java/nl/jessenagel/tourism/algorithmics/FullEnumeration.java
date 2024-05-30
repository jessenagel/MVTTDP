
package nl.jessenagel.tourism.algorithmics;

import java.util.ArrayList;
import java.util.List;
import nl.jessenagel.tourism.framework.*;

public class FullEnumeration {
    public User user;
    public Area area;
    public List<List<Event>> permutations;
    public double bestScore;

    public FullEnumeration() {
        this.permutations = new ArrayList<>();
    }

    public List<Batch> solve(List<Event> wishList) {
        generatePermutations(wishList.size(), new ArrayList<>(wishList));
        this.bestScore = 0.0;
        List<Batch> bestRoute = new ArrayList<>();
        for (List<Event> events : this.permutations) {
            TouristTime time = user.startTime.copy();
            List<Batch> route = new ArrayList<>();
            double score = 0.0;
            Location previous = user.start;
            for (Event event : events) {
                time = time.increaseBy(area.travelTimes.get(previous).get(event.entrance));
                Batch firstBatch = firstBatch(time, event, this.user);
                if (firstBatch == null) {
                    break;
                }
                time = firstBatch.startTime.copy();
                time = time.increaseBy(event.length);
                if (TouristTime.greater(time.increaseBy(area.travelTimes.get(event.exit).get(user.end)), user.endTime)) {
                    break;
                }
                route.add(firstBatch);
                score += user.scoreFunction.get(event) + TouristConstants.WEIGHT_2 * area.travelTimes.get(previous).get(event.entrance).toMinutes();
                previous = event.exit;
            }
            if (score > this.bestScore) {
                bestRoute = new ArrayList<>(route);
                this.bestScore = score;
            }
        }
        return bestRoute;
    }

    static public Batch
    firstBatch(TouristTime time, Event event, User user) {

        for (Batch batch : event.batches) {
            if (TouristTime.geq(batch.startTime, time) && batch.sufficientCapacityForGroup(user)) {
                return (batch);
            }
        }
        //Return null
        return null;
    }

    private void generatePermutations(int k, List<Event> array) {
        if (k == 1) {
            permutations.add(new ArrayList<>(array));
        } else {
            generatePermutations(k - 1, array);

            for (int i = 0; i < k - 1; i++) {
                if (k % 2 == 0) {
                    swap(array, i, k - 1);
                } else {
                    swap(array, 0, k - 1);
                }
                generatePermutations(k - 1, array);
            }
        }
    }

    private <T> void swap(List<T> input, int a, int b) {
        T tmp = input.get(a);
        input.set(a, input.get(b));
        input.set(b, tmp);
    }

}
