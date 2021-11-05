package lv.lumii.balticlsc.module.rest;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import lv.lumii.balticlsc.module.data.DataItem;
import lv.lumii.balticlsc.module.domain.*;
import lv.lumii.balticlsc.module.dto.XInputTokenMessage;
import lv.lumii.balticlsc.module.dto.XJobStatus;
import lv.lumii.balticlsc.module.dto.XOutputTokenMessage;
import lv.lumii.balticlsc.module.dto.XTokensAck;
import lv.lumii.balticlsc.module.pins.HostAccessCredential;
import lv.lumii.balticlsc.module.pins.Pin;
import lv.lumii.balticlsc.module.pins.PinList;
import lv.lumii.balticlsc.module.task.Job;
import org.bson.Document;
import org.bson.types.Binary;
import org.bson.types.ObjectId;
import org.optaplanner.core.api.solver.SolverJob;
import org.optaplanner.core.api.solver.SolverManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

import javax.annotation.PostConstruct;
import javax.xml.crypto.Data;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.stream.Collectors;

import static com.mongodb.client.model.Filters.eq;

@Controller
@RequestMapping("/")
public class JobController {

    private static final Logger logger = LoggerFactory.getLogger(JobController.class);

    @Value("${SYS_MODULE_INSTANCE_UID:foo}")
    private String ModuleUID;
    @Value("${SYS_BATCH_MANAGER_TOKEN_ENDPOINT:foo}")
    private String TokenEndpoint;
    @Value("${SYS_BATCH_MANAGER_ACK_ENDPOINT:foo}")
    private String AckEndpoint;
    @Value("${SYS_PIN_CONFIG_FILE_PATH:foo}")
    private String PinConfigPath;// = "pin.config";

    private PinList pinList = new PinList();
    private byte status;

    private List<DataItem> dataItems = new ArrayList<>();

    // Geo Waste Logistics Optimizer is module which optimizes routes for the waste collecting trucks
    // It recieves the geo objects - clients, waste fields, and vehicle depots.
    // It recieves the distance matrix between these objects.
    // It calculates the sequence the objects should be visited

    // It uses OptaPlanner.org open source library
    // OptaPlanner implements all the threading issues, therefore implementation of the module is non-standard (simpler)

    //@Autowired
    //private ExecutorService taskThreadPool;
    @Autowired
    private SolverManager<CVRPSolution, Long> solverManager;

    @Autowired
    private RestTemplate restTemplate;

    @PostConstruct
    public void init() {
        try {
            logger.info("Module initialization started (version 05-NOV-2021 Geo Optimizer 2)");
            
            logger.debug("Module environment variables: ");
            logger.debug("SYS_MODULE_INSTANCE_UID = " + ModuleUID);
            logger.debug("SYS_BATCH_MANAGER_TOKEN_ENDPOINT = " + TokenEndpoint);
            logger.debug("SYS_BATCH_MANAGER_ACK_ENDPOINT = " + AckEndpoint);
            logger.debug("SYS_PIN_CONFIG_FILE_PATH = " + PinConfigPath);

            byte[] pinConfigFileContent = Files.readAllBytes(Paths.get(getPinConfigPath()));
            String pinConfigFileContentString = new String(pinConfigFileContent, StandardCharsets.UTF_8);
            String pinConfigFileContentStringExtended = "{\"Pins\":" + pinConfigFileContentString + "}";
            logger.debug(pinConfigFileContentStringExtended);
            pinList = new ObjectMapper().readValue(pinConfigFileContentStringExtended, PinList.class);
            logger.debug(pinList.toString());

            status = 2; // Completed/Ready

            logger.info("Module initialized.");

        } catch (NoSuchFileException e) {
            logger.error("Module initialization failed - no such file " + getPinConfigPath());
            e.printStackTrace();
        } catch (JsonProcessingException e) {
            logger.error("Module initialization failed - bad configuration file");
        } catch (Exception e) {
            logger.error("Module initialization failed");
            e.printStackTrace();
        }
    }

