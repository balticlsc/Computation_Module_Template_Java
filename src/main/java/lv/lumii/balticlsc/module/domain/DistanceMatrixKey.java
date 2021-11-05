package lv.lumii.balticlsc.module.domain;

public class DistanceMatrixKey {
    public final String from;
    public final String to;
    public final int hashCode;

    public DistanceMatrixKey(String from, String to) {
        this.from = from;
        this.to = to;
        this.hashCode = (from + to).hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (this==obj) return true;
        if (!(obj instanceof  DistanceMatrixKey)) return  false;
        DistanceMatrixKey key = (DistanceMatrixKey) obj;
        return this.from.equals(key.from) && this.to.equals(key.to);
    }

    @Override
    public int hashCode() {
        return this.hashCode;
    }

    @Override
    public String toString() {
        return this.from + "-->" + this.to;
    }
}
