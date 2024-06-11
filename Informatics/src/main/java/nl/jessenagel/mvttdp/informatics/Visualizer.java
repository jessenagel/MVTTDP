package nl.jessenagel.mvttdp.informatics;

import java.util.List;
import nl.jessenagel.mvttdp.framework.*;
public class Visualizer {

    public static void printSchedule(Area area, List<Batch> solution){
        for(Batch batch : solution){
            System.out.println(batch.event.name + " from: ");
            batch.startTime.print();
            System.out.println("to: ");
            batch.endTime.print();
        }
    }
}
