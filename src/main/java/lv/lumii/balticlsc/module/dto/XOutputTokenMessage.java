package lv.lumii.balticlsc.module.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public class XOutputTokenMessage {
    @JsonProperty("BaseMsgUid")
    private String BaseMsgUid;
    @JsonProperty("PinName")
    private String PinName;
    @JsonProperty("Values")
    private String Values;
    @JsonProperty("SenderUid")
    private String SenderUid;
    @JsonProperty("IsFinal")
    private boolean IsFinal;

    public XOutputTokenMessage(String pinName, String senderUid, String values, String baseMsgUid, boolean isFinal) {
        setPinName(pinName);
        setSenderUid(senderUid);
        setValues(values);
        setBaseMsgUid(baseMsgUid);
        setFinal(isFinal);
    }

    @Override
    public String toString() {
        return "BaseMsgUid=" + getBaseMsgUid() + "\nPinName=" +
                getPinName() + "\nValues=" +
                getSenderUid() + "\nSenderUid=" +
                getValues() + "\nIsFinal=" +
                isFinal();
    }

    public String getBaseMsgUid() {
        return BaseMsgUid;
    }

    public void setBaseMsgUid(String baseMsgUid) {
        BaseMsgUid = baseMsgUid;
    }

    public String getPinName() {
        return PinName;
    }

    public void setPinName(String pinName) {
        PinName = pinName;
    }

    public String getValues() {
        return Values;
    }

    public void setValues(String values) {
        Values = values;
    }

    public String getSenderUid() {
        return SenderUid;
    }

    public void setSenderUid(String senderUid) {
        SenderUid = senderUid;
    }

    public boolean isFinal() {
        return IsFinal;
    }

    public void setFinal(boolean aFinal) {
        IsFinal = aFinal;
    }
}
