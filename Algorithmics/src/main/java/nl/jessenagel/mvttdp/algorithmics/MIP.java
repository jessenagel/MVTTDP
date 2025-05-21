package nl.jessenagel.mvttdp.algorithmics;

import ilog.concert.*;
import ilog.cplex.*;

import java.util.HashMap;
import java.util.Map;
import nl.jessenagel.mvttdp.framework.*;

public class MIP {

    private static final int MAX_ITERATIONS = 100;
    private static final double TRESHOLD_VALUE = 0.01;
    private int currentRound = 0;
    private IloCplex cplex;
    private Map<User, Map<Event, Map<Event, IloNumVar>>> xMap;
    private Map<Event, Map<Batch, Map<User, IloNumVar>>> yMap;
    private Map<User, IloNumVar> lMap;
    private Map<User, IloNumVar> sMap;
    private Map<Integer, Map<User, Double>> sMapStar;

    private IloNumVar theta;
    private Area area;
    private static final double SUFFICIENTLY_LARGE = 100000000000.0;
    private boolean firstRound;
    private static final long MAX_TIME = 14400;
    public void solve(Area area) {
        this.firstRound = true;
        sMapStar = new HashMap<>();
        long startTime = System.nanoTime();
        for (int i = 0; i < MAX_ITERATIONS; i++) {
            this.area = area;
            this.xMap = new HashMap<>();
            this.yMap = new HashMap<>();
            this.lMap = new HashMap<>();
            this.sMap = new HashMap<>();
            try {
                cplex = new IloCplex();
                addVariables();
                addConstraints();
                switch (TouristConstants.scoreFunction) {
                    case "product":
                        addBernoulliNashVariables();
                        addBernoulliNashObjective();
                        break;
                    case "sum":
                        addObjective();
                        break;
                    case "maximin":
                        addThetaConstraintsAndObjective();
                        break;
                    default:
                        System.err.print("Unknown score function: " + TouristConstants.scoreFunction);
                        System.exit(2);
                }

                cplex.exportModel("MIP.mps");
                cplex.setParam(IloCplex.Param.MIP.Tolerances.MIPGap, TouristConstants.MIPGap);
                cplex.setParam(IloCplex.Param.MIP.Strategy.File, 1);
                cplex.setParam(IloCplex.Param.WorkMem, 30000);
                cplex.setParam(IloCplex.Param.TimeLimit, 0);
                cplex.setParam(IloCplex.Param.WorkDir, TouristConstants.scratchDir);
                cplex.setParam(IloCplex.Param.Emphasis.MIP,2);

                if (cplex.solve()) {
                    System.out.println("solved");
                    cplex.writeSolution("MIP.sol");
                    cplex.output().println("Solution status " + cplex.getStatus());
                    cplex.output().println("Solution value " + cplex.getObjValue());
                    if(TouristConstants.scoreFunction.equals("product")) {
                        double sum = 0;

                        for (int k = 0; k < this.area.users.size(); k++) {
                            sum += Math.abs(Math.log(cplex.getValue(this.sMap.get(this.area.users.get(k)))) - cplex.getValue(lMap.get(this.area.users.get(k))));
                        }
                        System.out.print("SUM BN=" +sum);
                        this.sMapStar.put(currentRound, new HashMap<>());
                        for (User user : this.area.users) {
                            this.sMapStar.get(currentRound).put(user, cplex.getValue(sMap.get(user)));
                        }
                        firstRound = false;
                        currentRound++;
                        //Break if accuracy is reached or time is exceeded.
                        if (sum <= TRESHOLD_VALUE || System.nanoTime() - startTime > MAX_TIME * 1_000_000_000) {
                            System.out.println("Accuracy = " + sum);
                            break;
                        }
                    }else{
                        break;
                    }

                } else {
                    System.err.println("Could not solve");
                }

            } catch (IloException e) {
                System.err.println("Concert exception caught: " + e);
            }

        }
        bookTrips();
    }

    private void addBernoulliNashVariables() throws IloException {
        int i = 0;
        for (User user : this.area.users) {
            IloNumVar varL = cplex.numVar(0, Double.MAX_VALUE);
            IloNumVar varS = cplex.numVar(0, Double.MAX_VALUE);
            this.lMap.put(user, varL);
            this.lMap.get(user).setName("l" + i);
            this.sMap.put(user, varS);
            this.sMap.get(user).setName("s" + i++);

        }
    }