    // Returns 0=OK, 1=corrupted-token, 2=bad-credentials
    private Integer checkTokenCompleteness(XInputTokenMessage token) {
        Pin pin = pinList.getPin(token.getPinName());
        if (pin == null) {
            logger.debug("no pin found in the pin list");
            return 1;
        }
        if (!pin.getPinType().equalsIgnoreCase("input")) return 1;

        try {
            ObjectMapper mapper = new ObjectMapper();
            JsonNode values = mapper.readTree(token.getValues());
            logger.debug(values.toPrettyString());

            if (values.get("FileName") == null) return 1;
            if (values.get("ObjectId") == null) return 1;
            if (values.get("Database") == null) return 1;
            if (values.get("Collection") == null) return 1;

            HostAccessCredential ac = (HostAccessCredential) pin.getAccessCredential();
            if (ac == null) { return 2; }

            try (MongoClient mongoClient = MongoClients.create(ac.getConnectionString())) {
                MongoDatabase db = mongoClient.getDatabase(values.get("Database").asText());
                MongoCollection<Document> col = db.getCollection(values.get("Collection").asText());

                Document document = col.find(eq("_id", new ObjectId(values.get("ObjectId").asText()))).first();
                if (document == null) return 2;

                dataItems.add(new DataItem(pin.getPinName(), document, token.getMsgUid()));

            } catch (IllegalArgumentException e) {
                e.printStackTrace();
                return 2;
            }

        } catch (JsonProcessingException e) {
            e.printStackTrace();
            return 1;
        }

        return 0;
    }



