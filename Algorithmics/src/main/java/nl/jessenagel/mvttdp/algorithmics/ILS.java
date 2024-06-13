package nl.jessenagel.mvttdp.algorithmics;


import java.util.ArrayList;
import java.util.List;

import nl.jessenagel.mvttdp.framework.*;

public class ILS {
    private static final int ITERATIONS = 100;
    public static double MIN_IMPROVEMENT = 1.0;
    public User user;
    public Area area;
    static final int MAX_DEPTH = 7;
    static final int MAX_NO_IMPROVE = 100;
    public int maxLength;



    public boolean insert(List<Batch> batches, int start, Batch end, double minProfit, int maxDepth) {

        if (maxDepth <= 0) {
            return false;
        }
        if (!batches.isEmpty()) {
            if (batches.size() > this.maxLength || TouristTime.geq(batches.get(batches.size() - 1).endTime.increaseBy(area.travelTimes.get(batches.get(batches.size() - 1).event.exit).get(user.end)), user.endTime)) {
                return false;
            }
        }
        for (Event event : area.events.values()) {
            boolean alreadyVisited = false;
            for (Batch batch : batches) {
                if (batch.event == event) {
                    alreadyVisited = true;
                    break;
                }
            }
            if (alreadyVisited) {
                continue;
            }
            Batch nextBatch;
            if (start == 0) {

                nextBatch = FullEnumeration.firstBatch(user.startTime.increaseBy(area.travelTimes.get(user.start).get(event.entrance)), event, user);


            } else {
                nextBatch = FullEnumeration.firstBatch(batches.get(start - 1).endTime.increaseBy(area.travelTimes.get(batches.get(start - 1).event.exit).get(event.entrance)), event, user);
            }

            if (nextBatch != null) {
                if (end == null) {
                    if (TouristTime.greater(nextBatch.endTime.increaseBy(area.travelTimes.get(event.exit).get(this.user.end)), this.user.endTime)) {
                        continue;
                    }
                } else {
                    if (TouristTime.greater(nextBatch.endTime.increaseBy(area.travelTimes.get(event.exit).get(end.event.entrance)), end.startTime)) {
                        continue;
                    }
                }

                batches.add(start, nextBatch);

                if (Generic.calcScore(batches, user, area) > minProfit) {
                    return true;
                }

                if (insert(batches, batches.indexOf(nextBatch) + 1, end, minProfit, maxDepth - 1)) {
                    return true;
                }
                batches.remove(nextBatch);
            }
        }

        return false;
    }

    public List<Batch> solve(List<Event> eventsToCheck) {
        int S = 0;
        int R = 1;
        int noImprove = 0;
        double bestScore = 0;
        double score;

        List<Batch> solution = gapInsert(new ArrayList<>(), eventsToCheck);
        List<Batch> bestSolution = new ArrayList<>(solution);

        for (int i = 0; i < ITERATIONS; i++) {
            if (noImprove > MAX_NO_IMPROVE) {
                break;
            }

            noImprove++;
            solution = gapInsert(solution, eventsToCheck);

            score = Generic.calcScore(solution, user, this.area);

            if (score > bestScore) {
                bestSolution = new ArrayList<>(solution);

                bestScore = score;
                S = 0;
                R = 1;
                noImprove = 0;
            }
            if (solution.isEmpty()) {
                return solution;
            }
            while (S >= solution.size()) {
                S -= solution.size();
            }
            if (S + R >= solution.size()) {
                R = 1;
            }

            shake(solution, S, R);

            S += R;
            R++;

        }
        return bestSolution;
    }

    private void shake(List<Batch> solution, int s, int r) {
        if (s + r > s) {
            solution.subList(s, s + r).clear();
        }
        for (int i = 0; i < solution.size(); i++) {
            Batch batch = solution.get(i);
            if (solution.indexOf(batch) >= s) {
                if (solution.indexOf(batch) == 0) {
                    Batch earlierBatch = FullEnumeration.firstBatch(this.user.startTime.increaseBy(area.travelTimes.get(user.start).get(batch.event.entrance)), batch.event, user);
                    if (earlierBatch == null) {
                        continue;
                    }
                    if (TouristTime.less(earlierBatch.startTime, batch.startTime)) {
                        solution.add(solution.indexOf(batch), earlierBatch);
                        solution.remove(batch);
                    }
                } else {
                    Batch earlierBatch = FullEnumeration.firstBatch(solution.get(solution.indexOf(batch) - 1).endTime.increaseBy(area.travelTimes.get(solution.get(solution.indexOf(batch) - 1).event.exit).get(batch.event.entrance)), batch.event, user);
                    if (earlierBatch == null) {
                        continue;
                    }
                    if (TouristTime.less(earlierBatch.startTime, batch.startTime)) {
                        solution.add(solution.indexOf(batch), earlierBatch);
                        solution.remove(batch);
                    }
                }
            }
        }
    }

