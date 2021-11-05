package lv.lumii.balticlsc.module.domain;

import org.optaplanner.core.api.domain.solution.*;
import org.optaplanner.core.api.domain.valuerange.ValueRangeProvider;
import org.optaplanner.core.api.score.buildin.hardsoft.HardSoftScore;

import java.util.ArrayList;
import java.util.List;

@PlanningSolution
public class CVRPSolution {

    private List<Vehicle> vehicles;
    private List<Customer> customers;
    private List<Location> locations;
    private DistanceMatrix distanceMatrix;
    private DistanceMatrix distanceMatrix2;

    private Location landfill;

    // NEW / SOLVING / FINISHED / ERROR / TERMINATED
    private String status;
    private long timeSpent;

    private String MsgUid;

    private HardSoftScore score;

    public CVRPSolution() {
        setVehicles(new ArrayList<>());
        setCustomers(new ArrayList<>());
        setLocations(new ArrayList<>());
        setDistanceMatrix(new DistanceMatrix());
        setDistanceMatrix2(new DistanceMatrix());
    }

    @ValueRangeProvider(id = "vehicleRange")
    @PlanningEntityCollectionProperty
    public List<Vehicle> getVehicles() {
        return vehicles;
    }

    public void setVehicles(List<Vehicle> vehicles) {
        this.vehicles = vehicles;
    }

    @ValueRangeProvider(id = "customerRange")
    @PlanningEntityCollectionProperty
    public List<Customer> getCustomers() {
        return customers;
    }

    public void setCustomers(List<Customer> customers) {
        this.customers = customers;
    }

    public Location getLandfill() {
        return landfill;
    }

    public void setLandfill(Location landfill) {
        this.landfill = landfill;
    }

    @PlanningScore
    public HardSoftScore getScore() {
        return score;
    }

    public void setScore(HardSoftScore score) {
        this.score = score;
    }

    @ProblemFactCollectionProperty
    public List<Location> getLocations() {
        return locations;
    }

    public void setLocations(List<Location> locations) {
        this.locations = locations;
    }

    @ProblemFactProperty
    public DistanceMatrix getDistanceMatrix() {
        return distanceMatrix;
    }

    public void setDistanceMatrix(DistanceMatrix distanceMatrix) {
        this.distanceMatrix = distanceMatrix;
    }

    public DistanceMatrix getDistanceMatrix2() {
        return distanceMatrix2;
    }

    public void setDistanceMatrix2(DistanceMatrix distanceMatrix) {
        this.distanceMatrix2 = distanceMatrix;
    }

    public String getMsgUid() {
        return MsgUid;
    }

    public void setMsgUid(String msgUid) {
        MsgUid = msgUid;
    }
}