    private void bookTrips() {
        for (Event event : this.area.events.values()) {
            for (Batch batch : event.batches) {
                for (User user : this.area.users) {
                    try {
                        if (cplex.getValue(yMap.get(event).get(batch).get(user)) > 0.9) {
                            if (!batch.bookGroup(user)) {
                                System.err.println("Something going very wrong while booking!");
                            }
                        }
                    } catch (IloException e) {
                        System.err.println("IloException");
                    }
                }
            }
        }
        for (User user : this.area.users) {
            user.sortSchedule();
            user.happiness = Generic.calcScore(user.schedule, user, this.area);
        }
    }

    void addVariables() throws IloException {
        int i = 0;
        for (User user : this.area.users) {
            this.xMap.put(user, new HashMap<>());
            for (Event fromEvent : this.area.events.values()) {
                this.xMap.get(user).put(fromEvent, new HashMap<>());
                for (Event toEvent : this.area.events.values()) {
                    if (fromEvent == toEvent) {
                        continue;
                    }
                    IloNumVar var = cplex.boolVar();
                    this.xMap.get(user).get(fromEvent).put(toEvent, var);
                    xMap.get(user).get(fromEvent).get(toEvent).setName("X_" + i);
                    i++;
                }
            }
            this.xMap.get(user).put(user.startEvent, new HashMap<>());
            for (Event toEvent : this.area.events.values()) {
                IloNumVar var = cplex.boolVar();
                this.xMap.get(user).get(user.startEvent).put(toEvent, var);
                xMap.get(user).get(user.startEvent).get(toEvent).setName("X_" + i);
                i++;
            }
            for (Event fromEvent : this.area.events.values()) {
                IloNumVar var = cplex.boolVar();
                this.xMap.get(user).get(fromEvent).put(user.endEvent, var);
                xMap.get(user).get(fromEvent).get(user.endEvent).setName("X_" + i);
                i++;
            }
            IloNumVar var = cplex.boolVar();
            this.xMap.get(user).get(user.startEvent).put(user.endEvent, var);
            xMap.get(user).get(user.startEvent).get(user.endEvent).setName("X_" + i);
            i++;
        }
        int j = 0;
        for (Event event : this.area.events.values()) {
            this.yMap.put(event, new HashMap<>());
            for (Batch batch : event.batches) {
                this.yMap.get(event).put(batch, new HashMap<>());
                for (User user : this.area.users) {
                    IloNumVar var = cplex.boolVar();
                    this.yMap.get(event).get(batch).put(user, var);
                    this.yMap.get(event).get(batch).get(user).setName("Y_" + j);
                    j++;
                }
            }
        }
        //Theta
        theta = cplex.numVar(0, Double.max(0, Double.MAX_VALUE));
    }

