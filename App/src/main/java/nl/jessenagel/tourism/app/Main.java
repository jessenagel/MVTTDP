package nl.jessenagel.tourism.app;
import nl.jessenagel.tourism.framework.*;
import nl.jessenagel.tourism.problemformulators.*;
import nl.jessenagel.tourism.io.*;
public class Main {


    public static void main(String[] args) {
        if (args.length != 2) {
            System.out.println("Usage: java main <seed(integer)> <parameterfile>");
            System.out.println("args.length=" + args.length);
            System.exit(2);
        }
        TouristConstants.seed = Integer.parseInt(args[0]);
        LoadInputs.readParameterFile(args[1]);
        if ((TouristConstants.inputFolder.equals("inputfiles/amsterdam/") && !TouristConstants.distanceType.equals("file")) || (TouristConstants.inputFolder.equals("inputfiles/solvable/") && !TouristConstants.distanceType.equals("euclidean"))) {
            System.err.println("ARE YOU SURE ABOUT THE DISTANCE TYPE!?");
            System.exit(5);
        }
        MVTTDP MVTTDP = new MVTTDP();
        MVTTDP.create();
        System.out.println(MVTTDP.area.users.size());
        MVTTDP.run();
    }
}
