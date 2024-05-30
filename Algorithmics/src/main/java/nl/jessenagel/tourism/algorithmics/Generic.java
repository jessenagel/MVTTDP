package nl.jessenagel.tourism.algorithmics;

import java.util.List;
import nl.jessenagel.tourism.framework.*;

public class Generic {

    static public double calcScore(List<Batch> batches, User user, Area area) {
        double score = 0.0;
        Batch previousBatch = null;
        for (Batch batch : batches) {
            if (TouristConstants.travelPenalty) {
                if (previousBatch == null) {
                    score += user.scoreFunction.get(batch.event) + area.travelTimes.get(user.start).get(batch.event.entrance).toMinutes() * TouristConstants.WEIGHT_2;
                } else {
                    score += user.scoreFunction.get(batch.event) + area.travelTimes.get(previousBatch.event.exit).get(batch.event.entrance).toMinutes() * TouristConstants.WEIGHT_2;

                }
            } else {
                if (previousBatch == null) {
                    score += user.scoreFunction.get(batch.event);
                } else {
                    score += user.scoreFunction.get(batch.event);
                }
            }

            previousBatch = batch;
        }
        if (previousBatch != null&& TouristConstants.travelPenalty) {
            score += area.travelTimes.get(previousBatch.event.exit).get(user.end).toMinutes() * TouristConstants.WEIGHT_2;
        }
        return score;
    }


}
