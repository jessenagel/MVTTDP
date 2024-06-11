package nl.jessenagel.mvttdp.io;

import org.json.*;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.Format;
import java.text.SimpleDateFormat;
import java.util.Date;

import nl.jessenagel.mvttdp.framework.*;
public class WriteOutputs {
    private static final String folder = TouristConstants.folder;

    public static void exportUsers(Area area) {
        try {
            FileWriter output = new FileWriter(folder + "inputfiles/userfiles/users0.dat");
            for (User user : area.users) {
                output.write(user.name + ";");
                output.write(user.queryTime.toString() + ";");
                output.write(user.startTime.toString() + ";");
                output.write(user.endTime.toString() + ";");
                output.write(user.start.name + ";");
                output.write(user.end.name);
                for (Event event : user.wishList) {
                    output.write(";" + event.name);
                    output.write(";" + user.groupSizePerEvent.get(event));

                }
                output.write("\n");
                output.flush();
            }
            output.close();
        } catch (IOException e) {
            System.err.println("IOException");

        }
    }
    public static void exportHappiness(Area area, String algorithm) {
        try {
            Date date = new Date();
            Format formatter = new SimpleDateFormat("yyyy-MM-dd_HH:mm:ss:SS");
            String s = formatter.format(date);
            FileWriter out = new FileWriter(folder + "outputfiles/results/happiness" + s + ".result");
            out.write("Seed:" + TouristConstants.seed);
            out.write(algorithm + "\n");
            double sum = 0;
            int numberOfZeros = 0;
            boolean firstUser = true;
            for (User user : area.users) {
                switch (TouristConstants.scoreFunction){
                    case "product":
                        sum *=user.happiness;
                        break;
                    case "sum":
                        sum +=user.happiness;
                        break;
                    case "maximin":
                        if(user.happiness < sum || firstUser){
                            sum = user.happiness;
                            firstUser = false;
                        }
                        break;
                }
                if (user.happiness == 0) {
                    numberOfZeros++;
                }
            }
            double mean = 1 / (double) area.users.size() * sum;
            double stDevSum = 0;
            for (User user : area.users) {
                stDevSum += Math.pow(user.happiness - mean, 2);
            }
            double stDev = Math.sqrt(1 / (double) (area.users.size() - 1) * stDevSum);
            out.write(sum + "\n");
            out.write(mean + "\n");
            out.write(stDev + "\n");
            out.write(numberOfZeros + "\n");
            for (User user : area.users) {
                out.write(user.happiness + "\n");
            }
            out.flush();
            out.close();
        } catch (IOException e) {
            System.err.println("IOException");
        }
    }

    public static void exportJson(Area area,long runTime){
        Date date = new Date();
        Format formatter = new SimpleDateFormat("yyyy-MM-dd_HH:mm:ss:SS");
        String s = formatter.format(date);
        double sum = 0;
        int numberOfZeros = 0;
        boolean firstUser = true;
        for (User user : area.users) {
            switch (TouristConstants.scoreFunction){
                case "product":
                    sum +=Math.log(user.happiness);
                    break;
                case "sum":
                    sum +=user.happiness;
                    break;
                case "maximin":
                    if(user.happiness < sum || firstUser){
                        sum = user.happiness;
                        firstUser = false;
                    }
                    break;
            }
            if (user.happiness == 0) {
                numberOfZeros++;
            }
        }

        for(Event event : area.events.values()){
            System.out.println(event.name);
            System.out.println(event.getCapacityFromTill(TouristConstants.openTime,TouristConstants.closeTime));

            for(Batch batch: event.batches){
                System.out.println("start time");
                batch.startTime.print();
                System.out.println("Capacity " + batch.getCurrentCapacity());
            }
        }
        double totalScheduleLength = 0;
        for(User user : area.users){
            System.out.println("_________________________________");
            System.out.println("Name: " + user.name);
            System.out.println("Querytime");
            user.queryTime.print();
            System.out.println("arrival time");
            user.startTime.print();
            System.out.println("end time");
            user.endTime.print();
            System.out.println("Schedule size: " + user.schedule.size());
            user.printSchedule();
            System.out.println("Schedule score: " + user.happiness);
            totalScheduleLength += user.schedule.size();
        }
        System.out.println("Average schedule size" + totalScheduleLength/area.users.size());
        switch (TouristConstants.scoreFunction) {
            case "product":
                System.out.println(Math.exp(sum / area.users.size()));
                break;
            case "sum":
                System.out.println(sum / area.users.size());
                break;
            case "maximin":
                System.out.println(sum);
                break;
        }
        System.out.println("Number of zeroes:" + numberOfZeros);
        JSONObject results = getJsonObject(area, runTime);
        new File("outputfiles/results/"+TouristConstants.experimentID).mkdirs();
        try (FileWriter file = new FileWriter(folder+"outputfiles/results/"+TouristConstants.experimentID+"/results"+s+".json")) {
            //We can write any JSONArray or JSONObject instance to the file
            file.write(results.toString(4));
            file.flush();

        } catch (IOException e) {
            System.err.println("IOException");
        }

    }

    private static JSONObject getJsonObject(Area area, long runTime) {
        JSONObject results = new JSONObject();
        results.put("seed",TouristConstants.seed);
        results.put("b0.jseta",TouristConstants.BETA);
        results.put("lambda",TouristConstants.lambda);
        results.put("weight1",TouristConstants.WEIGHT_1);
        results.put("weight2",TouristConstants.WEIGHT_2);
        results.put("runTime", runTime);
        results.put("heterogeneity",TouristConstants.heterogeneity);
        results.put("strictness",TouristConstants.strictness);
        results.put("threshold",TouristConstants.threshold);
        results.put("capacityMultiplier",TouristConstants.capacityMultiplier);
        results.put("index",TouristConstants.index);

        JSONArray scores = new JSONArray();
        JSONArray times = new JSONArray();

        for(User user: area.users){
            scores.put(user.happiness);
            times.put(user.runTime);
        }
        results.put("scores",scores);
        results.put("times",times);
        return results;
    }

    public static void exportUtilization(Area area ){
            Date date = new Date();
            Format formatter = new SimpleDateFormat("yyyy-MM-dd_HH:mm:ss:SS");
            String s = formatter.format(date);
        try {
            FileWriter out = new FileWriter(folder + "outputfiles/utilizations/utilization" + s + ".result");
            for(Event event: area.events.values()){
                for(Batch batch : event.batches){
                    out.write(batch.event.name+";"+batch.startTime.toMinutes()+ ";" + batch.getCurrentCapacity()+";"+batch.capacity+"\n");
                }
            }
            out.flush();
            out.close();

        } catch (IOException e) {
            System.err.println("IOException");
        }

    }
}
