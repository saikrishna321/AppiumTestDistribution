package com.appium.capabilities;

import com.appium.device.AtdEnvironment;
import com.appium.utils.FigletHelper;
import com.appium.utils.JsonParser;
import org.apache.log4j.Logger;
import org.everit.json.schema.Schema;
import org.everit.json.schema.ValidationException;
import org.everit.json.schema.loader.SchemaLoader;
import org.json.JSONArray;
import org.json.JSONObject;
import org.json.JSONTokener;

import java.io.InputStream;
import java.net.ConnectException;
import java.net.InetAddress;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import static com.appium.utils.ConfigFileManager.CAPS;
import static com.appium.utils.OverriddenVariable.getOverriddenStringValue;

public class Capabilities {
    private static final Logger LOGGER = Logger.getLogger(Capabilities.class.getName());

    private static Capabilities instance;
    private JSONObject capabilities;
    private final AtdEnvironment atdEnvironment;

    private Capabilities() {
        atdEnvironment = new AtdEnvironment();
        String capabilitiesFilePath = getCapabilityLocation();
        JsonParser jsonParser = new JsonParser(capabilitiesFilePath);
        createInstance(jsonParser.getObjectFromJSON());
    }

    public Capabilities(String capabilitiesJson, AtdEnvironment atdEnvironment) {
        this.atdEnvironment = atdEnvironment;
        createInstance(new JsonParser().getObjectFromJSONString(capabilitiesJson));
    }

    private void createInstance(JSONObject capabilitiesJsonObject) {
        StringBuilder varParsing = new StringBuilder(200);
        varParsing.append("atd").append("_");
        capabilities = loadAndOverrideFromEnvVars(
                capabilitiesJsonObject,
                new JSONObject(),
                getAllATDOverrideEnvVars(),
                varParsing);
        validateCapabilitySchema();
    }

    public static Capabilities getInstance() {
        if (instance == null) {
            instance = new Capabilities();
        }
        return instance;
    }

    private Map<String, Object> getAllATDOverrideEnvVars() {
        Map<String, Object> atdOverrideEnv = new HashMap<>();
        System.getenv().forEach((key, value) -> {
            if (key.startsWith("atd")) {
                atdOverrideEnv.put(key, value);
            }
        });
        Properties properties = System.getProperties();
        properties.keySet().forEach(key -> {
            if (key.toString().startsWith("atd")) {
                atdOverrideEnv.put(key.toString(), properties.getProperty(key.toString()));
            }
        });
        return atdOverrideEnv;
    }

    private JSONObject loadAndOverrideFromEnvVars(JSONObject originalObject,
                                                  JSONObject objectToUpdate,
                                                  Map<String, Object> allATDOverrideEnvVars,
                                                  StringBuilder currentPath) {
        Set<String> keys = originalObject.keySet();
        keys.forEach(keyStr -> {
            Object keyvalue = originalObject.get(keyStr);
            if (keyvalue instanceof JSONObject) {
                processJSONObject(objectToUpdate,
                        allATDOverrideEnvVars,
                        currentPath,
                        keyStr,
                        (JSONObject) keyvalue);
            } else if (keyvalue instanceof JSONArray) {
                processJSONArray(objectToUpdate,
                        allATDOverrideEnvVars,
                        currentPath,
                        keyStr,
                        (JSONArray) keyvalue);
            } else {
                processJSONObject(objectToUpdate,
                        currentPath,
                        keyStr,
                        keyvalue);
            }
        });
        return objectToUpdate;
    }

    private void processJSONObject(JSONObject objectToUpdate,
                                   StringBuilder currentPath,
                                   String keyStr,
                                   Object keyvalue) {
        currentPath.append(keyStr);
        String getFromEnv = getOverriddenStringValue(currentPath.toString());
        Object updatedValue = (null == getFromEnv) ? keyvalue : getFromEnv;
        objectToUpdate.put(keyStr, updatedValue);
        currentPath.delete(currentPath.lastIndexOf("_") + 1, currentPath.length());
    }

