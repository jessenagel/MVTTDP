package nl.jessenagel.mvttdp.framework;
import nl.jessenagel.mvttdp.framework.*;
import java.util.*;
import java.util.stream.Collectors;
import  org.apache.commons.math3.distribution.NormalDistribution;
public class Area {
    public static Random generator = new Random(TouristConstants.seed);
    public Map<String, Event> events;
    public Map<String, Location> locations;
    public List<User> users;
    public Map<Location, Map<Location, TouristTime>> travelTimes;
    public List<Location> overnightLocations;
    public List<Event> baseRanking;
    public int numberOfUsers;
    public int lengthOfWishlist;
    public boolean simplified; //Use the simplified version with types instead of wishlists

    public Area() {
        locations = new HashMap<>();
        users = new ArrayList<>();
        events = new HashMap<>();
        travelTimes = new HashMap<>();
        baseRanking = new ArrayList<>();
    }


    public void setOvernightLocations() {
        List<Location> overnightLocations = new ArrayList<>();
        for (Location location : this.locations.values()) {
            if (location.isOvernightLocation) {
                overnightLocations.add(location);
            }
        }
        this.overnightLocations = overnightLocations;
    }

    public Location getRandomOvernightLocation() {
        double random = generator.nextDouble();
        random *= overnightLocations.size();
        random = Math.floor(random);
        return overnightLocations.get((int) random);
    }

    private static TouristTime generateTime() {
        Random generator = new Random(TouristConstants.seed);
        double number;
        double a = 9;
        double d = a - 1.0 / 0.3;
        double c = 1.0 / Math.sqrt(9 * d);
        while (true) {
            double X = generator.nextGaussian();
            double U = generator.nextDouble();
            double v = Math.pow(1 + c * X, 3);
            if (v > 0 && Math.log(U) < Math.pow(X, 2) / 2.0 + d - d * v + d * Math.log(v)) {
                number = d * v;
                break;
            }
        }
        number = 420 + number * 30;
        TouristTime result = new TouristTime("0");
        result.minute = (int) Math.floor(number);
        result.rebalance();
        return result;
    }

    private static TouristTime generateTimeUniform() {
        double number;
        number = 420 + generator.nextDouble() * 540;
        TouristTime result = new TouristTime("0");
        result.minute = (int) Math.floor(number);
        result.rebalance();
        result.day = TouristConstants.dayOfEvents;
        result.rebalance();
        return result;
    }
    private static TouristTime generateQuery() {
        double number;
        number = TouristConstants.startArrivals.toMinutes() + generator.nextDouble() * (TouristConstants.endArrivals.toMinutes()-420);
        double day;
        switch (TouristConstants.arrivalProcess) {
            case "during":
                day = TouristConstants.dayOfEvents;
                break;
            case "before":
                day = Math.floor(generator.nextDouble() * TouristConstants.dayOfEvents);
                break;
            case "combined":
                day = Math.floor(generator.nextDouble() * (TouristConstants.dayOfEvents + 1));
                break;
            default:
                System.err.println("Unknown arrivalprocess: " + TouristConstants.arrivalProcess + ". Defaulting to during");
                day = TouristConstants.dayOfEvents;
                break;
        }
        TouristTime result = new TouristTime("0");
        result.minute = (int) Math.floor(number);
        result.rebalance();
        result.day = (int) day;
        result.rebalance();
        return result;
    }

    public static int poissonRNG(double lambda) {
        Random poissionGenerator = new Random(TouristConstants.seed);
        double Lleft = lambda;
        int step = 500;
        int k = 0;
        double p = 1;
        do {
            k++;
            p *= poissionGenerator.nextDouble();
            while (p < 1 && Lleft > 0) {
                if (Lleft > step) {
                    p *= Math.exp(step);
                    Lleft -= step;
                } else {
                    p *= Math.exp(Lleft);
                    Lleft = 0;
                }
            }
        } while (p > 1);
        return k - 1;
    }

