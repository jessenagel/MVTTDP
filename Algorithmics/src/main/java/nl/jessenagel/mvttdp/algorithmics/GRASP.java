package nl.jessenagel.mvttdp.algorithmics;

import nl.jessenagel.mvttdp.framework.*;
import org.apache.commons.math3.util.Pair;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class GRASP {
   public static final int MAX_ITERATIONS = 30;
   public static final int RCL_SIZE = 10;
    public User user;
    public Area area;

    public List<Batch> solve(List<Event> wishlist){
        List<Batch> bestSolution = null;
        for(int i = 1; i< MAX_ITERATIONS; i++){
            List <Batch> solution = GRASPConstructionPhase();
            solution = localSearch(solution);
            updateSolution(solution, bestSolution);
        }
        return null;
    }

    private void updateSolution(List<Batch> solution, List<Batch> bestSolution) {
        List<Batch> partialSolution = fuzzyConstructive();
    }

    private List<Batch> fuzzyConstructive() {
        List<Batch> partialSolution = new ArrayList<>();
        double worstValueInList = Double.MAX_VALUE;
        List<Triplet> candidateList = new ArrayList<>();
        while(true){
            for(Event event : this.area.events.values()) {
                //calculate f
                for (Batch batch : event.batches) {
                    if (partialSolution.isEmpty()) {
                        double f = area.travelTimes.get(event.exit).get(user.start).toMinutes() + event.length.toMinutes() +
                                (batch.startTime.toMinutes() - user.startTime.toMinutes()) +
                                area.travelTimes.get(event.exit).get(user.end).toMinutes() - area.travelTimes.get(user.start).get(user.end).toMinutes();
                        if (candidateList.size() < RCL_SIZE) {
                            candidateList.add(new Triplet(batch,null, f));
                        } else {
                            replaceWorstIfBetter(candidateList,new Triplet(batch,null, f) );
                        }
                    } else {
                        for (int i = 0; i <= partialSolution.size(); i++) {
                            System.out.println(i);
                            Location start = null;
                            if (i == 0) {
                                start = user.start;
                            } else {
                                start = partialSolution.get(i - 1).event.exit;
                            }
                            Location end = null;
                            if (i == partialSolution.size()) {
                                end = user.end;
                            } else {
                                end = partialSolution.get(i).event.entrance;
                            }
                            double f = area.travelTimes.get(event.exit).get(user.start).toMinutes() + event.length.toMinutes() +
                                    (batch.startTime.toMinutes() - user.startTime.toMinutes()) +
                                    area.travelTimes.get(event.exit).get(user.end).toMinutes() - area.travelTimes.get(user.start).get(user.end).toMinutes();

                            if (candidateList.size() < RCL_SIZE) {
                                if(start==user.start){
                                    candidateList.add(new Triplet(batch,null,f));
                                }else{
                                    candidateList.add(new Triplet(batch,partialSolution.get(i - 1),f));
                                }

                            } else {
                                if(start==user.start){
                                    replaceWorstIfBetter(candidateList, new Triplet(batch,null,f));
                                }else{
                                    replaceWorstIfBetter(candidateList, new Triplet(batch,partialSolution.get(i-1),f));
                                }
                            }
                        }

                    }
                }
            }
            if(candidateList.isEmpty()){
                break;
            }else{
                insertTriplet(partialSolution,candidateList);
            }

        }

        return null;
    }

    private void insertTriplet(List<Batch> partialSolution, List<Triplet> candidateList) {

    }

    private List<Batch> localSearch(List<Batch> solution) {
        return null;
    }

    private List<Batch> GRASPConstructionPhase() {
        return null;
    }
    private static void replaceWorstIfBetter(List<Triplet> before, Triplet newTriplet) {
        Triplet worstPair = null;
        for(Triplet triplet : before){
            if(worstPair != null){
                if(worstPair.f > triplet.f){
                    worstPair = triplet;
                }
            }else{
                worstPair = triplet;
            }
        }
        assert worstPair != null;
        if(worstPair.f<newTriplet.f) {
            before.remove(worstPair);
            before.add(newTriplet);
        }
    }
    private class Triplet{
        private Batch i;
        private Batch j;
        private final double f;
        Triplet(Batch i, Batch j, double f){
            this.i = i;
            this.j = j;
            this.f = f;
        }

    }
}

