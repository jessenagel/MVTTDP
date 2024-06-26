package nl.jessenagel.mvttdp.problemformulators;

import java.util.*;

import nl.jessenagel.mvttdp.framework.*;
import nl.jessenagel.mvttdp.algorithmics.*;

public class CommonFunctions {

    public static void query(User user, Area area, int numberOfAllowedBookings) {
        List<Batch> route = new ArrayList<>();
        switch (TouristConstants.algorithm) {
            case "ILS":
                ILS ils = new ILS();
                ils.user = user;
                ils.area = area;
                ils.maxLength = numberOfAllowedBookings;
                route = ils.solve(user.wishList.subList(0, ils.maxLength));
                break;
            case "FullEnumeration":
                FullEnumeration fullEnumeration = new FullEnumeration();
                fullEnumeration.user = user;
                fullEnumeration.area = area;
                route = fullEnumeration.solve(user.wishList);
                break;
            case "GRASP":
                GRASP grasp = new GRASP();
                grasp.user = user;
                grasp.area = area;
                route = grasp.solve(user.wishList.subList(0, numberOfAllowedBookings));
                break;
            case "ScoreGRASP":
                ScoreGRASP scoreGRASP = new ScoreGRASP();
                scoreGRASP.user = user;
                scoreGRASP.area = area;
                route = scoreGRASP.solve(user.wishList.subList(0, numberOfAllowedBookings));
                break;
        }

        if (route.isEmpty()) {
            System.out.println("Could not create a schedule for " + user.name);
            return;
        }
        for (Batch batch : route) {
            if (!batch.bookGroup(user)) {
                System.out.println("Unable to book assigned slot. Something going very wrong!");
            }
        }

        user.happiness = Generic.calcScore(route, user, area);
        user.schedule = route;
        user.isScheduled = true;
    }


    public static void createBatches(Area amsterdam) {
        for (Event event : amsterdam.events.values()) {
            if (event.singleBatch) {
                Batch batch = new Batch();
                batch.event = event;
                batch.startTime = event.first;
                batch.capacity = event.capacity;
                batch.endTime = batch.startTime.copy().increaseBy(event.length);
                event.batches.add(batch);
                continue;
            }
            TouristTime iterator = event.first.copy();
            while (TouristTime.leq(iterator, event.last)) {
                Batch batch = new Batch();
                batch.event = event;
                batch.startTime = iterator;
                batch.capacity = event.batchCapacity;
                batch.endTime = batch.startTime.copy().increaseBy(event.length);

                event.batches.add(batch);
                iterator = iterator.increaseBy(event.every);
            }
        }
    }
}
