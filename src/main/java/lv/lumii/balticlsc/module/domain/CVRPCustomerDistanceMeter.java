package lv.lumii.balticlsc.module.domain;

import org.optaplanner.core.impl.heuristic.selector.common.nearby.NearbyDistanceMeter;

public class CVRPCustomerDistanceMeter implements NearbyDistanceMeter<Customer, RouteElement> {

    public double getNearbyDistance(Customer origin, RouteElement destination) {
        return origin.getLocation().getAirDistance(destination.getLocation());
    }
}
