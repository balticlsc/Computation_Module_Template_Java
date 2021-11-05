package lv.lumii.balticlsc.module.domain;

public class XGeoWasteLogisticsObject extends XAddressable {
    private Integer type; // 0 - waste field, 1 - vehicle, 2 - customer
    private Double capacity; // for customer
    private Double maxCapacity; // for vehicle

    public XGeoWasteLogisticsObject() {}

    public XGeoWasteLogisticsObject(String idx, Integer type) {
        setUID(idx);
        setType(type);
    }


    public Integer getType() {
        return type;
    }

    public void setType(Integer type) {
        this.type = type;
    }

    public Double getCapacity() {
        return capacity;
    }

    public void setCapacity(Double capacity) {
        this.capacity = capacity;
    }

    public Double getMaxCapacity() {
        return maxCapacity;
    }

    public void setMaxCapacity(Double maxCapacity) {
        this.maxCapacity = maxCapacity;
    }
}
