package nl.jessenagel.tourism.algorithmics;

import ilog.concert.IloException;
import ilog.concert.IloNumExpr;
import ilog.concert.IloNumVar;
import ilog.cplex.IloCplex;

import java.util.*;
import nl.jessenagel.tourism.framework.*;
public class Slootweg {
    private static final Double PRE_DETERMINED_CUT_OFF = 0.01;
    private static int I_MAX = 7;
    private static final int I_ENUM = 6;
    public Area area;
    private static final int MAX_NUMBER_OF_ASSIGNED_EVENTS = 1 ;

    public Slootweg() {

    }

    public void solve() {
        if(this.area.events.size()< I_MAX){
            I_MAX = this.area.events.size();
        }
        assignment();
        for (User user : this.area.users) {
            initialTripBuilder(user);
            user.sortSchedule();
            user.happiness = Generic.calcScore(user.schedule, user, this.area);
        }
        for (int i = 1; i < I_MAX; i++) {
            this.area.users.sort(Comparator.comparing(o -> o.happiness)); //Sort by happiness
            for (User user : this.area.users) {
                tripExpansion(user, i);
                user.happiness = Generic.calcScore(user.schedule, user, this.area);
            }
        }

    }

    private void assignment() {
        try {
            IloCplex cplex = new IloCplex();
            //Create variables
            Map<User, Map<Event, IloNumVar>> xMap = new HashMap<>();
            for (User user : this.area.users) {
                xMap.put(user, new HashMap<>());
                for (Event event : this.area.events.values()) {
                    IloNumVar var = cplex.boolVar();
                    var.setName(user.name + "_" + event.name);
                    xMap.get(user).put(event, var);
                }
            }
            //Create Constraint 1
            for (User user : this.area.users) {
                IloNumExpr lhs = cplex.constant(0);
                for (Event event : this.area.events.values()) {
                    lhs = cplex.sum(lhs, xMap.get(user).get(event));
                }
                cplex.addGe(lhs, 1);
                cplex.addLe(lhs, MAX_NUMBER_OF_ASSIGNED_EVENTS);
            }
            //Create Constraint 2
            for (Event event : this.area.events.values()) {
                IloNumExpr lhs = cplex.constant(0);
                for (User user : this.area.users) {
                    lhs = cplex.sum(lhs, xMap.get(user).get(event));
                }
                cplex.addLe(lhs, event.getCapacityRestOfDay(new TouristTime("0:0:0")));
            }
            //Create Objective Function
            IloNumExpr obj = cplex.constant(0);
            for (User user : this.area.users) {
                for (Event event : this.area.events.values()) {
                    obj = cplex.sum(obj, cplex.prod(xMap.get(user).get(event), user.scoreFunction.get(event)));
                }
            }
            cplex.addMaximize(obj);
            if (cplex.solve()) {
                System.out.println("Solution value:" + cplex.getObjValue());
                //
                for (User user : this.area.users) {
                    for (Event event : this.area.events.values()) {
                        if (cplex.getValue(xMap.get(user).get(event)) > 0.5) {
                            user.assignment.add(event);
                        }
                    }
                }
            } else {
                System.err.println("Initial assignment infeasible, exiting");
                System.exit(7);
            }
        } catch (IloException e) {
            throw new RuntimeException(e);
        }

    }


    private void tripExpansion(User user, int numberOfEventsFromWishlist) {
        if (greedyExpansion(user, numberOfEventsFromWishlist)) {
        } else {
            if(numberOfEventsFromWishlist < I_ENUM){
                //Full enumeration used
                FullEnumeration fullEnumeration = new FullEnumeration();
                fullEnumeration.user =user;
                fullEnumeration.area = area;
                List<Event> eventsToCheck = new ArrayList<>();
                for (int i = 0; i < numberOfEventsFromWishlist; i++) {
                    eventsToCheck.add(user.wishList.get(i));
                }
                List<Batch> newRoute = fullEnumeration.solve(eventsToCheck);
                if(Generic.calcScore(newRoute,user,area)> user.happiness){
                    this.area.unbookBatches(user,new ArrayList<>(user.schedule));
                    this.area.bookBatches(user,newRoute);
                }
            }else{
                //Improve by ILS
                ILS ils = new ILS();
                ils.area = this.area;
                ils.user = user;
                List<Event> eventsToCheck = new ArrayList<>();
                for (int i = 0; i < numberOfEventsFromWishlist; i++) {
                    eventsToCheck.add(user.wishList.get(i));
                }
                List<Batch> newRoute = ils.solve(eventsToCheck);
                if(Generic.calcScore(newRoute,user,area)> user.happiness){
                    this.area.unbookBatches(user,new ArrayList<>(user.schedule));
                    this.area.bookBatches(user,newRoute);
                }
            }
        }
    }