    public int calcNumberOfSpacesLeft(TouristTime time) {
        int totalSpaces = 0;
        for (Event event : this.events.values()) {
            totalSpaces += event.getCapacityRestOfDay(time);
        }
        return totalSpaces;
    }




    public TouristTime getTimeLeftInDay(TouristTime currentTime) {
        return TouristTime.difference(TouristConstants.closeTime, currentTime);
    }
    public void generateWishLists() {
        //Use thurstonian model to create rankings
        Map<Event, Double> zMap = new HashMap<>();
        for (User user : this.users) {
            int i = 0;
            for (Event event : this.baseRanking) {
                zMap.put(event, 16 * TouristConstants.heterogeneity * generator.nextGaussian() + (1 - TouristConstants.heterogeneity) * 2 * this.baseRanking.indexOf(event));
                i++;
            }

            user.wishList = zMap.entrySet().stream()
                    .sorted(Comparator.comparing(Map.Entry::getValue, Comparator.naturalOrder()))
                    .map(Map.Entry::getKey)
                    .collect(Collectors.toList());

            for (Event event : user.wishList) {
                user.scoreFunction.put(event, Math.pow(10 * Math.exp(Math.pow(user.wishList.indexOf(event) + 1.0, 2) / (-TouristConstants.BETA)), 2));
            }
        }

    }


    public Double calculateNumberOfUsersStillTooComeAtBatch(Batch batch, TouristTime currentTime) {
        double numberOfUsersToCome = 0;
            if (TouristConstants.poisson.equals("inhomogeneous")) {
                for (int hour = currentTime.hour; hour < 24; hour++) {
                    if (hour == currentTime.hour) {
                        numberOfUsersToCome += (double) (60 - currentTime.minute) / 60.0 * TouristConstants.lambda * TouristConstants.arrivalRate[hour] / 100.0;
                    } else {
                        numberOfUsersToCome += TouristConstants.lambda * TouristConstants.arrivalRate[hour] / 100.0;
                    }
                }
            } else {//TODO: CHANGED PARAMETERS OF TIMING!!! CHANGE!!!
                numberOfUsersToCome += TouristConstants.lambda * (TouristConstants.endArrivals.toMinutes() - currentTime.toMinutes()) / (TouristConstants.endArrivals.toMinutes() - TouristConstants.startArrivals.toMinutes());
            }
        return numberOfUsersToCome;
    }

    public Double calculateNumberOfUsersStillTooComeAtBatchMoreSophisticated(Batch batch, TouristTime currentTime) {
        double numberOfUsersToCome = 0;
        if (TouristConstants.poisson.equals("inhomogeneous")) {
            for (int hour = currentTime.hour; hour <= batch.startTime.hour; hour++) {
                if (hour == currentTime.hour) {
                    numberOfUsersToCome += (double) (60 - currentTime.minute) / 60.0 * TouristConstants.lambda * TouristConstants.arrivalRate[hour] / 100.0;
                }else if(hour== batch.startTime.hour){
                    numberOfUsersToCome += (double) (batch.startTime.minute) / 60.0 * TouristConstants.lambda * TouristConstants.arrivalRate[hour] / 100.0;
                }else {
                    numberOfUsersToCome += TouristConstants.lambda * TouristConstants.arrivalRate[hour] / 100.0;
                }
            }
        } else {//TODO: CHANGED PARAMETERS OF TIMING!!! CHANGE!!!
            numberOfUsersToCome += TouristConstants.lambda * (batch.startTime.toMinutes() - currentTime.toMinutes()) / (TouristConstants.endArrivals.toMinutes() - TouristConstants.startArrivals.toMinutes());
        }
        return numberOfUsersToCome;
    }
    public double calculateProbabilityOfEventRankingHigherThanK(Event event, int ranking){
        double probability = 0.0;
        for(int i = 1; i <= ranking; i++){
            double addition = 1.0 /this.baseRanking.size() + 1.0/ (this.baseRanking.size()-1.0) * (this.baseRanking.indexOf(event)+1-this.baseRanking.size()/2.0)/TouristConstants.sigma * mu(i,this.baseRanking.size());
            if(addition > 0){
                probability += addition;
            }
        }

        if(probability< 0){
            return 0;
        }
        if(probability>1){
            return 1;
        }
        return probability;
    }

