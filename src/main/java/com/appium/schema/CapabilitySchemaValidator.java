package com.appium.schema;

import java.io.InputStream;
import java.net.ConnectException;
import java.net.InetAddress;
import java.util.logging.Logger;

import com.appium.utils.FigletHelper;
import com.github.capability.CapabilityManager;
import com.github.manager.RemoteAppiumManager;
import org.everit.json.schema.Schema;
import org.everit.json.schema.ValidationException;
import org.everit.json.schema.loader.SchemaLoader;
import org.json.JSONArray;
import org.json.JSONObject;
import org.json.JSONTokener;

public class CapabilitySchemaValidator {

    private static final Logger LOGGER = Logger.getLogger(RemoteAppiumManager.class.getName());

    public void validateCapabilitySchema(JSONObject capability) {
        try {
            isPlatformInEnv();
            InputStream inputStream = getClass().getResourceAsStream(getPlatform());
            JSONObject rawSchema = new JSONObject(new JSONTokener(inputStream));
            Schema schema = SchemaLoader.load(rawSchema);
            schema.validate(new JSONObject(capability.toString()));
            validateRemoteHosts();
        } catch (ValidationException e) {
            if (e.getCausingExceptions()
                .size() > 1) {
                e.getCausingExceptions()
                    .stream()
                    .map(ValidationException::getMessage)
                    .forEach(System.out::println);
            } else {
                LOGGER.info(e.getErrorMessage());
            }

            throw new ValidationException("Capability json provided is missing the above schema");
        }
    }

    private String getPlatform() {
        String platform = System.getenv("Platform");
        String schema = null;
        switch (platform.toLowerCase()) {
            case "both":
                schema = "/androidAndiOSSchema.json";
                break;
            case "android":
                schema = "/androidSchema.json";
                break;
            case "ios":
                schema = "/iOSSchema.json";
                break;
            default:
                System.out.println("Just for codacy!!");
                break;
        }
        return schema;
    }

    private void isPlatformInEnv() {
        if (System.getenv("Platform") == null) {
            String msg = "Please set Platform environment variable.";
            throw new IllegalArgumentException(msg);
        }
    }

    private void validateRemoteHosts() {
        CapabilityManager capabilityManager;
        try {
            capabilityManager = CapabilityManager.getInstance();
            if (capabilityManager.getCapabilities()
                .has("hostMachines")) {
                JSONArray hostMachines = capabilityManager.getHostMachineObject();
                for (Object hostMachine : hostMachines) {
                    JSONObject hostMachineJson = ((JSONObject) hostMachine);
                    boolean isCloud = hostMachineJson.has("isCloud");
                    if (isCloud) {
                        isCloud = hostMachineJson.getBoolean("isCloud");
                    }
                    String machineIP = (String) hostMachineJson.get("machineIP");
                    if (isCloud || InetAddress.getByName(machineIP)
                        .isReachable(5000)) {
                        LOGGER.info("ATD is Running on " + machineIP);
                    } else {
                        FigletHelper.figlet("Unable to connect to Remote Host " + machineIP);
                        throw new ConnectException();
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
