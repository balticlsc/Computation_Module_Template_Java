package lv.lumii.balticlsc.module.data;

import org.bson.Document;

public class DataItem {
    private String pinName;
    private Document document;
    private String msgId;

    public DataItem() {}

    public DataItem(String pinName, Document document, String msgId) {
        setDocument(document);
        setMsgId(msgId);
        setPinName(pinName);
    }

    public String getPinName() {
        return pinName;
    }

    public void setPinName(String pinName) {
        this.pinName = pinName;
    }

    public Document getDocument() {
        return document;
    }

    public void setDocument(Document document) {
        this.document = document;
    }

    public String getMsgId() {
        return msgId;
    }

    public void setMsgId(String msgId) {
        this.msgId = msgId;
    }
}
