package lv.lumii.balticlsc.module.task;

import org.bson.BsonBinary;
import org.bson.Document;
import org.bson.types.Binary;
import org.bson.types.ObjectId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.Callable;
import java.util.function.Consumer;

public class Job implements Callable {
    private static final Logger logger = LoggerFactory.getLogger(Job.class);

    private Consumer<Document> callbackFunction;
    private Document document;

    public Job(Consumer<Document> callback, Document document) {
        setCallbackFunction(callback);
        setDocument(document);
    }

    @Override
    public Object call() throws Exception {
        logger.info("Job started");
        logger.debug(document.toJson());

        String fileName = document.getString("fileName");
        logger.debug(fileName);
        Binary fileContent = document.get("fileContent", Binary.class);
        logger.debug(fileContent.toString());
        String fileContentAsString = new String(fileContent.getData());

        logger.debug(fileContentAsString);
        fileContentAsString = "Output from java module!";

        fileContent = new Binary(fileContentAsString.getBytes());

        Document output_document = new Document("_id", new ObjectId());
        output_document.append("fileContent", fileContent);
        output_document.append("fileName","java_module_output.out");

        logger.info("Job Ended");
        callbackFunction.accept(output_document);
        return null;
    }

    public Consumer<Document> getCallbackFunction() {
        return callbackFunction;
    }

    public void setCallbackFunction(Consumer<Document> callbackFunction) {
        this.callbackFunction = callbackFunction;
    }

    public Document getDocument() {
        return document;
    }

    public void setDocument(Document document) {
        this.document = document;
    }
}
