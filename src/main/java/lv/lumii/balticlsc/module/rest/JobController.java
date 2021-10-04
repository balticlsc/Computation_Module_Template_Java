package lv.lumii.balticlsc.module.rest;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.util.JSONPObject;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
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
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;

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
    private JsonNode values;
    private Document document;

    @Autowired
    private ExecutorService taskThreadPool;

    @Autowired
    private RestTemplate restTemplate;

    @PostConstruct
    public void init() {
        try {
            logger.info("Module initialization started (version 01-OCT-2021)");
            
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

    private boolean checkToken(XInputTokenMessage token) {
        Pin pin = pinList.getPin(token.getPinName());
        if (pin == null) {
            logger.debug("no pin found in the pin list");
            return false;
        }
        if (!pin.getPinType().equalsIgnoreCase("input")) return false;

        try {
            ObjectMapper mapper = new ObjectMapper();
            values = mapper.readTree(token.getValues());
            logger.debug(values.toPrettyString());

            if (values.get("FileName") == null) return false;
            if (values.get("ObjectId") == null) return false;
            if (values.get("Database") == null) return false;
            if (values.get("Collection") == null) return false;

        } catch (JsonProcessingException e) {
            e.printStackTrace();
            return false;
        }

        return true;
    }

    private boolean checkResponse(XInputTokenMessage token) {
        return true;
    }

    private boolean checkCredentials(XInputTokenMessage token) {
        Pin pin = pinList.getPin(token.getPinName());
        HostAccessCredential ac = (HostAccessCredential) pin.getAccessCredential();
        if (ac == null) { return false; }

        try (MongoClient mongoClient = MongoClients.create(ac.getConnectionString())) {
            MongoDatabase db = mongoClient.getDatabase(values.get("Database").asText());
            MongoCollection<Document> col = db.getCollection(values.get("Collection").asText());

            document = col.find(eq("_id", new ObjectId(values.get("ObjectId").asText()))).first();
            if (document == null) return false;

        } catch (IllegalArgumentException e) {
            e.printStackTrace();
            return false;
        }

        return true;
    }



    @PostMapping("/token")
    @ResponseBody
    public ResponseEntity<String> ProcessTokenMessage(@RequestBody XInputTokenMessage token) {
        logger.debug("Token message recieved. MsgUid="+token.getMsgUid()+" PinName="+token.getPinName()+ "\n"+
                "Values= "+token.getValues());

        if (!this.checkToken(token)) {
             return ResponseEntity.badRequest()
                                  .body("corrupted-token");
        }

        if (!this.checkResponse(token)) {
            return ResponseEntity.notFound()
                                 .build();
        }

        if (!this.checkCredentials(token)) {
            return ResponseEntity.status(401)
                                  .body("bad-credentials");
        }

        Job job = new Job((output_document) -> {

            logger.debug(output_document.toJson());
            Pin pin = pinList.getPin("OPIN");
            HostAccessCredential ac = (HostAccessCredential) pin.getAccessCredential();

            String mongo_db_name = "baltic_database_"+ UUID.randomUUID().toString().substring(0,7);
            String mongo_col_name = "baltic_collection_"+ UUID.randomUUID().toString().substring(0,7);

            try (MongoClient mongoClient = MongoClients.create(ac.getConnectionString())) {

                // insert document into Mongo DB
                MongoDatabase db = mongoClient.getDatabase(mongo_db_name);
                MongoCollection<Document> col = db.getCollection(mongo_col_name);
                col.insertOne(output_document);

                Map<String, Object> values_map = new HashMap<>();
                values_map.put("FileName", output_document.getString("fileName"));
                values_map.put("ObjectId", output_document.get("_id").toString());
                values_map.put("Database", mongo_db_name);
                values_map.put("Collection", mongo_col_name);

                ObjectMapper mapper = new ObjectMapper();
                String out_values = mapper.writeValueAsString(values_map);

                XOutputTokenMessage out_token = new XOutputTokenMessage("OPIN",
                        this.getModuleUID(), out_values, token.getMsgUid(), true);

                XTokensAck out_ack_token = new XTokensAck(this.getModuleUID(),
                        token.getMsgUid(), true, false, "");

                logger.debug(out_token.toString());
                logger.debug(out_ack_token.toString());

                //PutTokenMessage
                try {
                    ResponseEntity<String> res1 = restTemplate.postForEntity(this.TokenEndpoint, out_token, String.class);
                    logger.debug(res1.getStatusCode().toString());
                } catch (Exception ex) {
                    ex.printStackTrace();
                }

                //FinalizeTokenMessageProcessing
                ResponseEntity<String> res2 = restTemplate.postForEntity(this.AckEndpoint, out_ack_token, String.class);
                logger.debug(res2.getStatusCode().toString());

            } catch (IllegalArgumentException | JsonProcessingException e) {
                e.printStackTrace();
            }

            // do NOT forget to reset all token-specific fields
            values = null;
            document = null;

            this.status = 2;
        }, document);
        Future<Integer> future = this.taskThreadPool.submit(job);

        this.status = 1;

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