    public static double mu(int i,  int n){
        double mean = 0.0;
        double stDev = 1.0;
        NormalDistribution distribution = new NormalDistribution(mean,stDev);
        double input = (i - (3.0/8.0))/(n+1.0/4.0);
        return distribution.inverseCumulativeProbability(input);
    }
    public void generateUsers(int numberOfUsers) {
        Random generator = new Random(TouristConstants.seed);
        for (int i = 0; i < numberOfUsers; i++) {
            User user = new User();
            user.name = Integer.toString(i);
            user.groupSize = (1);
            user.start = this.getRandomOvernightLocation();
            user.end = this.getRandomOvernightLocation();
            user.startEvent = new Event();
            user.endEvent = new Event();
            user.startEvent.exit = user.start;
            user.endEvent.entrance = user.end;
            user.startTime = generateTimeUniform();

            user.queryTime = generateQuery();
            if(TouristTime.greater(user.queryTime,user.startTime)){
                user.startTime= user.queryTime;
            }

            user.endTime.day = TouristConstants.dayOfEvents;
            user.endTime.rebalance();
            //user.groupSize - generator.nextInt(user.groupSize)
            for (Event event : this.events.values()) {
                user.groupSizePerEvent.put(event, user.groupSize);
            }
            this.users.add(user);

        }
        this.users.sort((o1, o2) -> {
            if (TouristTime.greater(o1.queryTime, o2.queryTime)) {
                return 1;
            }
            if (TouristTime.less(o1.queryTime, o2.queryTime)) {
                return -1;
            }
            return 0;
        });
    }

    public void generateUserInhomogeneous() {
        Random generator = new Random(TouristConstants.seed);
        for (int hour = 0; hour < 24; hour++) {
            numberOfUsers = poissonRNG(TouristConstants.lambda * TouristConstants.arrivalRate[hour] / 100);
            for (int i = 0; i < numberOfUsers; i++) {
                User user = new User();
                user.name = hour + "_" + i;
                user.groupSize = (1);
                user.start = this.getRandomOvernightLocation();
                user.end = this.getRandomOvernightLocation();
                user.startEvent = new Event();
                user.endEvent = new Event();
                user.startEvent.exit = user.start;
                user.endEvent.entrance = user.end;
                user.startTime = generateTimeUniformHour(hour).increaseBy(new TouristTime(TouristConstants.dayOfEvents + ":0:0"));
                user.queryTime = user.startTime; //TODO Make these separate
                //user.groupSize - generator.nextInt(user.groupSize)
                for (Event event : this.events.values()) {
                    user.groupSizePerEvent.put(event, user.groupSize);
                }
                this.users.add(user);

            }
        }
        this.users.sort((o1, o2) -> {
            if (TouristTime.greater(o1.queryTime, o2.queryTime)) {
                return 1;
            }
            if (TouristTime.less(o1.queryTime, o2.queryTime)) {
                return -1;
            }
            return 0;
        });
    }

    private TouristTime generateTimeUniformHour(int hour) {
        double number;
        number = hour * 60 + generator.nextDouble() * 60;
        TouristTime result = new TouristTime("0");
        result.minute = (int) Math.floor(number);
        result.rebalance();
        return result;
    }

    public void bookBatches(User user, List<Batch> batches) {
        for (Batch batch : batches) {
            if (!batch.bookGroup(user)) {
                System.err.println("BOOKING WENT WRONG!");
            }
        }
    }

    public void unbookBatches(User user, List<Batch> batches) {
        for (Batch batch : batches) {
            if (!batch.unbookGroup(user)) {
                System.err.println("BOOKING WENT WRONG!");
            }
        }
    }
}

