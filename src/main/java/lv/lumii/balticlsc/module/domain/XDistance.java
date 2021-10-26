package lv.lumii.balticlsc.module.domain;

public class XDistance {
    private XLocationObject from;
    private XLocationObject to;
    private Double distance;

    public XDistance() {}

    public XDistance(XLocationObject fromObject, XLocationObject toObject, Double distance) {
        setDistance(distance);
        setFrom(fromObject);
        setTo(toObject);
    }

    public XLocationObject getFrom() {
        return from;
    }

    public void setFrom(XLocationObject from) {
        this.from = from;
    }

    public XLocationObject getTo() {
        return to;
    }

    public void setTo(XLocationObject to) {
        this.to = to;
    }

    public Double getDistance() {
        return distance;
    }

    public void setDistance(Double distance) {
        this.distance = distance;
    }
}
