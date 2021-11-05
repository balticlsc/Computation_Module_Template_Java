package lv.lumii.balticlsc.module.domain;

public class Vehicle implements RouteElement {
    static int CURRENT_ID = 1;
    private Double capacity;
    private Location depot;
    private Location landfill;
    private Location currentLocation;
    private String id;
    private boolean ghostVehicle;

    protected Customer nextCustomer;

    public Vehicle() {}
    public Vehicle(Double capacity, Location depot, Location landfill, Location currentLocation, String id, boolean isGhost){
        this.capacity = capacity;
        this.depot = depot;
        this.landfill = landfill;
        this.currentLocation = currentLocation;
        this.id = id;
        this.ghostVehicle = isGhost;
    }


    public Double getCapacity() {
        return capacity;
    }

    public void setCapacity(Double capacity) {
        this.capacity = capacity;
    }

    public Location getDepot() {
        return depot;
    }

    public void setDepot(Location depot) {
        this.depot = depot;
    }

    public Location getLandfill() {
        return landfill;
    }

    public void setLandfill(Location landfill) {
        this.landfill = landfill;
    }
    @Override
    public Customer getNextCustomer() {
        return nextCustomer;
    }

    @Override
    public void setNextCustomer(Customer nextCustomer) {
        this.nextCustomer = nextCustomer;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public static String getNewId() {
        CURRENT_ID++;
        return "VEHICLE_"+Integer.toString(CURRENT_ID - 1);
    }

    public boolean isGhostVehicle() {
        return ghostVehicle;
    }

    public void setGhostVehicle(boolean ghostVehicle) {
        this.ghostVehicle = ghostVehicle;
    }

    public Location getCurrentLocation() {
        return currentLocation;
    }

    public void setCurrentLocation(Location currentLocation) {
        this.currentLocation = currentLocation;
    }

    @Override
    public Location getLocation() {
        return getCurrentLocation();
    }
}
