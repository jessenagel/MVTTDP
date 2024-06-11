package nl.jessenagel.mvttdp.framework;

import java.util.Comparator;

public class BatchComparator implements Comparator<Batch> {
    @Override
    public int compare(Batch one, Batch two) {
        if (TouristTime.eq(one.endTime, two.endTime)) {
            return 0;
        } else if (TouristTime.greater(one.endTime, two.endTime)) {
            return 1;
        } else {
            return -1;
        }
    }
}
