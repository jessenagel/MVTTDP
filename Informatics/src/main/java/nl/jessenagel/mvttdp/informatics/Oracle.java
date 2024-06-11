package nl.jessenagel.mvttdp.informatics;

import nl.jessenagel.mvttdp.framework.*;
public class Oracle {
    User user;
    public Oracle(User user){
        this.user = user;
    }

    public int numberOfUsersWithHigherRanking(Batch batch, Area area){
        int placeOnWishlist = this.user.wishList.indexOf(batch.event);
        int usersWithHigherRanking = 0;
        for(User userToCompare: area.users){
            if(!userToCompare.isScheduled){
                if(userToCompare.wishList.indexOf(batch.event) < placeOnWishlist){
                    usersWithHigherRanking += userToCompare.groupSizePerEvent.get(batch.event);
                }
            }
        }
        return usersWithHigherRanking;
    }
}
