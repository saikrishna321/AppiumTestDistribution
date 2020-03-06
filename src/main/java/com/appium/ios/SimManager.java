package com.appium.ios;

import com.github.capability.CapabilityManager;
import org.json.JSONArray;
import org.json.JSONObject;

/**
 * Created by saikrisv on 27/05/16.
 */
public class SimManager {
    private CapabilityManager capabilityManager;

    public SimManager() {
        try {
            capabilityManager = CapabilityManager.getInstance();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public boolean isSimulatorObjectAvailableInCapsJson() {
        JSONArray hostMachines = capabilityManager.getCapabilitiesArrayFromKey("hostMachines");
        final boolean[] firstSimulatorObject = { false };

        if (hostMachines != null) {
            hostMachines.forEach(simulatorObject -> {
                if (((JSONObject) simulatorObject).has("simulators")) {
                    firstSimulatorObject[0] = true;
                }
            });
        }
        return firstSimulatorObject[0];
    }
}