    private boolean greedyExpansion(User user, int numberOfEventsFromWishlist) {
        int currentLengthOfSchedule = user.schedule.size();
        if (numberOfEventsFromWishlist <= user.wishList.size()) {
            numberOfEventsFromWishlist = user.wishList.size();
            List<Event> eventsToCheck = new ArrayList<>();
            for (int i = 0; i < numberOfEventsFromWishlist; i++) {
                eventsToCheck.add(user.wishList.get(i));
            }
            int i = 0;
            while (true) {
                if (i < eventsToCheck.size()) {
                    if (user.scoreFunction.get(eventsToCheck.get(i)) < PRE_DETERMINED_CUT_OFF) {
                        eventsToCheck.remove(i);
                    } else {
                        i++;
                    }
                } else {
                    break;
                }
            }
            for (Event event : eventsToCheck) {
                user.nonBlockedBatches.computeIfAbsent(event, e -> new ArrayList<>(e.batches));
            }
            for (Batch batch : user.schedule) {
                for (Event eventComparison : eventsToCheck)
                    user.nonBlockedBatches.get(eventComparison).removeAll(eventComparison.getBlocked(user.nonBlockedBatches.get(eventComparison), batch.startTime.decreaseBy(this.area.travelTimes.get(eventComparison.exit).get(batch.event.entrance)).decreaseBy(eventComparison.length), batch.endTime.increaseBy(this.area.travelTimes.get(batch.event.exit).get(eventComparison.entrance))));
            }
            for (Event event : eventsToCheck) {
                double minimalBlockingValue = Double.MAX_VALUE;
                Batch minimalBlockingBatch = null;
                for (Batch batch : user.nonBlockedBatches.get(event)) {
                    double blockingValue = 0.0;
                    if (TouristTime.geq(user.startTime, batch.startTime.decreaseBy(this.area.travelTimes.get(user.start).get(batch.event.entrance))) || TouristTime.leq(user.endTime, batch.endTime.increaseBy(this.area.travelTimes.get(batch.event.exit).get(user.end))) || !batch.sufficientCapacityForGroup(user)) {
                        continue;
                    }
                    for (Event eventComparison : eventsToCheck) {
                        if (event == eventComparison) {
                            continue;
                        }
                        blockingValue += user.scoreFunction.get(eventComparison) * eventComparison.calculateFraction(user.nonBlockedBatches.get(eventComparison), batch.startTime.decreaseBy(this.area.travelTimes.get(eventComparison.exit).get(event.entrance)).decreaseBy(eventComparison.length), batch.endTime.increaseBy(this.area.travelTimes.get(event.exit).get(eventComparison.entrance)));
                    }
                    if (blockingValue < minimalBlockingValue) {
                        minimalBlockingBatch = batch;
                        minimalBlockingValue = blockingValue;
                    }
                }
                if (minimalBlockingBatch == null) {
                    continue;
                }

                for (Event eventComparison : eventsToCheck) {
                    if (event == eventComparison) {
                        user.nonBlockedBatches.get(event).removeAll(user.nonBlockedBatches.get(event));
                        continue;
                    }
                    user.nonBlockedBatches.get(eventComparison).removeAll(eventComparison.getBlocked(user.nonBlockedBatches.get(eventComparison), minimalBlockingBatch.startTime.decreaseBy(this.area.travelTimes.get(eventComparison.exit).get(event.entrance)).decreaseBy(eventComparison.length), minimalBlockingBatch.endTime.increaseBy(this.area.travelTimes.get(event.exit).get(eventComparison.entrance))));

                }
                minimalBlockingBatch.bookGroup(user);
            }
        }
        user.sortSchedule();
        return currentLengthOfSchedule < user.schedule.size();

    }

    private void initialTripBuilder(User user) {
        user.assignment.sort(Comparator.comparing(o -> user.scoreFunction.get(o))); //Sort by score function
        int i = 0;
        while (true) {
            if (i < user.assignment.size()) {
                if (user.scoreFunction.get(user.assignment.get(i)) < PRE_DETERMINED_CUT_OFF) {
                    user.assignment.remove(i);
                } else {
                    i++;
                }
            } else {
                break;
            }
        }
        for (Event event : user.assignment) {
            user.nonBlockedBatches.put(event, new ArrayList<>(event.batches));
        }
        for (Event event : user.assignment) {
            double minimalBlockingValue = Double.MAX_VALUE;
            Batch minimalBlockingBatch = null;
            for (Batch batch : user.nonBlockedBatches.get(event)) {
                double blockingValue = 0.0;
                if (TouristTime.geq(user.startTime, batch.startTime.decreaseBy(this.area.travelTimes.get(user.start).get(batch.event.entrance))) || TouristTime.leq(user.endTime, batch.endTime.increaseBy(this.area.travelTimes.get(batch.event.exit).get(user.end))) || !batch.sufficientCapacityForGroup(user)) {
                    continue;
                }
                for (Event eventComparison : user.assignment) {
                    if (event == eventComparison) {
                        continue;
                    }
                    blockingValue += user.scoreFunction.get(eventComparison) * eventComparison.calculateFraction(user.nonBlockedBatches.get(eventComparison), batch.startTime.decreaseBy(this.area.travelTimes.get(eventComparison.exit).get(event.entrance)).decreaseBy(eventComparison.length), batch.endTime.increaseBy(this.area.travelTimes.get(event.exit).get(eventComparison.entrance)));
                }
                if (blockingValue < minimalBlockingValue) {
                    minimalBlockingBatch = batch;
                    minimalBlockingValue = blockingValue;
                }
            }
            if (minimalBlockingBatch == null) {
                continue;
            }

            for (Event eventComparison : user.assignment) {
                if (event == eventComparison) {
                    user.nonBlockedBatches.get(event).removeAll(user.nonBlockedBatches.get(event));
                    continue;
                }
                user.nonBlockedBatches.get(eventComparison).removeAll(eventComparison.getBlocked(user.nonBlockedBatches.get(eventComparison), minimalBlockingBatch.startTime.decreaseBy(this.area.travelTimes.get(eventComparison.exit).get(event.entrance)).decreaseBy(eventComparison.length), minimalBlockingBatch.endTime.increaseBy(this.area.travelTimes.get(event.exit).get(eventComparison.entrance))));

            }
            minimalBlockingBatch.bookGroup(user);
        }
    }
}
