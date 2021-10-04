package lv.lumii.balticlsc.module.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public class XSeqToken {

    @JsonProperty("SeqUid")
    private String SeqUid;
    @JsonProperty("No")
    private long No;
    @JsonProperty("IsFinal")
    private boolean IsFinal;

    public String getSeqUid() {
        return SeqUid;
    }

    public void setSeqUid(String seqUid) {
        SeqUid = seqUid;
    }

    public long getNo() {
        return No;
    }

    public void setNo(long no) {
        No = no;
    }

    public boolean isFinal() {
        return IsFinal;
    }

    public void setFinal(boolean aFinal) {
        IsFinal = aFinal;
    }
}
