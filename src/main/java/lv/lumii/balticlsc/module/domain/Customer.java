package lv.lumii.balticlsc.module.domain;

import org.optaplanner.core.api.domain.entity.PlanningEntity;
import org.optaplanner.core.api.domain.variable.AnchorShadowVariable;
import org.optaplanner.core.api.domain.variable.PlanningVariable;
import org.optaplanner.core.api.domain.variable.PlanningVariableGraphType;

@PlanningEntity
public class Customer implements RouteElement {

    private Double demand;
    private Location location;
    private String nids;
    private String name;
    private Vehicle vehicle;

    private RouteElement previous;
    private Customer nextCustomer;

    public Customer() {}
    public Customer(Double demand, Location location, String nids, String name) {
        this.demand = demand;
        this.location = location;
        this.nids = nids;
        this.name = name;
    }

    public Double getDemand() {
        return demand;
    }

    public void setDemand(Double demand) {
        this.demand = demand;
    }

    @Override
    public Location getLocation() {
        return location;
    }

    public void setLocation(Location location) {
        this.location = location;
    }

    public String getNids() {
        return nids;
    }

    public void setNids(String nids) {
        this.nids = nids;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
    @PlanningVariable(graphType = PlanningVariableGraphType.CHAINED,
                      valueRangeProviderRefs = {"customerRange","vehicleRange"})
    public RouteElement getPrevious() {
        return previous;
    }

    public void setPrevious(RouteElement previous) {
        this.previous = previous;
    }

    @AnchorShadowVariable(sourceVariableName = "previous")
    public Vehicle getVehicle() {
        return vehicle;
    }

    public void setVehicle(Vehicle vehicle) {
        this.vehicle = vehicle;
    }

    @Override
    public Customer getNextCustomer() {
        return nextCustomer;
    }

    @Override
    public void setNextCustomer(Customer nextCustomer) {
        this.nextCustomer = nextCustomer;
    }
}
