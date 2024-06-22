package nl.jessenagel.mvttdp.problemformulators;

import java.util.*;
import nl.jessenagel.mvttdp.framework.*;
import nl.jessenagel.mvttdp.algorithmics.*;
public class CommonFunctions {

    public static void query(User user, Area area, int numberOfALlowedBookings) {
//        System.out.println(user.queryTime.toMinutes());
        user.queryTime.print();
        List<Batch> route = new ArrayList<>();
        switch (TouristConstants.algorithm) {
            case "ILS":
                ILS ils = new ILS();
                ils.user = user;
                ils.area = area;
                ils.maxLength = numberOfALlowedBookings;
                route = ils.solve(user.wishList.subList(0,ils.maxLength));
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
                route = grasp.solve(user.wishList);
                break;
        }

        if (route.isEmpty()) {
            System.out.println("Could not create a schedule for " + user.name);
            return;
        }
        for (Batch batch : route) {
            if (!batch.bookGroup(user)) {
                System.out.println("Something going very wrong!");
            }
        }

        user.happiness = Generic.calcScore(route, user, area);
        user.schedule = route;
//        user.printSchedule();
        user.isScheduled = true;
        if(user.happiness < 5){
            System.out.println("_--------------------------------------------------------------_");
            user.printSchedule();
            user.queryTime.print();
            for(Event event : user.wishList){
                System.out.println(event.name);
            }
        }
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
                System.out.println(batch.event.name);
                batch.capacity = event.batchCapacity;
                batch.endTime = batch.startTime.copy().increaseBy(event.length);

                event.batches.add(batch);
                iterator = iterator.increaseBy(event.every);
            }
        }
    }
}
