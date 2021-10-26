package lv.lumii.balticlsc.module.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.ArrayList;
import java.util.List;

public class XTokensAck {
    @JsonProperty("SenderUid")
    private String senderUid;
    @JsonProperty("MsgUids")
    private List<String> msgUids;
    @JsonProperty("IsFinal")
    private boolean isFinal;
    @JsonProperty("IsFailed")
    private boolean isFailed;
    @JsonProperty("Note")
    private String note;

    public XTokensAck(String senderUid, List<String> msgUids, boolean isFinal, boolean isFailed, String note) {
        setSenderUid(senderUid);
        setMsgUids(msgUids);
        setFinal(isFinal);
        setFailed(isFailed);
        setNote(note);
    }

    @Override
    public String toString() {
        return "SenderUid=" + getSenderUid() + "\nMsgUids=" +
                getMsgUids() + "\nIsFinal=" +
                isFinal() + "\nIsFailed=" +
                isFailed()+ "\nNote=" +
                getNote();
    }

    public String getSenderUid() {
        return senderUid;
    }

    public void setSenderUid(String senderUid) {
        this.senderUid = senderUid;
    }

    public List<String> getMsgUids() {
        return msgUids;
    }

    public void setMsgUids(List<String> msgUids) {
        this.msgUids = msgUids;
    }

    public boolean isFinal() {
        return isFinal;
    }

    public void setFinal(boolean aFinal) {
        isFinal = aFinal;
    }

    public boolean isFailed() {
        return isFailed;
    }

    public void setFailed(boolean failed) {
        isFailed = failed;
    }

    public String getNote() {
        return note;
    }

    public void setNote(String note) {
        this.note = note;
    }
}
