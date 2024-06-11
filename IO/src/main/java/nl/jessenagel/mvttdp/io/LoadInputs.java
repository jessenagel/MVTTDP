package nl.jessenagel.mvttdp.io;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.*;

import org.json.*;


import nl.jessenagel.mvttdp.framework.*;

public class LoadInputs {

    public static void readParameterFile(String fileLocation) {
        try (FileReader reader = new FileReader(fileLocation)) {
            //Read JSON file
            JSONTokener jsonTokener = new JSONTokener(reader);

            JSONObject parameters = new JSONObject(jsonTokener);
            try {
                TouristConstants.folder = parameters.getString("folder");
                TouristConstants.inputFolder = parameters.getString("inputFolder");
                TouristConstants.algorithm = parameters.getString("algorithm");
                TouristConstants.distanceType = parameters.getString("distanceType");
                TouristConstants.WEIGHT_1 = parameters.getDouble("weightOne");
                TouristConstants.WEIGHT_2 = parameters.getDouble("weightTwo");
                TouristConstants.openTime = new TouristTime(parameters.getString("openTime"));
                TouristConstants.closeTime = new TouristTime(parameters.getString("closeTime"));
                TouristConstants.startArrivals = new TouristTime(parameters.getString("startArrivals"));
                TouristConstants.endArrivals = new TouristTime(parameters.getString("endArrivals"));
                TouristConstants.dayOfEvents = Math.toIntExact( parameters.getLong("dayOfEvents"));
                TouristConstants.BETA = parameters.getDouble("beta");
                TouristConstants.lambda = parameters.getDouble("lambda");
                TouristConstants.useBlocking = parameters.getBoolean("useBlocking");
                TouristConstants.scoreFunction = parameters.getString("scoreFunction");
                TouristConstants.method = parameters.getString("method");
                TouristConstants.restrictNumberOfActivities = parameters.getBoolean("restrictActivities");
                TouristConstants.heterogeneity = parameters.getDouble("heterogeneity");
                TouristConstants.travelPenalty = parameters.getBoolean("travelPenalty");
                TouristConstants.poisson = parameters.getString("poisson");
                TouristConstants.capacityMultiplier = parameters.getDouble("capacityMultiplier");
                TouristConstants.arrivalProcess = parameters.getString("arrivalProcess");
                TouristConstants.strictness = parameters.getDouble("strictness");
                TouristConstants.experimentID = parameters.getString("experimentID");
            } catch (NullPointerException e) {
                System.err.println("Missing parameter, using defaults");
            }

        } catch (FileNotFoundException e) {
            System.err.println("Did not find file: " + fileLocation);
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
                if (location.name.replaceAll("\\d", "").equals("Hotel") || location.name.replaceAll("\\d", "").equals("hotel")) {
                    location.isOvernightLocation = true;
                }
                area.locations.put(location.name, location);
            }
            area.setOvernightLocations();

        } catch (FileNotFoundException e) {
            System.err.println("Did not find file: " + TouristConstants.inputFolder + "locations" + TouristConstants.index);

        }
    }

    public static void readDistances(Area area) {
        if (TouristConstants.distanceType.equals("file")) {
            File distanceFile = new File(TouristConstants.inputFolder + "distances");
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
                    if (timeValue == 0) {
                        timeValue = 1.0;
                    }
                    timeString = String.valueOf((int) Math.floor(timeValue));
                    area.travelTimes.get(area.locations.get(from)).put(area.locations.get(to), new TouristTime(timeString));
                }
            } catch (FileNotFoundException e) {
                System.err.println("Did not find file: " + TouristConstants.inputFolder + "distances");

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
            System.err.println("Did not find file: " + TouristConstants.inputFolder + "events");
        }
    }

    public static void readRanking(Area area) {
        try {
            File rankingFile = new File(TouristConstants.inputFolder + "ranking" + TouristConstants.index);
            Scanner rankingLineScanner = new Scanner(rankingFile);
            while (rankingLineScanner.hasNextLine()) {
                area.baseRanking.add(area.events.get(rankingLineScanner.nextLine()));
            }
        } catch (FileNotFoundException e) {
            System.err.println("Did not find file: " + TouristConstants.inputFolder + "ranking_" + TouristConstants.index);
        }
    }
}
