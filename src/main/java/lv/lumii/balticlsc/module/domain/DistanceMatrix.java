package lv.lumii.balticlsc.module.domain;

import java.util.HashMap;

public class DistanceMatrix {
    private HashMap<DistanceMatrixKey, Integer> distanceMatrix;

    public DistanceMatrix() {
        setDistanceMatrix(new HashMap<>());
    }

    public HashMap<DistanceMatrixKey, Integer> getDistanceMatrix() {
        return distanceMatrix;
    }

    public void addEntry(String fromId, String toId, Integer distance) {
        distanceMatrix.put(new DistanceMatrixKey(fromId, toId), distance);
    }

    public void setDistanceMatrix(HashMap<DistanceMatrixKey, Integer> distanceMatrix) {
        this.distanceMatrix = distanceMatrix;
    }

    public int getDistance(Location source, Location destination) {
        /*System.out.println(source.id + "-->" + destination.id);
        System.out.println(distanceMatrix.get(new DistanceMatrixKey(source.id, destination.id)));*/
        return distanceMatrix.get(new DistanceMatrixKey(source.id, destination.id));
    }
}