    @PostMapping("/token")
    @ResponseBody
    public ResponseEntity<String> ProcessTokenMessage(@RequestBody XInputTokenMessage token) {
        logger.debug("Token message recieved. MsgUid="+token.getMsgUid()+" PinName="+token.getPinName()+ "\n"+
                "Values= "+token.getValues());

        switch (this.checkTokenCompleteness(token)) {
            case 1:
                return ResponseEntity.badRequest()
                        .body("corrupted-token");
            case 2:
                return ResponseEntity.status(401)
                        .body("bad-credentials");
            case 3:
                return ResponseEntity.notFound()
                        .build();
            default:
        }

        // Since we can have multiple input tokens, we should ensure that all of them are here and then fire the job!
        if (pinList.getInputPins().stream()
                                      .allMatch(pin -> {return dataItems.stream().
                                                            anyMatch(dataItem -> {return dataItem.getPinName().equals(pin.getPinName());}); })) {

            List<DataItem> all_data_items = pinList.getInputPins().stream()
                    .map(pin -> { return dataItems.stream().filter(dataItem -> { return dataItem.getPinName().equals(pin.getPinName());})
                                                           .findFirst().get();})
                    .collect(Collectors.toList());

            // Now we have to process inputs!!!!
            // DistanceMatrix pin  will have a List<XDistance>
            DataItem dataItem_DM = all_data_items.stream().filter(dataItem -> {return dataItem.getPinName().equals("DistanceMatrix");})
                    .findFirst().get();

            String stringContentOfDistanceListDataItem = dataItem_DM.extractStringDataFromDataItem();
            logger.debug(stringContentOfDistanceListDataItem);
            ObjectMapper mapper = new ObjectMapper();
            List<XDistance> listOfDistances = new ArrayList<>();
            try {
                logger.debug("trying to parse file");
                listOfDistances = mapper.readValue(stringContentOfDistanceListDataItem, new TypeReference<List<XDistance>>() { });
                logger.debug("file parsed");
            } catch (JsonProcessingException ex) {
                logger.error("Bad json file in the input!");
                ex.printStackTrace();
                // TODO: job should notify the main thread about failure
            } catch (Exception ex) {
                logger.error("Error while parsing input file content");
                ex.printStackTrace();
            }

            // AllGeoObjects pin will have all WasteObjects - Vehicles, Customers and Waste Fields
            DataItem dataItem_GO = all_data_items.stream().filter(dataItem -> {return dataItem.getPinName().equals("AllGeoObjects");})
                    .findFirst().get();

            String stringContentOfGeoObjectListDataItem = dataItem_GO.extractStringDataFromDataItem();
            List<XGeoWasteLogisticsObject> listOfGeoObjects = new ArrayList<>();
            try {
                logger.debug("trying to parse file");
                listOfGeoObjects = mapper.readValue(stringContentOfGeoObjectListDataItem, new TypeReference<List<XGeoWasteLogisticsObject>>() { });
                logger.debug("file parsed");
            } catch (JsonProcessingException ex) {
                logger.error("Bad json file in the input!");
                ex.printStackTrace();
                // TODO: job should notify the main thread about failure
            } catch (Exception ex) {
                logger.error("Error while parsing input file content");
                ex.printStackTrace();
            }

            // Prepare CVRP Solution
            CVRPSolution problem = new CVRPSolution();

            // Set-up distance matrix
            DistanceMatrix DM = new DistanceMatrix();
            for (XDistance distObj: listOfDistances) {
                logger.debug("Found distance!"+distObj.toString());
                DM.addEntry(distObj.getFrom().getUID(), distObj.getTo().getUID(), distObj.getDistance().intValue());
            }
            problem.setDistanceMatrix(DM);
            logger.debug("DM Set!");
            // add all geo objects
            for (XGeoWasteLogisticsObject obj: listOfGeoObjects) {
                logger.debug("Found geo object! "+obj.toString());
                switch (obj.getType()) {
                    case 0:
                        // Waste Field (we assume that there is only one wastefield in the area)
                        Location wasteField = new Location();
                        wasteField.id = obj.getUID();
                        problem.getLocations().add(wasteField);
                        problem.setLandfill(wasteField);
                        break;
                    case 1:
                        // Vehicle
                        Vehicle vehicle = new Vehicle();
                        vehicle.setId(obj.getUID());
                        Location depot = new Location();
                        depot.id = obj.getUID();
                        problem.getLocations().add(depot);
                        vehicle.setDepot(depot);
                        vehicle.setCurrentLocation(depot);
                        vehicle.setCapacity(obj.getMaxCapacity());
                        vehicle.setGhostVehicle(false);
                        problem.getVehicles().add(vehicle);
                        break;
                    case 2:
                        // Customer
                        Customer customer = new Customer();
                        customer.setDemand(obj.getCapacity());
                        Location loc = new Location();
                        loc.id = obj.getUID();
                        customer.setLocation(loc);
                        problem.getLocations().add(loc);
                        problem.getCustomers().add(customer);
                        break;
                    default:
                        logger.error("Geo Object has unidentified type!");
                }
            }
            //add waste field to vehicles
            problem.getVehicles().forEach(v -> {v.setLandfill(problem.getLandfill());});
            logger.debug("Problem ready!");

            SolverJob<CVRPSolution, Long> job = solverManager.solve(1l, problem,
                    solution -> {
                        List<List<XGeoWasteLogisticsObject>> listOfRoutes = new ArrayList<>();
                        solution.getVehicles().forEach(v -> {
                                List<XGeoWasteLogisticsObject> route = new ArrayList<>();
                                XGeoWasteLogisticsObject vehicle = new XGeoWasteLogisticsObject(v.getCurrentLocation().id, 1);
                                vehicle.setMaxCapacity(v.getCapacity());
                                route.add(vehicle);
                                Customer nextCustomer = v.getNextCustomer();
                                while (nextCustomer != null) {
                                    XGeoWasteLogisticsObject customer = new XGeoWasteLogisticsObject(nextCustomer.getLocation().id, 2);
                                    customer.setCapacity(nextCustomer.getDemand());
                                    route.add(customer);
                                    nextCustomer = nextCustomer.getNextCustomer();
                                }
                                XGeoWasteLogisticsObject wasteField = new XGeoWasteLogisticsObject(solution.getLandfill().id, 0);
                                route.add(wasteField);
                                listOfRoutes.add(route);
                        });

                        String outputFileContentAsString ="";
                        try {
                            outputFileContentAsString = mapper.writeValueAsString(listOfRoutes);
                        } catch (Exception ex) {
                            logger.error("Error serializing routes array");
                            ex.printStackTrace();
                        }
                        Binary outputFileContent = new Binary(outputFileContentAsString.getBytes());

                        Document output_document = new Document("_id", new ObjectId());
                        output_document.append("fileContent", outputFileContent);
                        output_document.append("fileName","geo_waste_logistics_opt.out");

                        DataItem output_data_item = new DataItem("Routes", output_document, "123");

                        dataItems.add(output_data_item);

                        Pin opin = pinList.getPin(output_data_item.getPinName());
                        HostAccessCredential ac = (HostAccessCredential) opin.getAccessCredential();

                        String mongo_db_name = "baltic_database_"+ UUID.randomUUID().toString().substring(0,7);
                        String mongo_col_name = "baltic_collection_"+ UUID.randomUUID().toString().substring(0,7);

                        try (MongoClient mongoClient = MongoClients.create(ac.getConnectionString())) {

                            // insert document into Mongo DB
                            MongoDatabase db = mongoClient.getDatabase(mongo_db_name);
                            MongoCollection<Document> col = db.getCollection(mongo_col_name);
                            col.insertOne(output_data_item.getDocument());

                            // prepare OutputTokenMessage
                            Map<String, Object> values_map = new HashMap<>();
                            values_map.put("FileName", output_data_item.getDocument().getString("fileName"));
                            values_map.put("ObjectId", output_data_item.getDocument().get("_id").toString());
                            values_map.put("Database", mongo_db_name);
                            values_map.put("Collection", mongo_col_name);
                            String out_values = mapper.writeValueAsString(values_map);

                            XOutputTokenMessage out_token = new XOutputTokenMessage(output_data_item.getPinName(),
                                    this.getModuleUID(), out_values, token.getMsgUid(), true);
                            logger.debug(out_token.toString());

                            //Send PutTokenMessage
                            try {
                                ResponseEntity<String> res1 = restTemplate.postForEntity(this.TokenEndpoint, out_token, String.class);
                                logger.debug(res1.getStatusCode().toString());
                            } catch (Exception ex) {
                                ex.printStackTrace();
                            }
                            // if all output tokens have been sent - send ack message
                            if (pinList.getOutputPins().stream()
                                    .allMatch(pin -> {return dataItems.stream().
                                            anyMatch(dataItem -> {return dataItem.getPinName().equals(pin.getPinName());}); })) {

                                List<DataItem> input_data_items = pinList.getInputPins().stream()
                                        .map(pin -> { return dataItems.stream()
                                                .filter(dataItem  -> {return dataItem.getPinName().equals(pin.getPinName());}).findFirst().get();})
                                        .collect(Collectors.toList());

                                List<String> ack_tocken_ids = input_data_items.stream()
                                        .map(data_item -> { return data_item.getMsgId();})
                                        .collect(Collectors.toList());

                                // Prepare Tokens Ack Message
                                XTokensAck out_ack_token = new XTokensAck(this.getModuleUID(),
                                        ack_tocken_ids, true, false, "");
                                logger.debug(out_ack_token.toString());

                                //FinalizeTokenMessageProcessing
                                ResponseEntity<String> res2 = restTemplate.postForEntity(this.AckEndpoint, out_ack_token, String.class);
                                logger.debug(res2.getStatusCode().toString());

                                // do NOT forget to reset all token-specific fields
                                pinList.getPins().forEach(pin -> {
                                    dataItems.clear();
                                });

                                this.status = 2;
                            }
                        } catch (IllegalArgumentException | JsonProcessingException e) {
                            e.printStackTrace();
                        }
                    });

            /* Job job = new Job((output_data_item) -> {


            }, all_data_items, restTemplate);
            Future<Integer> future = this.taskThreadPool.submit(job);
            */

            this.status = 1; // Working

        } else {
            this.status = 0; // Idle - Waiting for data
        }

        return ResponseEntity.ok()
                             .body("OK");
    }


    @GetMapping("/status")
    @ResponseBody
    public ResponseEntity<XJobStatus> solution() {
        logger.debug("Status asked");
        return ResponseEntity.ok()
                             .body(new XJobStatus(this.status, 0));
    }


// GETTERS & SETTERS
    public String getModuleUID() {
        return ModuleUID;
    }

    public void setModuleUID(String moduleUID) {
        ModuleUID = moduleUID;
    }

    public String getTokenEndpoint() {
        return TokenEndpoint;
    }

    public void setTokenEndpoint(String tokenEndpoint) {
        TokenEndpoint = tokenEndpoint;
    }

    public String getAckEndpoint() {
        return AckEndpoint;
    }

    public void setAckEndpoint(String ackEndpoint) {
        AckEndpoint = ackEndpoint;
    }

    public String getPinConfigPath() {
        return PinConfigPath;
    }

    public void setPinConfigPath(String pinConfigPath) {
        PinConfigPath = pinConfigPath;
    }
}