    void addConstraints() throws IloException {
        //constraint 1, We should move from the starting and to the final event
        int i1 = 0;
        for (User user : this.area.users) {
            IloNumExpr lhs = cplex.constant(0);
            for (Event event : this.area.events.values()) {
                lhs = cplex.sum(lhs, this.xMap.get(user).get(user.startEvent).get(event));
            }
            lhs = cplex.sum(lhs, this.xMap.get(user).get(user.startEvent).get(user.endEvent));
            cplex.addEq(lhs, cplex.constant(1)).setName("Constraint1_1_" + i1);
            i1++;
        }
        int i2 = 0;

        for (User user : this.area.users) {
            IloNumExpr lhs = cplex.constant(0);
            for (Event event : this.area.events.values()) {
                lhs = cplex.sum(lhs, this.xMap.get(user).get(event).get(user.endEvent));
            }
            lhs = cplex.sum(lhs, this.xMap.get(user).get(user.startEvent).get(user.endEvent));

            cplex.addEq(lhs, cplex.constant(1)).setName("Constraint1_2_" + i2);
            i2++;

        }
        //constraint 2
        int i3 = 0;
        for (Event event : this.area.events.values()) {
            for (User user : this.area.users) {
                IloNumExpr lhs = cplex.constant(0);
                for (Event eventStar : this.area.events.values()) {
                    if (eventStar == event) {
                        continue;
                    }
                    lhs = cplex.sum(lhs, this.xMap.get(user).get(event).get(eventStar));
                }

                lhs = cplex.sum(lhs, this.xMap.get(user).get(event).get(user.endEvent));
                IloNumExpr rhs = cplex.constant(0);
                for (Batch batch : event.batches) {
                    rhs = cplex.sum(rhs, yMap.get(event).get(batch).get(user));
                }
                cplex.addEq(lhs, rhs).setName("Constraint2_1_" + i3);
                i3++;

            }

        }
        int i4 = 0;
        for (Event event : this.area.events.values()) {
            for (User user : this.area.users) {
                IloNumExpr lhs = cplex.constant(0);
                for (Event eventStar : this.area.events.values()) {
                    if (eventStar == event) {
                        continue;
                    }
                    lhs = cplex.sum(lhs, this.xMap.get(user).get(eventStar).get(event));
                }
                lhs = cplex.sum(lhs, this.xMap.get(user).get(user.startEvent).get(event));
                IloNumExpr rhs = cplex.constant(0);
                for (Batch batch : event.batches) {
                    rhs = cplex.sum(rhs, yMap.get(event).get(batch).get(user));
                }
                cplex.addEq(lhs, rhs).setName("Constraint2_2_" + i4);
                i4++;

            }
        }
        //constraint 3
        int i5 = 0;

        for (Event event : this.area.events.values()) {
            for (User user : this.area.users) {
                IloNumExpr lhs = cplex.constant(0);
                for (Batch batch : event.batches) {
                    lhs = cplex.sum(lhs, yMap.get(event).get(batch).get(user));
                }
                cplex.addLe(lhs, cplex.constant(1)).setName("constraint3_" + i5);
                i5++;

            }
        }
        //constraint 4
        int i6 = 0;

        for (User user : this.area.users) {
            for (Event event : this.area.events.values()) {
                IloNumExpr lhs = cplex.constant(0);
                for (Batch batch : event.batches) {
                    lhs = cplex.sum(lhs, cplex.prod(yMap.get(event).get(batch).get(user), batch.endTime.toMinutes()));
                }
                lhs = cplex.sum(lhs, cplex.prod(xMap.get(user).get(event).get(user.endEvent), area.travelTimes.get(event.exit).get(user.end).toMinutes()));
                cplex.addLe(lhs, cplex.constant(user.endTime.toMinutes())).setName("constraint4_" + i6);
                i6++;

            }
        }
        //constraint 5
        int i7 = 0;

        for (User user : this.area.users) {
            for (Event event : this.area.events.values()) {
                IloNumExpr lhs = cplex.constant(0);
                IloNumExpr rhs = cplex.constant(0);
                lhs = cplex.sum(lhs, cplex.constant(user.startTime.toMinutes()));
                lhs = cplex.sum(lhs, area.travelTimes.get(user.start).get(event.entrance).toMinutes());
                lhs = cplex.prod(xMap.get(user).get(user.startEvent).get(event), lhs);

                for (Batch batch : event.batches) {
                    if (TouristTime.geq(batch.startTime, user.startTime)) {
                        rhs = cplex.sum(rhs, cplex.prod(yMap.get(event).get(batch).get(user), batch.startTime.toMinutes()));
                    }
                }
                cplex.addLe(lhs, rhs).setName("constraint5_" + i7);

                i7++;
            }
        }
        //constraint 6
        int i8 = 0;
        for (User user : this.area.users) {
            for (Event event : this.area.events.values()) {
                for (Event eventStar : this.area.events.values()) {
                    if (event == eventStar) {
                        continue;
                    }
                    IloNumExpr lhs = cplex.constant(0);
                    IloNumExpr rhs = cplex.constant(0);
                    for (Batch batch : event.batches) {
                        lhs = cplex.sum(lhs, cplex.prod(yMap.get(event).get(batch).get(user), batch.endTime.toMinutes()));
                    }
                    lhs = cplex.sum(lhs, area.travelTimes.get(event.exit).get(eventStar.entrance).toMinutes());

                    for (Batch batch : eventStar.batches) {
                        rhs = cplex.sum(rhs, cplex.prod(yMap.get(eventStar).get(batch).get(user), batch.startTime.toMinutes()));
                    }
                    rhs = cplex.sum(rhs, cplex.prod(cplex.sum(1, cplex.prod(cplex.constant(-1), xMap.get(user).get(event).get(eventStar))), SUFFICIENTLY_LARGE));
                    cplex.addLe(lhs, rhs).setName("constraint6_" + i8);
                    i8++;

                }
            }
        }
        //constraint 7
        int i9 = 0;
        for (Event event : this.area.events.values()) {
            for (Batch batch : event.batches) {
                IloNumExpr lhs = cplex.constant(0);
                for (User user : this.area.users) {
                    lhs = cplex.sum(lhs, cplex.prod(yMap.get(event).get(batch).get(user), user.groupSizePerEvent.get(event)));
                }
                cplex.addLe(lhs, batch.capacity).setName("constraint7_" + i9);
                i9++;

            }
        }
        //constraint 8
        int i10 = 0;

        for (Event event : this.area.events.values()) {
            for (int t = 0; t <= 1440; t++) {
                IloNumExpr lhs = cplex.constant(0);
                for (Batch batch : event.getBatchesAtTime(new TouristTime("0:0:" + t))) {
                    for (User user : this.area.users) {
                        lhs = cplex.sum(lhs, cplex.prod(yMap.get(event).get(batch).get(user), user.groupSizePerEvent.get(event)));
                    }
                }
                cplex.addLe(lhs, event.capacity).setName("constraint8_" + i10);
                i10++;
            }
        }
    }

