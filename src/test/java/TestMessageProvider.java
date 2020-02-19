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
        BufferedReader br = new BufferedReader(new FileReader("src/test/resources/sample-data-small.json"));
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
                        .put(new JSONObject().put("Source", "value.reading.timestamp").put("Dest", "timestamp"))
                        .put(new JSONObject().put("Source", "value.reading.value").put("Dest", "value"))
                        .put(new JSONObject().put("Source", "value.reading.DayPrediction").put("Dest", "DayPrediction"))
                        .put(new JSONObject().put("Source", "value.reading.MonthPrediction").put("Dest", "MonthPrediction"))
                        .put(new JSONObject().put("Source", "value.reading.YearPrediction").put("Dest", "YearPrediction"))
                )));
        return config;
    }

    public static Message getMessageWithValues(String timestamp, double value) {
        JSONObject config = getConfig();
        JSONObject jsonObjectRead = new JSONObject("{\"value\": " + value + ", \"timestamp\": \"" + timestamp + "\"}");
        JSONObject jsonObject = new JSONObject().put("device_id", "1").put("value", new JSONObject().put("reading", jsonObjectRead));
        Builder builder = new Builder("1", "1");
        Message m = new Message(builder.formatMessage(jsonObject.toString()));
        m.setConfig(config.toString());
        return m;
    }
}
