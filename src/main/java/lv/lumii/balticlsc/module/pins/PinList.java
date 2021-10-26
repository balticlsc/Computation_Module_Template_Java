package lv.lumii.balticlsc.module.pins;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

import java.util.*;
import java.util.stream.Collectors;

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

    public Collection<Pin> getPins() { return Pins.values(); }

    public Collection<Pin> getInputPins() {return Pins.values().stream()
                                                 .filter(pin -> { return pin.getPinType().equals("input");})
                                                 .collect(Collectors.toList());};

    public Collection<Pin> getOutputPins() {return Pins.values().stream()
            .filter(pin -> { return pin.getPinType().equals("output");})
            .collect(Collectors.toList());};
}