    public List<Batch> gapInsert(List<Batch> solution, List<Event> eventsToCheck) {
        List<Batch> tempSolution = new ArrayList<>(solution);
        List<Gap> gaps = this.updateGaps(tempSolution);
        for (Event event : eventsToCheck) {
            boolean inSolution = false;
            for (Batch batch : tempSolution) {
                if (batch.event == event) {
                    inSolution = true;
                    break;
                }
            }
            if (inSolution) {
                continue;
            }

            boolean inserted = false;

            for (Gap gap : gaps) {
                if (canFitInGap(gap, event)) {
                    Batch batch = findLeastCrowdedInGap(gap, event);

                    if (gap.startBatch != null) {
                        List<Batch> tempTempSolution = new ArrayList<>(tempSolution);
                        tempTempSolution.add(tempTempSolution.indexOf(gap.startBatch) + 1, batch);

                        if (Generic.calcScore(tempSolution, user, area) * MIN_IMPROVEMENT < Generic.calcScore(tempTempSolution, user, area)) {
                            tempSolution.add(tempSolution.indexOf(gap.startBatch) + 1, batch);
                        }
                    } else {
                        tempSolution.add(0, batch);
                    }
                    inserted = true;
                    break;
                }

            }
            if (inserted) {
                gaps = this.updateGaps(tempSolution);
            }

        }

        return tempSolution;
    }

    public boolean canFitInGap(Gap gap, Event event) {
        TouristTime minTime = gap.startTime.increaseBy(area.travelTimes.get(gap.startLocation).get(event.entrance));
        TouristTime maxTime = gap.endTime.decreaseBy(area.travelTimes.get(event.exit).get(gap.endLocation));
        for (Batch batch : event.batches) {
            if (TouristTime.leq(minTime, batch.startTime) && TouristTime.leq(batch.endTime, maxTime) && batch.sufficientCapacityForGroup(user)) {
                return true;
            }

        }
        return false;
    }

    public List<Gap> updateGaps(List<Batch> tempSolution) {
        List<Gap> gaps = new ArrayList<>();
        //set gaps
        for (Batch batch : tempSolution) {
            if (tempSolution.indexOf(batch) == 0) {
                Gap gap = new Gap();
                gap.startTime = user.startTime;
                gap.endTime = batch.startTime;
                gap.startLocation = user.start;
                gap.endLocation = batch.event.entrance;
                gap.endBatch = batch;
                gaps.add(gap);
            }
            if (tempSolution.indexOf(batch) == tempSolution.size() - 1) {
                Gap gap = new Gap();
                gap.startTime = batch.endTime;
                gap.endTime = user.endTime;
                gap.startLocation = batch.event.exit;
                gap.endLocation = user.end;
                gap.startBatch = batch;
                gaps.add(gap);
            }
            if (tempSolution.indexOf(batch) != 0 && tempSolution.indexOf(batch) != tempSolution.size() - 1) {
                Gap gap = new Gap();
                gap.startTime = batch.endTime;
                gap.endTime = tempSolution.get(tempSolution.indexOf(batch) + 1).startTime;
                gap.startLocation = batch.event.exit;
                gap.endLocation = tempSolution.get(tempSolution.indexOf(batch) + 1).event.entrance;
                gap.startBatch = batch;
                gap.endBatch = tempSolution.get(tempSolution.indexOf(batch) + 1);

                gaps.add(gap);
            }
        }
        if (gaps.isEmpty()) {
            Gap gap = new Gap();
            gap.startTime = user.startTime;
            gap.endTime = user.endTime;
            gap.startLocation = user.start;
            gap.endLocation = user.end;
            gaps.add(gap);
        }
        return gaps;
    }

    public Batch findLeastCrowdedInGap(Gap gap, Event event) {
        Batch leastCrowded = null;
        TouristTime minTime = gap.startTime.increaseBy(area.travelTimes.get(gap.startLocation).get(event.entrance));
        TouristTime maxTime = gap.endTime.decreaseBy(area.travelTimes.get(event.exit).get(gap.endLocation));
        int leastCrowdedCapacity = 0;
        for (Batch batch : event.batches) {
            if (TouristTime.leq(minTime, batch.startTime) && TouristTime.leq(batch.endTime, maxTime) && batch.sufficientCapacityForGroup(user)) {
                if (leastCrowded == null) {
                    leastCrowded = batch;
                    leastCrowdedCapacity = leastCrowded.getCurrentCapacity();
                } else if (batch.getCurrentCapacity() > leastCrowdedCapacity) {
                    leastCrowded = batch;
                    leastCrowdedCapacity = batch.getCurrentCapacity();
                }
            }
        }
        if (leastCrowded == null) {
            System.err.println("NO MORE SPACES?");
        }
        return leastCrowded;
    }
}
