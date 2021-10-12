package lv.lumii.balticlsc.module.task;

import com.fasterxml.jackson.core.JsonGenerationException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lv.lumii.balticlsc.module.domain.XAddressable;
import lv.lumii.balticlsc.module.domain.XLocation;
import lv.lumii.balticlsc.module.domain.XLocationObject;
import org.bson.Document;
import org.bson.types.Binary;
import org.bson.types.ObjectId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.function.Consumer;

public class Job implements Callable {
    private static final Logger logger = LoggerFactory.getLogger(Job.class);

    private Consumer<Document> callbackFunction;
    private Document document;
    private RestTemplate restTemplate;

    public Job(Consumer<Document> callback, Document document, RestTemplate rt) {
        setCallbackFunction(callback);
        setDocument(document);
        setRestTemplate(rt);
    }

    @Override
    public Object call() throws Exception {
        logger.info("Job started");
        logger.debug(document.toJson());

        String fileName = document.getString("fileName");
        logger.debug(fileName);
        Binary fileContent = document.get("fileContent", Binary.class);
        logger.debug(fileContent.toString());
        String fileContentAsString = "";
        try {
            fileContentAsString = new String(fileContent.getData());
        } catch (Exception ex) {
            logger.error("Error processing file content");
            ex.printStackTrace();
        }
        logger.debug(fileContentAsString);

        // Geocoding finds lattitude and longitude (coordinates) for the given address list
        // For testing purposes the OpenCage Geocoding API is used https://opencagedata.com/ap
        // It has free 2500 requests per day with a rate no more 1 per second

        // How to use:
        // https://api.opencagedata.com/geocode/v1/json?q=PLACENAME&key=f1b0e61e49a94cb8969849a4d5d6ed76
        // key belongs to Agris Šostaks  --> key should be provided as module env variable?

        // input file should contain a json array of XAddressable objects
        ObjectMapper mapper = new ObjectMapper();
        List<XAddressable> listOfAddressables = new ArrayList<>();
        try {
            logger.debug("trying to parse file");
            listOfAddressables = mapper.readValue(fileContentAsString, new TypeReference<List<XAddressable>>() { });
            logger.debug("file parsed");
        } catch (JsonProcessingException ex) {
            logger.error("Bad json file in the input!");
            ex.printStackTrace();
            // TODO: job should notify the main thread about failure
        } catch (Exception ex) {
            logger.error("Error while parsing input file content");
            ex.printStackTrace();
        }

        // For each addressable call the OpenCage API - look for a second timeout!
        List<XLocationObject> locations = new ArrayList<>();
        for (XAddressable ad : listOfAddressables) {
            Thread.sleep(1000); // Have to wait a second because of request limit in OpenCage ...
            logger.debug("object:" + ad.toString());
            XLocation loc = new XLocation();
            XLocationObject loc_obj = new XLocationObject();
            loc_obj.setUID(ad.getUID()); // Pass the UID further
            loc_obj.setLocation(loc);
            try {
                String open_cage_uri = "https://api.opencagedata.com/geocode/v1/json?q="+ad.getAddress()+"&key=f1b0e61e49a94cb8969849a4d5d6ed76";
                logger.debug(open_cage_uri);
                ResponseEntity<String> response = getRestTemplate().getForEntity(open_cage_uri, String.class);
                logger.debug(response.getStatusCode().toString());
                logger.debug(response.getBody());

                if (response.getStatusCode().equals(HttpStatus.OK)) {
                    try {
                        JsonNode jsonLocation = mapper.readTree(response.getBody());
                        String lat = jsonLocation.get("results").get(0).get("geometry").get("lat").asText();
                        String lon = jsonLocation.get("results").get(0).get("geometry").get("lng").asText();
                        loc.setLatitude(lat); // get the lattitude --> results[0].geometry.lat
                        loc.setLongitude(lon); // get the longitude --> results[0].geometry.lng
                    } catch (JsonProcessingException ex) {
                        logger.error("Error parsing OpenCage response JSON!");
                        ex.printStackTrace();
                    } catch (NullPointerException ex) {
                        logger.error("Could not find required fields");
                        ex.printStackTrace();
                    }
                }
            } catch (Exception ex) {
                ex.printStackTrace();
            }
            locations.add(loc_obj);
        }

        // locations contains the result which should be deserialized and put into fileContentAsString

        // The most viable alternative is open source Nominatim https://nominatim.org/release-docs/latest/admin/Installation/
        // It requires setting up your own service on your own premises or developing and adapting the fork for BalticLSC
        try {
            fileContentAsString = mapper.writeValueAsString(locations);
        } catch (Exception ex) {
            logger.error("Error serializing locations array");
            ex.printStackTrace();
        }
        //fileContentAsString = "Output from java module!";

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

    public RestTemplate getRestTemplate() {
        return restTemplate;
    }

    public void setRestTemplate(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }
}
