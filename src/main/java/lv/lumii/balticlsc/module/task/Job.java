package lv.lumii.balticlsc.module.task;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.graphhopper.GHRequest;
import com.graphhopper.GHResponse;
import com.graphhopper.GraphHopper;
import com.graphhopper.config.Profile;
import lv.lumii.balticlsc.module.data.DataItem;
import lv.lumii.balticlsc.module.domain.XDistance;
import lv.lumii.balticlsc.module.domain.XLocationObject;
import org.bson.Document;
import org.bson.types.Binary;
import org.bson.types.ObjectId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.client.RestTemplate;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.Callable;
import java.util.function.Consumer;

public class Job implements Callable {
    private static final Logger logger = LoggerFactory.getLogger(Job.class);

    private Consumer<DataItem> putTokenMessageFunction;
    private List<DataItem> dataItems;
    private RestTemplate restTemplate;

    public Job(Consumer<DataItem> putF, List<DataItem> in_documents, RestTemplate rt) {
        setSendTokenFunction(putF);
        setDataItems(in_documents);
        setRestTemplate(rt);
    }

    @Override
    public Object call() throws Exception {
        logger.info("Job started");

        // GeoRouter module
        // map and point_list input pins
        // distance_list output pin

        DataItem dataItem_map = getDataItems().stream().filter(dataItem -> {return dataItem.getPinName().equals("map");})
                .findFirst().get();
        DataItem dataItem_point_list = getDataItems().stream().filter(dataItem -> {return dataItem.getPinName().equals("point_list");})
                .findFirst().get();
        // TODO: Failure handling
        logger.debug(dataItem_map.getDocument().toJson());
        logger.debug(dataItem_point_list.getDocument().toJson());

        // Geo Coder requires list of points by "point_list" input pin!
        // input file should contain a json array of XLocationObject objects
        String stringContentOfPointListDataItem = extractStringDataFromDataItem(dataItem_point_list);
        ObjectMapper mapper = new ObjectMapper();
        List<XLocationObject> listOfLocations = new ArrayList<>();
        try {
            logger.debug("trying to parse file");
            listOfLocations = mapper.readValue(stringContentOfPointListDataItem, new TypeReference<List<XLocationObject>>() { });
            logger.debug("file parsed");
        } catch (JsonProcessingException ex) {
            logger.error("Bad json file in the input!");
            ex.printStackTrace();
            // TODO: job should notify the main thread about failure
        } catch (Exception ex) {
            logger.error("Error while parsing input file content");
            ex.printStackTrace();
        }

        // Geo Coder uses the GraphHopper routing engine - https://www.graphhopper.com/
        // Graphhopper requires map file (.osm.pbf or .osm.bz2) from OpenStreetMap - https://download.geofabrik.de/
        // It is required by "map" input pin!
        String mapFileName = "DEFAULT NAME REQUIRED";
        try {
            logger.debug("Saving map file ...");
            mapFileName = SaveDataItemToFile(dataItem_map);
            logger.debug("Map file saved to " + mapFileName);
        } catch (IOException ex) {
            logger.error("could not save file");
            ex.printStackTrace();
        }

        // THE MAIN JOB!!!!
        List<XDistance> distances = new ArrayList<>();
        GraphHopper hopper = new GraphHopper();
        hopper.setOSMFile(mapFileName);
        hopper.setGraphHopperLocation("/tmp/Hopper");
        hopper.setProfiles(new Profile("car").setVehicle("car").setWeighting("shortest").setTurnCosts(false));
        hopper.importOrLoad();
        logger.debug("Hopper initialized. Calculation started ...");

        for (XLocationObject loc : listOfLocations) {
            for (XLocationObject loc2: listOfLocations) {
                GHRequest req = new GHRequest(Double.parseDouble(loc.getLocation().getLatitude()), Double.parseDouble(loc.getLocation().getLongitude()),
                        Double.parseDouble(loc2.getLocation().getLatitude()), Double.parseDouble(loc2.getLocation().getLongitude()))
                                   .setProfile("car").setLocale(Locale.US);
                GHResponse rsp = hopper.route(req);
                if (!rsp.hasErrors()) {
                    XDistance distance = new XDistance(loc, loc2, rsp.getBest().getDistance());
                    distances.add(distance);
                    logger.debug("Distance between "+loc.getUID()+" and "+loc2.getUID()+"calculated. " + distance.getDistance().toString());
                }
            }
        }
        logger.debug("Distances calculated");

        // Serialization
        logger.debug("Serialization started ...");
        String outputFileContentAsString ="";
        try {
            outputFileContentAsString = mapper.writeValueAsString(distances);
        } catch (Exception ex) {
            logger.error("Error serializing distances array");
            ex.printStackTrace();
        }
        Binary outputFileContent = new Binary(outputFileContentAsString.getBytes());

        Document output_document = new Document("_id", new ObjectId());
        output_document.append("fileContent", outputFileContent);
        output_document.append("fileName","geo_router_output.out");

        DataItem output_data = new DataItem("distance_list", output_document, "123");
        logger.info("Job Ended");

        putTokenMessageFunction.accept(output_data);
        return null;
    }

    private String SaveDataItemToFile(DataItem dataItem) throws IOException {
        String fileName = dataItem.getDocument().getString("fileName");
        logger.debug(fileName);
        Binary binaryFileContent = dataItem.getDocument().get("fileContent", Binary.class);

        FileOutputStream fos = new FileOutputStream("/tmp/" + fileName);
        fos.write(binaryFileContent.getData());
        fos.close();

        return "/tmp/" + fileName;
    }

    private String extractStringDataFromDataItem(DataItem dataItem) {

        String fileName = dataItem.getDocument().getString("fileName");
        logger.debug(fileName);
        Binary binaryFileContent = dataItem.getDocument().get("fileContent", Binary.class);
        String fileContentAsString = "";
        try {
            fileContentAsString = new String(binaryFileContent.getData());
        } catch (Exception ex) {
            logger.error("Error processing file ("+ fileName +") content");
            ex.printStackTrace();
        }
        logger.debug(fileContentAsString);
        return fileContentAsString;
    }

    public Consumer<DataItem> getSendTokenFunction() {
        return putTokenMessageFunction;
    }

    public void setSendTokenFunction(Consumer<DataItem> sendTokenFunction) {
        this.putTokenMessageFunction = sendTokenFunction;
    }

    public RestTemplate getRestTemplate() {
        return restTemplate;
    }

    public void setRestTemplate(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    public List<DataItem> getDataItems() {
        return dataItems;
    }

    public void setDataItems(List<DataItem> dataItems) {
        this.dataItems = dataItems;
    }
}
