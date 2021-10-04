package lv.lumii.balticlsc.module.pins;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import com.fasterxml.jackson.databind.node.ArrayNode;

import java.io.IOException;

public class PinListDeserializer extends StdDeserializer<PinList> {

    public PinListDeserializer() {
        this(null);
    }

    protected PinListDeserializer(Class<?> vc) {
        super(vc);
    }

    @Override
    public PinList deserialize(JsonParser jsonParser, DeserializationContext deserializationContext) throws IOException, JsonProcessingException {
        PinList pinList = new PinList();
        JsonNode jsonRoot = jsonParser.getCodec().readTree(jsonParser);

        ArrayNode jsonPins = (ArrayNode) jsonRoot.get("Pins");
        for (JsonNode jsonPin : jsonPins) {
            Pin pin = new Pin();
            pin.setPinName(jsonPin.get("PinName").asText());
            pin.setPinType(jsonPin.get("PinType").asText());
            pin.setDataMultiplicity(jsonPin.get("DataMultiplicity").asText());
            pin.setTokenMultiplicity(jsonPin.get("TokenMultiplicity").asText());
            pin.setAccessType(jsonPin.get("AccessType").asText());

            JsonNode jsonAccessCredential = jsonPin.get("AccessCredential");
            JsonNode jsonAccessPath = jsonPin.get("AccessPath");

            if (jsonAccessCredential != null) {
                switch(pin.getAccessType())
                {
                    case "FTP":
                    case "MySQL":
                    case "RelationalDB":
                    case "NoSQL_DB":
                    case "MongoDB":
                        pin.setAccessCredential(new HostAccessCredential(
                                jsonAccessCredential.get("Host").asText(),
                                jsonAccessCredential.get("Port").asText(),
                                jsonAccessCredential.get("User").asText(),
                                jsonAccessCredential.get("Password").asText()
                        ));
                        break;
                    default:
                        throw new JsonProcessingException("Can not determine pin("+ pin.getPinName() +") access type"){};
                }
            }

            if (jsonAccessPath != null ) {
                switch(pin.getAccessType())
                {
                    case "FTP":
                    case "MySQL":
                    case "RelationalDB":
                    case "NoSQL_DB":
                        pin.setAccessPath(new ResourceAccessPath(
                                jsonAccessPath.get("ResourcePath").asText()
                        ));
                        break;
                    case "MongoDB":
                        pin.setAccessPath(new MongoAccessPath(
                                jsonAccessPath.get("Database").asText(),
                                jsonAccessPath.get("Collection").asText(),
                                jsonAccessPath.get("ObjectId").asText()
                        ));
                        break;
                    default:
                        throw new JsonProcessingException("Can not determine pin("+ pin.getPinName() +") access type"){};
                }
            }

            pinList.addPin(pin.getPinName(), pin);
        }

        return pinList;
    }
}
