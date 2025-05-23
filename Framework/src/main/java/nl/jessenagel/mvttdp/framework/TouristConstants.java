package nl.jessenagel.mvttdp.framework;

public final class TouristConstants {
    public static double lambda = 10;
    public static int seed = 4;
    public static double BETA = 40;
    public static String algorithm = "ILS";
    public static boolean useBlocking = true; //Block fraction slots until timeBlock hours before event, yes or no
    public static String folder = "/export/scratch1/jmn/IdeaProjects/TouristTrip/";
    public static String inputFolder = "/export/scratch1/jmn/IdeaProjects/TouristTrip/inputfiles/amsterdam/";

    public static String distanceType = "file"; //Either Euclidean distance for simple coordinates or 'file' if read from file.
    public static double WEIGHT_1 = 1.0;
    public static double WEIGHT_2 = -1.0;
    public static TouristTime openTime = new TouristTime("00:07:00");
    public static TouristTime closeTime = new TouristTime("00:16:00");

    public static String scoreFunction = "product"; //product, sum or maximin.
    public static String method = "MIP"; //MIP or online
    public static boolean restrictNumberOfActivities = true;
    public static double heterogeneity = 0.5;
    public static Boolean travelPenalty = true;
    public static final int[] arrivalRate = {0, 0, 0, 0, 0, 0, 0, 8, 15, 25, 15, 11, 7, 6, 5, 4, 3, 0, 0, 0, 0, 0, 0, 0};
//    public static int[] arrivalRate = {0, 0, 0, 0, 0, 0, 0, 3, 4, 5, 6, 7, 11, 15, 25, 15, 8, 0, 0, 0, 0, 0, 0, 0};

    public static String poisson = "homogeneous";
    public static TouristTime startArrivals = new TouristTime("00:07:00");
    public static TouristTime endArrivals = new TouristTime("00:16:00");
    public static int dayOfEvents = 0;
    public static double capacityMultiplier = 1.0;
    public static double strictness = 1.0;
    public static final double threshold = 2;

    public static String arrivalProcess = "during"; //During, before,combined
    public static int index = 1;
    public static final double sigma = 4.0;

    public static double MIPGap = 0.001;

    public static String experimentID = "None";
    public static String scratchDir = ".";
    private TouristConstants() {
    }
}