    private void processJSONArray(JSONObject objectToUpdate,
                                  Map<String, Object> allATDOverrideEnvVars,
                                  StringBuilder currentPath,
                                  String keyStr,
                                  JSONArray keyvalue) {
        JSONArray jsonArray = new JSONArray();
        objectToUpdate.put(keyStr, jsonArray);
        currentPath.append(keyStr).append("_");
        for (int arrIndex = 0; arrIndex < keyvalue.length(); arrIndex++) {
            processJSONArrayItem(allATDOverrideEnvVars,
                    currentPath,
                    jsonArray,
                    keyvalue,
                    arrIndex);
        }
        currentPath.delete(currentPath.lastIndexOf(keyStr), currentPath.length());
    }

    private void processJSONArrayItem(Map<String, Object> allATDOverrideEnvVars,
                                      StringBuilder currentPath,
                                      JSONArray jsonArray,
                                      JSONArray arrayValues,
                                      int arrIndex) {
        JSONObject arrayItem = (JSONObject) arrayValues.get(arrIndex);
        JSONObject jsonObject = new JSONObject();
        jsonArray.put(jsonObject);
        currentPath.append(arrIndex).append("_");
        loadAndOverrideFromEnvVars((JSONObject) arrayItem,
                jsonObject,
                allATDOverrideEnvVars,
                currentPath);
        currentPath.delete(currentPath.lastIndexOf(String.valueOf(arrIndex)), currentPath.length());
    }

    private void processJSONObject(JSONObject objectToUpdate,
                                   Map<String, Object> allATDOverrideEnvVars,
                                   StringBuilder currentPath,
                                   String keyStr,
                                   JSONObject keyvalue) {
        JSONObject jsonObject = new JSONObject();
        objectToUpdate.put(keyStr, jsonObject);
        currentPath.append(keyStr).append("_");
        loadAndOverrideFromEnvVars(keyvalue, jsonObject, allATDOverrideEnvVars, currentPath);
        currentPath.delete(currentPath.lastIndexOf(keyStr), currentPath.length());
    }

    private String getCapabilityLocation() {
        String path = System.getProperty("user.dir") + "/caps/"
                + "capabilities.json";
        String caps = CAPS.get();
        if (caps != null) {
            Path userDefinedCapsPath = FileSystems.getDefault().getPath(caps);
            if (!userDefinedCapsPath.getParent().isAbsolute()) {
                path = userDefinedCapsPath.normalize()
                        .toAbsolutePath().toString();
            } else {
                path = userDefinedCapsPath.toString();
            }
        }
        return path;
    }


    public JSONObject getCapabilityObjectFromKey(String key) {
        boolean hasKey = capabilities.has(key);
        if (hasKey) {
            return (JSONObject) capabilities.get(key);
        }
        return null;
    }

    public JSONArray getCapabilitiesArrayFromKey(String key) {
        return capabilities.getJSONArray(key);
    }

    public Boolean getCapabilityBoolean(String key) {
        if (capabilities.has(key)) {
            return (Boolean) capabilities.get(key);
        }
        return false;
    }

    public HashMap<String, String> getMongoDbHostAndPort() {
        HashMap<String, String> params = new HashMap<>();
        if (capabilities.has("ATDServiceHost")
                && capabilities.has("ATDServicePort")) {
            params.put("atdHost", (String) capabilities.get("ATDServiceHost"));
            params.put("atdPort", (String) capabilities.get("ATDServicePort"));
        }
        return params;
    }

    public JSONArray getHostMachineObject() {
        return getCapabilitiesArrayFromKey("hostMachines");
    }

    public Boolean shouldExcludeLocalDevices() {
        return getCapabilityBoolean("excludeLocalDevices");
    }

    public boolean isSimulatorAppPresentInCapsJson() {
        boolean hasApp = getCapabilityObjectFromKey("iOS").has("app");
        return hasApp && getCapabilityObjectFromKey("iOS").getJSONObject("app").has("simulator");
    }

    public boolean isApp() {
        return getCapabilityObjectFromKey("iOS").has("app");
    }

