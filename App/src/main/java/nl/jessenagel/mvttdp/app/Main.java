package nl.jessenagel.mvttdp.app;
import nl.jessenagel.mvttdp.framework.*;
import nl.jessenagel.mvttdp.problemformulators.*;
import nl.jessenagel.mvttdp.io.*;
public class Main {


    public static void main(String[] args) {
        if (args.length != 5) {
            System.out.println("Usage: java main <seed(integer)> <parameterfile> <index> <gap> <scratchDir>");
            System.out.println("args.length=" + args.length);
            System.exit(2);
        }
        TouristConstants.seed = Integer.parseInt(args[0]);
        TouristConstants.index = Integer.parseInt(args[2]);
        TouristConstants.MIPGap = Double.parseDouble(args[3]);
        TouristConstants.scratchDir = args[4];
        LoadInputs.readParameterFile(args[1]);
        if ((TouristConstants.inputFolder.equals("inputfiles/amsterdam/") && !TouristConstants.distanceType.equals("file")) || (TouristConstants.inputFolder.equals("inputfiles/solvable/") && !TouristConstants.distanceType.equals("euclidean"))) {
            System.err.println("ARE YOU SURE ABOUT THE DISTANCE TYPE!?");
            System.exit(5);
        }
        MVTTDP mvttdp = new MVTTDP();
        mvttdp.create();
        long time = System.nanoTime();
        mvttdp.run();
        time = System.nanoTime() - time;
        //print running time in seconds
        System.out.println("Running time: " + time / 1e9);


    }
}
