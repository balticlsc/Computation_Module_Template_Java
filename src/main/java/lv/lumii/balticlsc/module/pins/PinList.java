package lv.lumii.balticlsc.module.pins;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

import java.util.HashMap;
import java.util.Map;

@JsonDeserialize(using = PinListDeserializer.class)
public class PinList {
    private Map<String, Pin> Pins;

    public PinList() {
        this.Pins = new HashMap<String, Pin>();
    }

    public void addPin(String key, Pin value) {
        Pins.put(key, value);
    }

    public Pin getPin(String key) {
        return Pins.getOrDefault(key, null);
    }
}
