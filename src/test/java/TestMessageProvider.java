import org.infai.seits.sepl.operators.Builder;
import org.infai.seits.sepl.operators.Message;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class TestMessageProvider {

    public static List<Message> getTestMesssagesSet() throws IOException {
        BufferedReader br = new BufferedReader(new FileReader("src/test/resources/smartmeter-780000-1110004.json"));
        Builder builder = new Builder("1", "1");
        List<Message> messageSet = new ArrayList<>();
        JSONObject config = getConfig();
        String line;
        Message m;
        JSONObject jsonObjectRead, jsonObject;
        while ((line = br.readLine()) != null) {
            jsonObjectRead = new JSONObject(line);
            jsonObject = new JSONObject().put("device_id", "1").put("value", new JSONObject().put("reading", jsonObjectRead));
            m = new Message(builder.formatMessage(jsonObject.toString()));
            m.setConfig(config.toString());
            messageSet.add(m);
        }
        return messageSet;
    }

    private static JSONObject getConfig() {
        JSONObject config = new JSONObject().put("inputTopics",new JSONArray().put(new JSONObject().put("Name", "test")
                .put("FilterType", "DeviceId")
                .put("FilterValue", "1")
                .put("Mappings", new JSONArray()
                        .put(new JSONObject().put("Source", "value.reading.TIMESTAMP_UTC").put("Dest", "timestamp"))
                        .put(new JSONObject().put("Source", "value.reading.CONSUMPTION").put("Dest", "value"))
                        .put(new JSONObject().put("Source", "value.reading.METER_ID").put("Dest", "device_id"))
                )));
        return config;
    }
}
