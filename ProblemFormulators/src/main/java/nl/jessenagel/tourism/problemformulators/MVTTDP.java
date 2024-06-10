package nl.jessenagel.tourism.problemformulators;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import nl.jessenagel.tourism.algorithmics.*;
import nl.jessenagel.tourism.framework.*;
import nl.jessenagel.tourism.io.*;
import nl.jessenagel.tourism.checks.*;
public class MVTTDP {
    public Area area;


    /**
     * Builds the required Area class and its subclasses.
     */
    public void create() {
        this.area = new Area();
        this.area.simplified = true;
        LoadInputs.readLocations(this.area);
        LoadInputs.readEvents(this.area);
        LoadInputs.readTypes(this.area);
        LoadInputs.readDistances(this.area);
        CommonFunctions.createBatches(this.area);
        this.area.setEventTypes();
        if (TouristConstants.poisson.equals("homogeneous")) {
            this.area.generateUsers(Area.poissonRNG(TouristConstants.lambda));
        } else {
            this.area.generateUserInhomogeneous();
        }
        this.area.generateWishLists();
    }

    /**
     * Run a single day simulation
     */
    public void run() {
        long startTime = System.nanoTime();
        switch (TouristConstants.method) {
            case "MIP":
                MIP mip = new MIP();
                try {
                    mip.solve(this.area);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
                break;
            case "online":
                this.dynamicDay();
                break;
            case "offline":
                Slootweg solver = new Slootweg();
                solver.area = this.area;
                solver.solve();
                break;
            default:
                System.err.println("Unknown solving method: " + TouristConstants.method);
                System.exit(2);
        }

        long timeElapsed = System.nanoTime() - startTime;
        if (Feasibility.feasibilityChecker(this.area)) {
            WriteOutputs.exportJson(this.area, timeElapsed / 1000000);
            WriteOutputs.exportUtilization(this.area);
        } else {
            WriteOutputs.exportJson(this.area, timeElapsed / 1000000);
            System.err.println("INFEASIBLE");
        }
    }

    /**
     * Called by run(), dynamically simulate a day of tourists arriving.
     */
    private void dynamicDay() {
        double lambda_hat = 0;
        if (TouristConstants.knownLambda) {
            lambda_hat = TouristConstants.lambda;
        }
        Map<TouristType, Integer> numberOfEachType = new HashMap<>();
        for (TouristType touristType : this.area.touristTypes) {
            numberOfEachType.put(touristType, 0);
        }
        for (User user : this.area.users) {
            long startTime = System.nanoTime();
            int spacesLeft = this.area.calcNumberOfSpacesLeft(user.queryTime);
            double numberOfExpectedUsers = (960 - user.queryTime.toMinutes()) / 420 * lambda_hat;
            int numberOfAllowedBookings = 20;
            //Block tourist types:
            if (TouristConstants.useBlocking) {
                ILS.MIN_IMPROVEMENT = 1.3; //1.3
                for (Event event : this.area.events.values()) {
                    for (Batch batch : event.batches) {
                        if(this.area.calculateNumberOfUsersStillTooComeAtBatchMoreSophisticated(batch,user.queryTime) * this.area.calculateProbabilityOfEventRankingHigherThanK(batch.event, user.wishList.indexOf(batch.event),user.touristType) * TouristConstants.strictness > batch.event.getCapacityFromTill(user.queryTime,batch.startTime)+batch.getCurrentCapacity()){
                            batch.blockList.add(user);
                        }

                    }
                }
            }
            if (!TouristConstants.restrictNumberOfActivities) {
                numberOfAllowedBookings = this.area.events.size();
            }
            CommonFunctions.query(user, this.area, numberOfAllowedBookings);
            long timeElapsed = System.nanoTime() - startTime;
            user.runTime = timeElapsed/1000000;
        }
    }

}
