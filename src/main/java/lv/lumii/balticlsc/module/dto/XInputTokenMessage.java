package lv.lumii.balticlsc.module.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.ArrayList;
import java.util.List;

public class XInputTokenMessage {
    @JsonProperty("MsgUid")
    private String MsgUid;
    @JsonProperty("PinName")
    private String PinName;
    @JsonProperty("Values")
    private String Values;
    @JsonProperty("AccessType")
    private String AcessType;
    @JsonProperty("TokenSeqStack")
    private List<XSeqToken> TokenSeqStack;

    public XInputTokenMessage() {
        TokenSeqStack = new ArrayList<>();
    }

    public String getMsgUid() {
        return MsgUid;
    }

    public void setMsgUid(String msgUid) {
        MsgUid = msgUid;
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

    public List<XSeqToken> getTokenSeqStack() {
        return TokenSeqStack;
    }

    public void setTokenSeqStack(List<XSeqToken> tokenSeqStack) {
        TokenSeqStack = tokenSeqStack;
    }

    public String getAcessType() {
        return AcessType;
    }

    public void setAcessType(String acessType) {
        AcessType = acessType;
    }
}
