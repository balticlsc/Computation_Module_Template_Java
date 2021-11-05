package lv.lumii.balticlsc.module.data;

import org.bson.Document;
import org.bson.types.Binary;

import java.io.FileOutputStream;
import java.io.IOException;

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

    public String extractStringDataFromDataItem() {

        //String fileName = this.getDocument().getString("fileName");
        Binary binaryFileContent = this.getDocument().get("fileContent", Binary.class);
        String fileContentAsString = "";
        try {
            fileContentAsString = new String(binaryFileContent.getData());
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return fileContentAsString;
    }

    public String SaveDataItemToFile() throws IOException {
        String fileName = this.getDocument().getString("fileName");
        Binary binaryFileContent = this.getDocument().get("fileContent", Binary.class);

        FileOutputStream fos = new FileOutputStream("/tmp/" + fileName);
        fos.write(binaryFileContent.getData());
        fos.close();

        return "/tmp/" + fileName;
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
