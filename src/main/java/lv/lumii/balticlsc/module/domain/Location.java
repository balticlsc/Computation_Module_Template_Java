package lv.lumii.balticlsc.module.domain;

public class Location {
    static final Double PIdiv180 = Math.PI / 180;
    static final Double EARTH_RADIUS = 6371.0;

    private Double deg2rad(Double deg) { return deg * PIdiv180; };
    public Double lat;
    public Double lon;
    public String id;

    public Location() {}
    public Location(Double lat, Double lon, String idx) {
        this.lat = lat;
        this.lon = lon;
        this.id = idx;
    }

    public Location(Double lat, Double lon) {
        this.lat = lat;
        this.lon = lon;
    }
    public int getDistance(Location destination, DistanceMatrix matrix) {
        /*System.out.println(matrix);
        System.out.println(matrix.getDistanceMatrix().keySet().size());
        for (DistanceMatrixKey name: matrix.getDistanceMatrix().keySet()) {
            String key = name.toString();
            String value = matrix.getDistanceMatrix().get(name).toString();
            System.out.println(key + " " + value);
        };
        System.out.println(this.id + "-->" + destination.id);*/
        return matrix.getDistance(this, destination);
    }

    public int getAirDistance(Location destination) {
        Double dLat = deg2rad(destination.lat-lat);
        Double dLon = deg2rad(destination.lon-lon);

        Double a = Math.sin(dLat/2.0) * Math.sin(dLat/2.0) +
                   Math.cos(deg2rad(lat)) * Math.cos(deg2rad(destination.lat)) *
                   Math.sin(dLon/2.0) * Math.sin(dLon/2.0);
        Double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1-a));

        return (int) (1000 * EARTH_RADIUS * c);
    };
}
