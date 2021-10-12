package lv.lumii.balticlsc.module.domain;

public class XAddressable {
    private String UID;
    private XAddress address;

    public XAddressable() {}

    @Override
    public String toString() {
        return this.getUID() + " : " + getAddress();
    }

    public String getUID() {
        return UID;
    }

    public void setUID(String UID) {
        this.UID = UID;
    }

    public XAddress getAddress() {
        return address;
    }

    public void setAddress(XAddress address) {
        this.address = address;
    }
}
