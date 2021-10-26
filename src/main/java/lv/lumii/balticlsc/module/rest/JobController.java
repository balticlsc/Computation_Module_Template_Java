package lv.lumii.balticlsc.module.rest;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import lv.lumii.balticlsc.module.data.DataItem;
import lv.lumii.balticlsc.module.dto.XInputTokenMessage;
import lv.lumii.balticlsc.module.dto.XJobStatus;
import lv.lumii.balticlsc.module.dto.XOutputTokenMessage;
import lv.lumii.balticlsc.module.dto.XTokensAck;
import lv.lumii.balticlsc.module.pins.HostAccessCredential;
import lv.lumii.balticlsc.module.pins.Pin;
import lv.lumii.balticlsc.module.pins.PinList;
import lv.lumii.balticlsc.module.task.Job;
import org.bson.Document;
import org.bson.types.ObjectId;
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

    @Autowired
    private ExecutorService taskThreadPool;

    @Autowired
    private RestTemplate restTemplate;

    @PostConstruct
    public void init() {
        try {
            logger.info("Module initialization started (version 26-OCT-2021 Geo Router 1)");
            
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

            Job job = new Job((output_data_item) -> {

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
                    ObjectMapper mapper = new ObjectMapper();
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
            }, all_data_items, restTemplate);
            Future<Integer> future = this.taskThreadPool.submit(job);
            this.status = 1; // Working - not all input tokens processed

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
