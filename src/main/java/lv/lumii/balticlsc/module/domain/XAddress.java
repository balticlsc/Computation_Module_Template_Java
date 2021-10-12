package lv.lumii.balticlsc.module.domain;

public class XAddress {
    private String country;
    private String administrativeArea; // region
    private String locality; // city
    private String postalCode;
    private String thoroughfare; // street address
    private String premise; // apartment

    public XAddress() {}

    @Override
    public String toString() {
        return this.getThoroughfare() + ", " +
                this.getPostalCode() + ", " +
                this.getLocality() + ", " +
                this.getCountry();
    }

    public String getCountry() {
        return country;
    }

    public void setCountry(String country) {
        this.country = country;
    }

    public String getAdministrativeArea() {
        return administrativeArea;
    }

    public void setAdministrativeArea(String administrativeArea) {
        this.administrativeArea = administrativeArea;
    }

    public String getLocality() {
        return locality;
    }

    public void setLocality(String locality) {
        this.locality = locality;
    }

    public String getPostalCode() {
        return postalCode;
    }

    public void setPostalCode(String postalCode) {
        this.postalCode = postalCode;
    }

    public String getThoroughfare() {
        return thoroughfare;
    }

    public void setThoroughfare(String thoroughfare) {
        this.thoroughfare = thoroughfare;
    }

    public String getPremise() {
        return premise;
    }

    public void setPremise(String premise) {
        this.premise = premise;
    }

}
