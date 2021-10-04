package lv.lumii.balticlsc.module.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public class XJobStatus {
    @JsonProperty("Status")
    private byte status; // Idle=0, Working=1, Completed=2, Failed=3
    @JsonProperty("JobProgress")
    private long jobProgress;

    public XJobStatus() {}

    public XJobStatus(byte status, long progress) {
        setStatus(status);
        setJobProgress(progress);
    }

    public byte getStatus() {
        return status;
    }

    public void setStatus(byte status) {
        this.status = status;
    }

    public long getJobProgress() {
        return jobProgress;
    }

    public void setJobProgress(long jobProgress) {
        this.jobProgress = jobProgress;
    }
}
