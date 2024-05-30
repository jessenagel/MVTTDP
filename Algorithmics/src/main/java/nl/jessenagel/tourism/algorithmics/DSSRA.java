package nl.jessenagel.tourism.algorithmics;

import java.util.*;
import nl.jessenagel.tourism.framework.*;

public class DSSRA {
    public User user;
    List<State> states;
    Map<Event, List<State>> table;
    Map<State, List<Batch>> routes;
    List<Batch> sortedListOfBatches;
    public double bestScore;
    public Area area;

    public DSSRA() {
        this.table = new HashMap<>();
        this.routes = new HashMap<>();
        this.sortedListOfBatches = new ArrayList<>();
        this.states = new ArrayList<>();
    }

    public void createListOfBatches(Area area) {
        this.area = area;
        for (Event event : area.events.values()) {
            if (user.wishList.contains(event)) {
                this.table.put(event, new ArrayList<>());
                for (Batch batch : event.batches) {
                    if (batch.sufficientCapacityForGroup(this.user) && TouristTime.geq(batch.startTime, user.startTime) && TouristTime.leq(batch.endTime, user.endTime)) {
                        sortedListOfBatches.add(batch);
                    }
                }
            }
        }
        sortedListOfBatches.sort(new BatchComparator());
    }

    public List<Batch> solve() {
        Batch criticalVertex = null;
        dynamicProgramming();
        return null;
    }

    public List<Batch> dynamicProgramming() {
        if (sortedListOfBatches.size() == 0) {
            return null;
        }

        for (int counter = 1; counter <= this.sortedListOfBatches.size(); counter++) {
            Batch batch = this.sortedListOfBatches.get(this.sortedListOfBatches.size() - counter);
            List<State> statesToBeAdded = new ArrayList<>();
            for (State comparisonState : this.states) {
                if (stateCanBeExtended(comparisonState, batch)) {

                    State newState = extendState(comparisonState, batch);
                    if (!stateIsDominated(states, newState)) {
                        statesToBeAdded.add(newState);

                    }
                }
            }
            this.states.addAll(statesToBeAdded);
            State state = createNewState(batch);
            if (!stateIsDominated(states, state)) {
                this.states.add(state);
            }
        }
        State bestState = this.states.get(0);
        for (State state : this.states) {
            if (state.P > bestState.P) {
                bestState = state;
            }
        }
        this.user.happiness = bestState.P;
        this.bestScore = bestState.P;
        System.out.println(bestState.P);
        return this.routes.get(bestState);
    }

    private boolean stateIsDominated(List<State> states, State state) {
        for (State possibleDominator : states) {
            if (state.i == possibleDominator.i) {
                boolean finishedLoop = true;
                for (Event event : this.area.events.values()) {
                    if ((possibleDominator.S.get(event) ? 1 : 0) > (state.S.get(event) ? 1 : 0)) {
                        finishedLoop = false;
                        break;
                    }
                }
                if (finishedLoop) {
                    if (TouristTime.geq(possibleDominator.tau, state.tau) && possibleDominator.P >= state.P) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    private State createNewState(Batch batch) {
        State state = new State();
        state.S = createEmptyBinaryVector();
        state.S.put(batch.event, true);
        state.tau = batch.startTime;
        state.i = batch.event;
        state.P = user.scoreFunction.get(batch.event) + TouristConstants.WEIGHT_2 * area.travelTimes.get(batch.event.exit).get(user.end).toMinutes();
        this.table.get(batch.event).add(state);
        List<Batch> route = new ArrayList<>();
        route.add(batch);
        this.routes.put(state, route);
        return state;
    }

    public boolean stateCanBeExtended(State state, Batch candidateBatch) {
        return (!state.S.get(candidateBatch.event) && TouristTime.geq(state.tau, candidateBatch.endTime.increaseBy(area.travelTimes.get(candidateBatch.event.exit).get(state.i.entrance))) && candidateBatch.event != state.i);
    }

    public State extendState(State state, Batch candidateBatch) {
        State newState = state.copy();
        newState.S.put(candidateBatch.event, true);
        newState.tau = candidateBatch.startTime;
        newState.P += this.user.scoreFunction.get(candidateBatch.event) + TouristConstants.WEIGHT_2 * area.travelTimes.get(candidateBatch.event.exit).get(newState.i.entrance).toMinutes();
        newState.i = candidateBatch.event;
        List<Batch> newRoute = new ArrayList<>(this.routes.get(state));
        newRoute.add(0, candidateBatch);
        this.routes.put(newState, newRoute);
        return newState;
    }

    Map<Event, Boolean> createEmptyBinaryVector() {
        Map<Event, Boolean> output = new HashMap<>();
        for (Event event : area.events.values()) {
            output.put(event, false);
        }
        return output;
    }
}

class State {
    Map<Event, Boolean> S;
    TouristTime tau;
    Event i;
    double P;

    State() {
        this.P = 0.0;
    }

    public State copy() {
        State output = new State();
        output.S = new HashMap<>(this.S);
        output.tau = this.tau.copy();
        output.i = this.i;
        output.P = this.P;
        return output;
    }
}
