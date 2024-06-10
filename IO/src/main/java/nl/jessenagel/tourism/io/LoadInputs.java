package nl.jessenagel.tourism.io;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.*;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import nl.jessenagel.tourism.framework.*;
public class LoadInputs {

    public static void readParameterFile(String fileLocation){
        JSONParser jsonParser = new JSONParser();
        try (FileReader reader = new FileReader(fileLocation))
        {
            //Read JSON file
            Object obj = jsonParser.parse(reader);

            JSONObject parameters = (JSONObject) obj;
            try {
                TouristConstants.folder = (String) parameters.get("folder");
                TouristConstants.inputFolder = (String) parameters.get("inputFolder");
                TouristConstants.version = (String) parameters.get("version");
                TouristConstants.algorithm = (String) parameters.get("algorithm");
                TouristConstants.distanceType = (String) parameters.get("distanceType");
                TouristConstants.WEIGHT_1 = (Double) parameters.get("weightOne");
                TouristConstants.WEIGHT_2 = (Double) parameters.get("weightTwo");
                TouristConstants.openTime = new TouristTime((String) parameters.get("openTime"));
                TouristConstants.closeTime = new TouristTime((String) parameters.get("closeTime"));
                TouristConstants.startArrivals = new TouristTime((String) parameters.get("startArrivals"));
                TouristConstants.endArrivals = new TouristTime((String) parameters.get("endArrivals"));
                TouristConstants.dayOfEvents = Math.toIntExact((long) parameters.get("dayOfEvents"));
                TouristConstants.BETA = (Double) parameters.get("beta");
                TouristConstants.lambda = (Double) parameters.get("lambda");
                TouristConstants.useBlocking = (Boolean) parameters.get("useBlocking");
                TouristConstants.scoreFunction = (String) parameters.get("scoreFunction");
                TouristConstants.method = (String) parameters.get("method");
                TouristConstants.restrictNumberOfActivities = (Boolean) parameters.get("restrictActivities");
                TouristConstants.heterogeneity = (double) parameters.get("heterogeneity");
                TouristConstants.travelPenalty = (Boolean) parameters.get("travelPenalty");
                TouristConstants.poisson = (String) parameters.get("poisson");
                TouristConstants.capacityMultiplier = (double) parameters.get("capacityMultiplier");
                TouristConstants.arrivalProcess = (String) parameters.get("arrivalProcess");
                TouristConstants.strictness = (Double) parameters.get("strictness");
            }catch(NullPointerException e){
                System.err.println("Missing parameter, using defaults");
            }

        } catch (FileNotFoundException | ParseException e) {
            e.printStackTrace();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
    public static void readLocations(Area area) {
        File locationFile = new File(TouristConstants.inputFolder + "locations" + TouristConstants.index);

        Scanner locationFileScanner;
        try {
            locationFileScanner = new Scanner(locationFile);
            while (locationFileScanner.hasNextLine()) {
                Scanner locationLineScanner = new Scanner(locationFileScanner.nextLine());
                locationLineScanner.useDelimiter(";");
                Location location = new Location();
                location.name = locationLineScanner.next();
                location.latitude = locationLineScanner.nextDouble();
                location.longitude = locationLineScanner.nextDouble();
                if (location.name.replaceAll("\\d", "").equals("Hotel")||location.name.replaceAll("\\d", "").equals("hotel")) {
                    location.isOvernightLocation = true;
                }
                area.locations.put(location.name, location);
            }
            area.setOvernightLocations();

        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
    }

    public static void readDistances(Area area) {
        if (TouristConstants.distanceType.equals("file")) {
            File distanceFile = new File(TouristConstants.inputFolder + "distancesnew");
            for (Location location : area.locations.values()) {
                area.travelTimes.put(location, new HashMap<>());
                area.travelTimes.get(location).put(location, new TouristTime("0"));
            }
            try {
                Scanner distanceFileScanner = new Scanner(distanceFile);
                while (distanceFileScanner.hasNextLine()) {
                    Scanner distanceLineScanner = new Scanner(distanceFileScanner.nextLine());
                    distanceLineScanner.useDelimiter(";");
                    String from = distanceLineScanner.next();
                    String to = distanceLineScanner.next();
                    String time = distanceLineScanner.next();
                    String timeString = time.split("\\.")[0];
                    double timeValue = Integer.parseInt(timeString);
                    timeValue /= 60.0;
                        if(timeValue== 0){
                        timeValue =1.0;
                    }
                    timeString = String.valueOf((int) Math.floor(timeValue));
                    area.travelTimes.get(area.locations.get(from)).put(area.locations.get(to), new TouristTime(timeString));
                }
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            }
        } else if (TouristConstants.distanceType.equals("euclidean")) {
            for (Location from : area.locations.values()) {
                area.travelTimes.put(from, new HashMap<>());
                for (Location to : area.locations.values()) {
                    if (from == to) {
                        area.travelTimes.get(from).put(from, new TouristTime("0"));
                    } else {
                        TouristTime time = new TouristTime("0");
                        time.minute = (int) Math.ceil(Math.sqrt(Math.pow(from.latitude - to.latitude, 2) + Math.pow(from.longitude - to.longitude, 2)));
                        time.rebalance();
                        area.travelTimes.get(from).put(to, time);
                    }
                }
            }
        } else {
            System.err.println("Unknown distance touristType given as parameter");
            System.exit(5);
        }
    }

    public static void readEvents(Area area) {
        File eventFile = new File(TouristConstants.inputFolder + "events");

        try {
            Scanner eventFileScanner = new Scanner(eventFile);
            while (eventFileScanner.hasNextLine()) {
                Scanner eventLineScanner = new Scanner(eventFileScanner.nextLine());
                eventLineScanner.useDelimiter(";");
                Event event = new Event();
                event.name = eventLineScanner.next();
                event.entrance = event.exit = area.locations.get(eventLineScanner.next());
                event.capacity = (int) Math.ceil((double) eventLineScanner.nextInt() * TouristConstants.capacityMultiplier * 1);
                event.length = new TouristTime(eventLineScanner.next());
                event.first = new TouristTime(eventLineScanner.next());
                event.first.day += TouristConstants.dayOfEvents;

                String lastString = eventLineScanner.next();
                if (lastString.equals("NA")) {
                    event.singleBatch = true;
                } else {
                    event.last = new TouristTime(lastString);
                    event.last.day += TouristConstants.dayOfEvents;
                    event.every = new TouristTime(eventLineScanner.next());
                    event.batchCapacity = (int) Math.ceil((double) eventLineScanner.nextInt() * TouristConstants.capacityMultiplier);
                    event.singleBatch = false;
                }
                area.events.put(event.name, event);
                event.entrance.events.put(event.name, event);
            }


        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
    }

    public static void importUsers(Area area) {
        try {
            File input = new File(TouristConstants.inputFolder + "users0.dat");
            Scanner userScanner = new Scanner(input);
            while (userScanner.hasNextLine()) {
                Scanner lineScanner = new Scanner(userScanner.nextLine());
                lineScanner.useDelimiter(";");
                User user = new User();
                user.name = lineScanner.next();
                user.queryTime = new TouristTime(lineScanner.next());
                user.startTime = new TouristTime(lineScanner.next());
                user.endTime = new TouristTime(lineScanner.next());
                user.wishList = new ArrayList<>();
                user.start = area.locations.get(lineScanner.next());
                user.startEvent = new Event();
                user.startEvent.exit = user.start;
                user.end = area.locations.get(lineScanner.next());
                user.endEvent = new Event();
                user.endEvent.entrance = user.end;
                int i = 0;

                while (lineScanner.hasNext()) {
                    Event event = area.events.get(lineScanner.next());
                    if (event == null) {
                        continue;
                    }
                    user.wishList.add(event);
                    user.groupSizePerEvent.put(event, lineScanner.nextInt());
                    i++;
                    if (i >= area.lengthOfWishlist) {
                        break;
                    }
                }
                area.users.add(user);
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        Collections.shuffle(area.users, new Random(TouristConstants.seed));
        area.users = area.users.subList(0, area.numberOfUsers);
    }

    public static void readTypes(Area area) {
        try {
            File input = new File(TouristConstants.inputFolder + "types");
            Scanner userScanner = new Scanner(input);
            while (userScanner.hasNextLine()) {
                Scanner lineScanner = new Scanner(userScanner.nextLine());
                lineScanner.useDelimiter(";");
                TouristType touristType = new TouristType();
                touristType.name = lineScanner.next();
                File rankingFile =  new File(TouristConstants.inputFolder + "ranking_" + touristType.name + TouristConstants.index);
                Scanner rankingLineScanner = new Scanner(rankingFile);
                while(rankingLineScanner.hasNextLine()){
                    touristType.baseRanking.add(area.events.get(rankingLineScanner.nextLine()));
                }
                while (lineScanner.hasNext()) {
                    touristType.bonusEvents.add(lineScanner.next());
                }
                area.touristTypes.add(touristType);
                try {
                    touristType.probability = TouristConstants.probabilitiesOfTypes[area.touristTypes.indexOf(touristType)];
                }catch (IndexOutOfBoundsException e){
                    System.err.println("Not enough probabilities for the touristtypes.");
                }
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
    }
}