    public boolean isWindowsApp() {
        return getCapabilityObjectFromKey("windows").has("app");
    }

    public boolean isRealDeviceAppPresentInCapsJson() {
        boolean hasApp = getCapabilityObjectFromKey("iOS").has("app");
        return hasApp && getCapabilityObjectFromKey("iOS").getJSONObject("app").has("device");
    }

    public String getAppiumServerPath(String host) throws Exception {
        try {
            return appiumServerProp(host, "appiumServerPath");
        } catch (Exception e) {
            return null;
        }
    }

    public boolean isCloud(String host) {
        try {
            return appiumServerProp(host, "isCloud");
        } catch (Exception e) {
            return false;
        }
    }

    public String getCloudName(String host) {
        try {
            return appiumServerProp(host, "cloudName");
        } catch (Exception e) {
            return null;
        }
    }

    private <T> T appiumServerProp(String host, String arg) throws Exception {
        JSONArray hostMachineObject = Capabilities.getInstance().getHostMachineObject();
        List<Object> hostIP = hostMachineObject.toList();
        Object machineIP = hostIP.stream().filter(object -> ((Map) object).get("machineIP")
                .toString().equalsIgnoreCase(host)
                && ((Map) object).get(arg) != null)
                .findFirst().orElse(null);
        return (T) ((Map) machineIP).get(arg);
    }

    public String getAppiumServerPort(String host) throws Exception {
        return appiumServerProp(host, "appiumPort");
    }

    public String getRemoteAppiumManangerPort(String host) {
        try {
            return appiumServerProp(host, "remoteAppiumManagerPort");
        } catch (Exception e) {
            return "4567";
        }
    }

    public JSONObject getCapabilities() {
        return capabilities;
    }

    public boolean hasHostMachines() {
        return getCapabilities().has("hostMachines");
    }

    public void validateCapabilitySchema() {
        try {
            isPlatformInEnv();
            InputStream inputStream = getClass().getResourceAsStream(getPlatform());
            JSONObject rawSchema = new JSONObject(new JSONTokener(inputStream));
            Schema schema = SchemaLoader.load(rawSchema);
            schema.validate(new JSONObject(getCapabilities().toString()));
            validateRemoteHosts();
        } catch (ValidationException e) {
            if (e.getCausingExceptions().size() > 1) {
                e.getCausingExceptions().stream()
                        .map(ValidationException::getMessage)
                        .forEach(System.out::println);
            } else {
                LOGGER.info(e.getErrorMessage());
            }

            throw new ValidationException("Capability json provided is missing the above schema");
        }
    }

    private String getPlatform() {
        String platform = atdEnvironment.get("Platform");
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
            case "windows":
                schema = "/windowsSchema.json";
                break;
            default:
                LOGGER.info("Just for codacy!!");
                break;

        }
        return schema;
    }

    private void isPlatformInEnv() {
        if (atdEnvironment.get("Platform") == null) {
            throw new IllegalArgumentException("Please execute with Platform environment"
                    + ":: Platform=android/ios/both/windows mvn clean -Dtest=Runner test");
        }
    }

    private void validateRemoteHosts() {
        try {
            if (!hasHostMachines()) {
                return;
            }
            JSONArray hostMachines = getHostMachineObject();
            for (Object hostMachine : hostMachines) {
                JSONObject hostMachineJson = ((JSONObject) hostMachine);
                boolean isCloud = hostMachineJson.has("isCloud");
                if (isCloud) {
                    isCloud = hostMachineJson.getBoolean("isCloud");
                }
                String machineIP = (String) hostMachineJson.get("machineIP");
                if (isCloud
                        || InetAddress.getByName(machineIP).isReachable(5000)) {
                    LOGGER.info("ATD is Running on " + machineIP);
                } else {
                    FigletHelper.figlet("Unable to connect to Remote Host " + machineIP);
                    throw new ConnectException();
                }
            }
        } catch (Exception e) {
            throw new RuntimeException("Provide hostMachine in Caps.json for execution");
        }
    }
}