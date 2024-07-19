package nl.jessenagel.mvttdp.algorithmics;

import nl.jessenagel.mvttdp.framework.*;

import java.util.ArrayList;
import java.util.List;

public class ScoreGRASP {
    public static final int MAX_ITERATIONS = 30;
    public static final int RCL_SIZE = 10;
    public static final double ALPHA = 0.0;
    public static final double MIN_IMPROVEMENT = 1.1;
    public User user;
    public Area area;

    public List<Batch> solve(List<Event> wishlist) {
        List<List<Batch>> bestSolutions = new ArrayList<>();
        for (int i = 1; i <= MAX_ITERATIONS; i++) {
            List<Batch> solution = fuzzyGRASPConstructionPhase(wishlist);
            localSearch(solution, wishlist);
            bestSolutions.add(new ArrayList<>(solution));
        }
        //Sort solutions by length
        bestSolutions.sort((o1, o2) -> {
            int length1 = o1.size();
            int length2 = o2.size();
            return Integer.compare(length1, length2);
        });
        //Get shortest solution
        List<Batch> bestSolution = bestSolutions.get(0);
        //Only allow a solution to become the new best solution if it's the same length or at least MIN_IMPROVEMENT better
        for (List<Batch> solution : bestSolutions) {
            if ((solution.size() == bestSolution.size() && solutionScore(solution) > solutionScore(bestSolution)) || solutionScore(solution) > solutionScore(bestSolution) * MIN_IMPROVEMENT) {
                bestSolution = solution;
            }
        }
//                for (List<Batch> solution : bestSolutions) {
//            if ( solutionScore(solution) > solutionScore(bestSolution) ) {
//                bestSolution = solution;
//            }
//        }
        return bestSolution;
    }

    private double solutionScore(List<Batch> solution ){
        double score = 0;
        for(Batch batch :solution){
            score += user.scoreFunction.get(batch.event);
        }
        return score;
    }
    private List<Batch> fuzzyGRASPConstructionPhase(List<Event> wishlist) {
        List<Batch> partialSolution = new ArrayList<>();
        List<Triplet> candidateList = new ArrayList<>();
        while (true) {
            candidateList.clear();
            for (Event event : wishlist) {
                //check if schedule contains this event:
                if (partialSolution.stream().anyMatch(batch -> batch.event == event)) {
                    continue;
                }
                //calculate f
                for (Batch batch : event.batches) {

                    if (!batch.sufficientCapacityForGroup(user) || batch.blockList.contains(user) || event.blockList.contains(user)) {
                        continue;
                    }
                    if (partialSolution.isEmpty()) {
                        //If solution is empty, we calculate f from and to the start and end of the user respectively.
//                        double f = area.travelTimes.get(event.exit).get(user.start).toMinutes() + event.length.toMinutes() +
//                                (batch.startTime.toMinutes() - user.startTime.toMinutes()) +
//                                area.travelTimes.get(event.exit).get(user.end).toMinutes() - area.travelTimes.get(user.start).get(user.end).toMinutes() + user.scoreFunction.get(batch.event);
                        double shift = area.travelTimes.get(event.exit).get(user.start).toMinutes() + event.length.toMinutes() +
                                (batch.startTime.toMinutes() - user.startTime.toMinutes()) +
                                area.travelTimes.get(event.exit).get(user.end).toMinutes() - area.travelTimes.get(user.start).get(user.end).toMinutes();
                        double f = Math.abs(user.scoreFunction.get(batch.event) / shift);
//                        double f =user.scoreFunction.get(batch.event);
                        //If the candidate list is not full, add the triplet to the list, otherwise replace the worst triplet if the new triplet is better.
                        if (candidateList.size() < RCL_SIZE) {
                            double score = solutionScore(partialSolution);
                            if( score + user.scoreFunction.get(batch.event) < score * MIN_IMPROVEMENT){
                                continue;
                            }
                            candidateList.add(new Triplet(batch, null, f));
                        } else {
                            replaceWorstIfBetter(candidateList, new Triplet(batch, null, f));
                        }
                    } else {
                        //If the solution is not empty, we calculate f from and to the previous event in the solution.
                        for (int i = 0; i <= partialSolution.size(); i++) {
                            Location start;
                            if (i == 0) {
                                start = user.start;
                            } else {
                                start = partialSolution.get(i - 1).event.exit;
                                //check if batch is reachable in time
                                if (batch.startTime.toMinutes() < partialSolution.get(i - 1).endTime.toMinutes() + area.travelTimes.get(partialSolution.get(i - 1).event.exit).get(batch.event.entrance).toMinutes()) {
                                    continue;
                                }

                            }
                            Location end;
                            if (i == partialSolution.size()) {
                                end = user.end;
                            } else {
                                end = partialSolution.get(i).event.entrance;
                            }
//                            double f = area.travelTimes.get(event.exit).get(start).toMinutes() + event.length.toMinutes() +
//                                    (batch.startTime.toMinutes() - user.startTime.toMinutes()) +
//                                    area.travelTimes.get(event.exit).get(end).toMinutes() - area.travelTimes.get(start).get(end).toMinutes() + user.scoreFunction.get(batch.event);
                            double f = user.scoreFunction.get(batch.event);
                            //If the candidate list is not full, add the triplet to the list, otherwise replace the worst triplet if the new triplet is better.
                            if (candidateList.size() < RCL_SIZE) {
                                if (start == user.start) {
                                    candidateList.add(new Triplet(batch, null, f));
                                } else {
                                    candidateList.add(new Triplet(batch, partialSolution.get(i - 1), f));
                                }

                            } else {
                                if (start == user.start) {
                                    replaceWorstIfBetter(candidateList, new Triplet(batch, null, f));
                                } else {
                                    replaceWorstIfBetter(candidateList, new Triplet(batch, partialSolution.get(i - 1), f));
                                }
                            }
                        }
                    }
                }
            }
            if (candidateList.isEmpty()) {
                break;
            } else {
                if (!insertTriplet(partialSolution, candidateList)) {
                    break;
                }
            }

        }
        return partialSolution;
    }