    private void addThetaConstraintsAndObjective() throws IloException {
        for (User user : this.area.users) {
            IloNumExpr lhs = cplex.constant(0);
            IloNumExpr rhs1 = cplex.constant(0);
            IloNumExpr rhs2 = cplex.constant(0);
            lhs = cplex.sum(lhs, theta);
            for (Event event : this.area.events.values()) {
                for (Batch batch : event.batches) {
                    rhs1 = cplex.sum(rhs1, cplex.prod(user.scoreFunction.get(event), yMap.get(event).get(batch).get(user)));
                }
            }
            for (Event fromEvent : this.area.events.values()) {
                for (Event toEvent : this.area.events.values()) {
                    if (fromEvent == toEvent) {
                        continue;
                    }
                    rhs2 = cplex.sum(rhs2, cplex.prod(this.area.travelTimes.get(fromEvent.exit).get(toEvent.entrance).toMinutes(), xMap.get(user).get(fromEvent).get(toEvent)));
                }
            }
            for (Event toEvent : this.area.events.values()) {
                rhs2 = cplex.sum(rhs2, cplex.prod(this.area.travelTimes.get(user.start).get(toEvent.entrance).toMinutes(), xMap.get(user).get(user.startEvent).get(toEvent)));

            }
            for (Event fromEvent : this.area.events.values()) {
                rhs2 = cplex.sum(rhs2, cplex.prod(this.area.travelTimes.get(fromEvent.exit).get(user.end).toMinutes(), xMap.get(user).get(fromEvent).get(user.endEvent)));
            }
            rhs2 = cplex.sum(rhs2, cplex.prod(this.area.travelTimes.get(user.start).get(user.end).toMinutes(), xMap.get(user).get(user.startEvent).get(user.endEvent)));
            rhs1 = cplex.prod(TouristConstants.WEIGHT_1, rhs1);
            rhs2 = cplex.prod(TouristConstants.WEIGHT_2, rhs2);
            rhs1 = cplex.sum(rhs1, rhs2);
            cplex.addLe(lhs, rhs1).setName("Theta_" + user.name);
        }
        cplex.addMaximize(theta);
    }

