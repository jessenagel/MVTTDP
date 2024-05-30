package nl.jessenagel.tourism.algorithmics;

import java.util.ArrayList;
import java.util.List;
import nl.jessenagel.tourism.framework.*;

public class FCFS {
    Area area;

    public FCFS(Area area) {
        this.area = area;
    }

    public void solve() {
        int i = 1;
        for (User user : this.area.users) {
            System.out.println("User: " + user.name + ", number: " + i + "/" + this.area.users.size());
            DSSRA solver = new DSSRA();
            solver.area = this.area;
            solver.user = user;
            solver.createListOfBatches(this.area);
            List<Batch> schedule = solver.dynamicProgramming();
            user.schedule = new ArrayList<>(schedule);
            for (Batch batch : user.schedule) {
                batch.visitors.add(user);
                System.out.println(user.name);
                System.out.println(batch.event.name + " from: ");
                batch.startTime.print();
                System.out.println("to: ");
                batch.endTime.print();
                System.out.println("Place on wishlist: " + solver.user.wishList.indexOf(batch.event));
            }
            i++;
        }
    }
}
