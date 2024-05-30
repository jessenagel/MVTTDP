package nl.jessenagel.tourism.algorithmics;

import java.util.*;
import nl.jessenagel.tourism.framework.*;

public class ESTP {
    Area area;
    static final int ENUM = 5;

    ESTP(Area area) {
        this.area = area;
    }

    public void solve() {
        List<User> usersToSchedule = new LinkedList<>(this.area.users);
        Map<User, List<Batch>> previousTrips = new HashMap<>();
        for (int i = 1; i <= 5; i++) {
            System.out.println(i);
            for (User user : usersToSchedule) {
                boolean success = false;
                if (previousTrips.get(user) != null) {
                    unBookBatches(previousTrips.get(user), user);
                    success = extendTrip(i, previousTrips.get(user), user);
                    user.happiness = calcScore(previousTrips.get(user), user);
                }
                if (!success) {
                    if (i <= ENUM) {
                        List<Batch> newTrip = createTripEnumerate(i, user);
                        double newScore = calcScore(newTrip, user);
                        if (newScore > user.happiness) {
                            user.happiness = newScore;
                            previousTrips.put(user, newTrip);
                        }
                    }
                }
                if (previousTrips.get(user) != null) {
                    bookBatches(previousTrips.get(user), user);
                    user.schedule = new ArrayList<>(previousTrips.get(user));
                }
            }
            resortUsers(usersToSchedule);
        }

    }

    private void resortUsers(List<User> users) {
        users.sort(Comparator.comparingDouble(o -> o.happiness));
    }

    private boolean extendTrip(int i, List<Batch> batches, User user) {
        Batch currentBatch = batches.get(batches.size() - 1);
        Event eventToVisit = user.wishList.get(i - 1);
        Batch firstBatch = FullEnumeration.firstBatch(currentBatch.endTime.increaseBy(area.travelTimes.get(currentBatch.event.exit).get(eventToVisit.entrance)), eventToVisit, user);
        if (firstBatch != null) {
            batches.add(firstBatch);
            return true;
        } else {
            return false;
        }
    }


    private List<Batch> createTripEnumerate(int i, User user) {
        FullEnumeration fullEnumeration = new FullEnumeration();
        fullEnumeration.area = this.area;
        fullEnumeration.user = user;
        return new ArrayList<>(fullEnumeration.solve(user.wishList.subList(0, i)));
    }

    private List<Batch> createTripILS(int i, User user) {
        //TODO
//        FullEnumeration fullEnumeration = new FullEnumeration();
//        fullEnumeration.area = this.area;
//        fullEnumeration.user = user;
//        List<Batch> trip = new ArrayList<>(fullEnumeration.solve(user.wishList.subList(0,i)));
//        fullEnumeration.user.happiness = fullEnumeration.bestScore;
        return null;
    }

    private void bookBatches(List<Batch> batches, User user) {
        for (Batch batch : batches) {
            batch.visitors.add(user);
        }
    }

    private void unBookBatches(List<Batch> batches, User user) {
        for (Batch batch : batches) {
            batch.visitors.remove(user);
        }
    }

    private double calcScore(List<Batch> batches, User user) {
        //TODO: add distance?
        Double score = 0.0;
        for (Batch batch : batches) {
            score += user.scoreFunction.get(batch.event);
        }
        return score;
    }
}
