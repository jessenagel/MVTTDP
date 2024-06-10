package nl.jessenagel.tourism.algorithmics;

import java.util.*;
import nl.jessenagel.tourism.framework.*;

public class FSTP {
    Area area;

    public FSTP(Area area) {
        this.area = area;
    }

    public void solve() {
        List<User> usersToSchedule = new LinkedList<>(this.area.users);
        Map<User, List<Batch>> previousTrips = new HashMap<>();
        for (int i = 1; i <= 3; i++) {
            for (User user : usersToSchedule) {
                if (previousTrips.get(user) != null) {
                    unBookBatches(previousTrips.get(user), user);
                }
                List<Batch> trip = createTrip(i, user);
                bookBatches(trip, user);
                previousTrips.put(user, trip);
                user.schedule = new ArrayList<>(trip);
            }
        }
        for (User user : usersToSchedule) {
            unBookBatches(previousTrips.get(user), user);
            List<Batch> trip = createTrip(user.wishList.size(), user);
            bookBatches(trip, user);
            previousTrips.put(user, trip);
            user.schedule = new ArrayList<>(trip);
        }

    }

    private List<Batch> createTrip(int i, User user) {
        FullEnumeration fullEnumeration = new FullEnumeration();
        fullEnumeration.area = this.area;
        fullEnumeration.user = user;
        List<Batch> trip = new ArrayList<>(fullEnumeration.solve(user.wishList.subList(0, i)));
        fullEnumeration.user.happiness = fullEnumeration.bestScore;
        return trip;
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
}