    private void updateSolution(List<Batch> solution, List<Batch> bestSolution) {
        //Check whether solution is shorter or has better score, and replace if so.
        double solutionScore = 0;
        double bestSolutionScore = 0;
        for (Batch batch : solution) {
            solutionScore += user.scoreFunction.get(batch.event);
        }
        for (Batch batch : bestSolution) {
            bestSolutionScore += user.scoreFunction.get(batch.event);
        }
        if (solutionScore > bestSolutionScore ) {
            bestSolution.clear();
            bestSolution.addAll(solution);
        }
    }

    private boolean insertTriplet(List<Batch> partialSolution, List<Triplet> candidateList) {
        List<Triplet> candidateListStar = new ArrayList<>();
        //Get the triplet with the highest  f from candidateList
        Triplet tripletStar = candidateList.get(0);
        for (Triplet triplet : candidateList) {
            if (user.scoreFunction.get(triplet.i.event) > user.scoreFunction.get(tripletStar.i.event)) {
                tripletStar = triplet;
            }
        }

        for (Triplet triplet : candidateList) {
            if (user.scoreFunction.get(triplet.i.event) / user.scoreFunction.get(tripletStar.i.event) >= ALPHA) {
                candidateListStar.add(triplet);
            }
        }
        while (!candidateListStar.isEmpty()) {
            List<Batch> tempSolution = new ArrayList<>(partialSolution);
            boolean tempSolutionFeasible = true;

            //Get a random Triplet out of the candidateListStar:
            int randomIndex = (int) (Area.generator.nextDouble() * candidateListStar.size());
            Triplet randomTriplet = candidateListStar.get(randomIndex);
            //Check minimprovement
            double currentScore = 0;
            for (Batch batch : partialSolution) {
                currentScore += user.scoreFunction.get(batch.event);
            }
//            if (user.scoreFunction.get(randomTriplet.i.event) + currentScore < currentScore * MIN_IMPROVEMENT) {
//                continue;
//            }
            if (randomTriplet.j == null) {
                tempSolution.add(0, randomTriplet.i);
            } else {
                tempSolution.add(tempSolution.indexOf(randomTriplet.j) + 1, randomTriplet.i);
            }
            int timeInMinutes = user.startTime.toMinutes();

            for (int i = 0; i < tempSolution.size(); i++) {
                if (i == 0) {
                    //update time
                    timeInMinutes += area.travelTimes.get(user.start).get(tempSolution.get(i).event.entrance).toMinutes();
                } else {
                    //update time
                    timeInMinutes += area.travelTimes.get(tempSolution.get(i - 1).event.exit).get(tempSolution.get(i).event.entrance).toMinutes();
                }
                //check if batch is reached in time
                if (timeInMinutes > tempSolution.get(i).startTime.toMinutes()) {
                    //Needs fixing
                    Batch replacementBatch = tempSolution.get(i).event.getNextBatchWithCapacity(TouristTime.fromMinutes(timeInMinutes), user);

                    if (replacementBatch != null) {
                        tempSolution.set(i, replacementBatch);
                    } else {
                        //cant be fixed, insertion not possible.
                        tempSolutionFeasible = false;
                        candidateListStar.remove(randomTriplet);
                        break;
                    }
                }
                //update time
                timeInMinutes = tempSolution.get(i).endTime.toMinutes();
            }
            //check if hotel is reached in time
            if (timeInMinutes + area.travelTimes.get(tempSolution.get(tempSolution.size() - 1).event.exit).get(user.end).toMinutes() >= user.endTime.toMinutes()) {
                candidateListStar.remove(randomTriplet);
                tempSolutionFeasible = false;
            }
            //if the solution is feasible, update the partial solution and return true.
            if (tempSolutionFeasible) {
                partialSolution.clear();
                partialSolution.addAll(tempSolution);
                return true;
            }
        }
        return false;
    }