    private void addBernoulliNashObjective() throws IloException {
        IloNumExpr objective = cplex.constant(0);
        for (User user : this.area.users) {
            IloNumExpr rhs1 = cplex.constant(0);
            IloNumExpr rhs2 = cplex.constant(0);
            for (Event event : this.area.events.values()) {
                for (Batch batch : event.batches) {
                    rhs1 = cplex.sum(rhs1, cplex.prod(user.scoreFunction.get(event), yMap.get(event).get(batch).get(user)));
                }
            }
            for (Event fromEvent : this.area.events.values()) {
                for (Event toEvent : this.area.events.values()) {
                    if (fromEvent == toEvent) {
                        continue;
                    }
                    rhs2 = cplex.sum(rhs2, cplex.prod(this.area.travelTimes.get(fromEvent.exit).get(toEvent.entrance).toMinutes(), xMap.get(user).get(fromEvent).get(toEvent)));

                }
            }
            for (Event toEvent : this.area.events.values()) {
                rhs2 = cplex.sum(rhs2, cplex.prod(this.area.travelTimes.get(user.start).get(toEvent.entrance).toMinutes(), xMap.get(user).get(user.startEvent).get(toEvent)));
            }
            for (Event fromEvent : this.area.events.values()) {
                rhs2 = cplex.sum(rhs2, cplex.prod(this.area.travelTimes.get(fromEvent.exit).get(user.end).toMinutes(), xMap.get(user).get(fromEvent).get(user.endEvent)));
            }
            rhs2 = cplex.sum(rhs2, cplex.prod(this.area.travelTimes.get(user.start).get(user.end).toMinutes(), xMap.get(user).get(user.startEvent).get(user.endEvent)));
            rhs1 = cplex.prod(TouristConstants.WEIGHT_1, rhs1);
            rhs2 = cplex.prod(TouristConstants.WEIGHT_2, rhs2);
            rhs1 = cplex.sum(rhs1, rhs2);
            cplex.addEq(sMap.get(user), rhs1);
            cplex.addGe(sMap.get(user), cplex.constant(1.0));

            if (firstRound) {
                IloNumExpr lConstraint;
                lConstraint = cplex.sum(sMap.get(user), cplex.prod(cplex.constant(-1), cplex.constant(1000.0)));
                lConstraint = cplex.prod(lConstraint, cplex.constant(1.0 / 1000.0));
                lConstraint = cplex.sum(lConstraint, cplex.constant(Math.log(1000.0)));
                cplex.addLe(lMap.get(user), lConstraint);

            } else {
                for (int i = 0; i < currentRound; i++) {
                    IloNumExpr lConstraint;

                    lConstraint = cplex.sum(sMap.get(user), cplex.prod(cplex.constant(-1), cplex.constant(sMapStar.get(i).get(user))));
                    lConstraint = cplex.prod(lConstraint, cplex.constant(1 / sMapStar.get(i).get(user)));
                    lConstraint = cplex.sum(lConstraint, cplex.constant(Math.log(sMapStar.get(i).get(user))));
                    cplex.addLe(lMap.get(user), lConstraint);

                }
            }
            objective = cplex.sum(objective, lMap.get(user));
        }

        cplex.addMaximize(objective);
    }

    void addObjective() throws IloException {
        IloNumExpr obj1 = cplex.constant(0);
        IloNumExpr obj2 = cplex.constant(0);

        for (Event event : this.area.events.values()) {
            for (Batch batch : event.batches) {
                for (User user : this.area.users) {
                    obj1 = cplex.sum(obj1, cplex.prod(user.scoreFunction.get(event), yMap.get(event).get(batch).get(user)));
                }
            }
        }
        if(TouristConstants.travelPenalty) {
            for (User user : this.area.users) {
                for (Event fromEvent : this.area.events.values()) {
                    for (Event toEvent : this.area.events.values()) {
                        if (fromEvent == toEvent) {
                            continue;
                        }
                        obj2 = cplex.sum(obj2, cplex.prod(this.area.travelTimes.get(fromEvent.exit).get(toEvent.entrance).toMinutes(), xMap.get(user).get(fromEvent).get(toEvent)));
                    }
                }
                for (Event toEvent : this.area.events.values()) {
                    obj2 = cplex.sum(obj2, cplex.prod(this.area.travelTimes.get(user.start).get(toEvent.entrance).toMinutes(), xMap.get(user).get(user.startEvent).get(toEvent)));

                }
                for (Event fromEvent : this.area.events.values()) {
                    obj2 = cplex.sum(obj2, cplex.prod(this.area.travelTimes.get(fromEvent.exit).get(user.end).toMinutes(), xMap.get(user).get(fromEvent).get(user.endEvent)));
                }
                obj2 = cplex.sum(obj2, cplex.prod(this.area.travelTimes.get(user.start).get(user.end).toMinutes(), xMap.get(user).get(user.startEvent).get(user.endEvent)));
            }
        }
        obj1 = cplex.prod(TouristConstants.WEIGHT_1, obj1);
        obj2 = cplex.prod(TouristConstants.WEIGHT_2, obj2);
        obj1 = cplex.sum(obj1, obj2);
        cplex.addMaximize(obj1);
    }
}
