package lv.lumii.balticlsc.module.domain;

public class XLocationObject {
    private String UID;
    private XLocation location;

    public XLocationObject() {}

    public String getUID() {
        return UID;
    }

    public void setUID(String UID) {
        this.UID = UID;
    }

    public XLocation getLocation() {
        return location;
    }

    public void setLocation(XLocation location) {
        this.location = location;
    }
}