    private void localSearch(List<Batch> solution, List<Event> wishList) {
        //Try all possible swaps of two batches in the solution.
        int numberOfImprovements = 0;
        boolean improved = true;
        do {
            List<Batch> bestSolution = new ArrayList<>(solution);
            for (int i = 0; i < solution.size(); i++) {
                for (int j = i + 1; j < solution.size(); j++) {
                    boolean feasible = true;
                    List<Event> orderOfEvents = new ArrayList<>();
                    for (Batch batch : solution) {
                        orderOfEvents.add(batch.event);
                    }
                    //Swap the two batches in the order of events.
                    Event temp = orderOfEvents.get(i);
                    orderOfEvents.set(i, orderOfEvents.get(j));
                    orderOfEvents.set(j, temp);
                    //Try to plan the new order
                    List<Batch> newSolution = new ArrayList<>();
                    int timeInMinutes = user.startTime.toMinutes();
                    for (Event event : orderOfEvents) {
                        if (newSolution.isEmpty()) {
                            timeInMinutes += area.travelTimes.get(user.start).get(event.entrance).toMinutes();
                        } else {
                            timeInMinutes += area.travelTimes.get(newSolution.get(newSolution.size() - 1).event.exit).get(event.entrance).toMinutes();
                        }
                        Batch batch = event.getNextBatchWithCapacity(TouristTime.fromMinutes(timeInMinutes), user);
                        if (batch == null) {
                            feasible = false;
                            break;
                        }
                        newSolution.add(batch);
                        timeInMinutes = batch.endTime.toMinutes();
                    }
                    if (feasible) {
                        if (newSolution.get(newSolution.size() - 1).endTime.toMinutes() < bestSolution.get(bestSolution.size() - 1).endTime.toMinutes()) {
                            //Improved time, update solution.
                            bestSolution.clear();
                            bestSolution.addAll(newSolution);
                        }
                    }
                }
            }
            //Try to improve the solution, if no improvement is found, return the best solution.
            if (bestSolution.equals(solution)) {
                improved = false;
            } else {
                numberOfImprovements++;
                solution.clear();
                solution.addAll(bestSolution);
                List<Event> orderOfEvents = new ArrayList<>();
                for (Batch batch : solution) {
                    orderOfEvents.add(batch.event);
                }
                //try to extend solution by adding a batch to the end of the solution.
                Event lastEvent = orderOfEvents.get(orderOfEvents.size() - 1);
                int timeInMinutes = solution.get(solution.size() - 1).endTime.toMinutes();
                for (Event event : wishList) {
                    if (orderOfEvents.contains(event)) {
                        continue;
                    }
                    if (timeInMinutes + area.travelTimes.get(lastEvent.exit).get(event.entrance).toMinutes() + event.length.toMinutes() + area.travelTimes.get(event.exit).get(user.end).toMinutes() >= user.endTime.toMinutes()) {
                        continue;
                    }
                    Batch batch = event.getNextBatchWithCapacity(TouristTime.fromMinutes(timeInMinutes + area.travelTimes.get(lastEvent.exit).get(event.entrance).toMinutes()), user);
                    if (batch != null && batch.endTime.toMinutes() + area.travelTimes.get(batch.event.exit).get(user.end).toMinutes() < user.endTime.toMinutes()) {
                        solution.add(batch);
                        break;
                    }
                }
            }
        } while (improved);
    }

    private static void replaceWorstIfBetter(List<Triplet> before, Triplet newTriplet) {
        Triplet worstPair = null;
        for (Triplet triplet : before) {
            if (worstPair != null) {
                if (worstPair.f > triplet.f) {
                    worstPair = triplet;
                }
            } else {
                worstPair = triplet;
            }
        }
        assert worstPair != null;
        if (worstPair.f < newTriplet.f) {
            before.remove(worstPair);
            before.add(newTriplet);
        }
    }

    private static class Triplet {
        private final Batch i;
        private final Batch j;
        private final double f;

        Triplet(Batch i, Batch j, double f) {
            this.i = i;
            this.j = j;
            this.f = f;
        }

    }
}

