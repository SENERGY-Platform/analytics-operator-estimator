import org.infai.ses.senergy.models.DeviceMessageModel;
import org.infai.ses.senergy.models.MessageModel;
import org.infai.ses.senergy.operators.BaseOperator;
import org.infai.ses.senergy.operators.Config;
import org.infai.ses.senergy.operators.Helper;
import org.infai.ses.senergy.operators.Message;
import org.infai.ses.senergy.testing.utils.JSONHelper;
import org.infai.ses.senergy.utils.ConfigProvider;
import org.joda.time.DateTimeUtils;
import org.json.JSONObject;
import org.json.simple.JSONArray;
import org.junit.Assert;
import org.junit.Test;

public class EstimatorTest {


    public Estimator run(Config config, boolean[][] skipTests, double accuracy) {
        JSONArray messages = new JSONHelper().parseFile("sample-data-small.json");

        String topicName = config.getInputTopicsConfigs().get(0).getName();
        ConfigProvider.setConfig(config);
        Message message = new Message();
        MessageModel model = new MessageModel();
        Estimator est = EstimatorFactory.createNewInstanceWithConfig(config);
        est.configMessage(message);

        for (int i = 0; i < messages.size(); i++) {
            Object m = messages.get(i);
            DeviceMessageModel deviceMessageModel = JSONHelper.getObjectFromJSONString(m.toString(), DeviceMessageModel.class);
            assert deviceMessageModel != null;
            model.putMessage(topicName, Helper.deviceToInputMessageModel(deviceMessageModel, topicName));
            message.setMessage(model);

            try {
                DateTimeUtils.setCurrentMillisFixed(DateParser.parseDateMills(message.getInput("timestamp").getString()));
            } catch(NullPointerException e) {
                System.out.println("Skipped test for message without timestamp");
            }

            est.run(message);

            if (!skipTests[i][0]) {
                try {
                    double actual = (double) message.getMessage().getOutputMessage().getAnalytics().get("DayPrediction");
                    double expected = (double) deviceMessageModel.getValue().get("DayPrediction");
                    Assert.assertEquals(expected, actual, expected * accuracy);
                    System.out.println("Successfully predicted for day. Expected " + expected + ", got " + actual);
                } catch (NullPointerException ignored) {
                } catch (NumberFormatException e) {
                    Assert.fail("Failed test: Operator did not provide prediction");
                }
            } else {
                System.out.println("Skipped test for day as requested");
            }
            if (!skipTests[i][1]) {
                try {
                    double actual = (double) message.getMessage().getOutputMessage().getAnalytics().get("MonthPrediction");
                    double expected = (double) deviceMessageModel.getValue().get("MonthPrediction");
                    Assert.assertEquals(expected, actual, expected * accuracy);
                    System.out.println("Successfully predicted for month. Expected " + expected + ", got " + actual);
                } catch (NullPointerException ignored) {
                } catch (NumberFormatException e) {
                    Assert.fail("Failed test: Operator did not provide prediction");
                }
            } else {
                System.out.println("Skipped test for month as requested");
            }
            if (!skipTests[i][2]) {
                try {
                    double actual = (double) message.getMessage().getOutputMessage().getAnalytics().get("YearPrediction");
                    double expected = (double) deviceMessageModel.getValue().get("YearPrediction");
                    Assert.assertEquals(expected, actual, expected * accuracy);
                    System.out.println("Successfully predicted for year. Expected " + expected + ", got " + actual);
                } catch (NullPointerException ignored) {
                } catch (NumberFormatException e) {
                    Assert.fail("Failed test: Operator did not provide prediction");
                }
            } else {
                System.out.println("Skipped test for year as requested");
            }
        }
        return est;
    }

    public Estimator run(Config config, double acurracy) {
        return run(config, new boolean[30][4], acurracy);
    }

    @Test
    public void UnknownAlgorithm() throws Exception {
        JSONObject jsonConfig = new JSONObject(getConfig());
        jsonConfig.put("config", new JSONObject("{\"Algorithm\":\"UnknownAlgorithm\"}"));
        run(new Config(jsonConfig.toString()), 0.05);
    }

    @Test
    public void NoAlgorithm() {
        run(new Config(getConfig()), 0.05);
    }

    @Test
    public void ApacheSimple() throws Exception {
        JSONObject jsonConfig = new JSONObject(getConfig());
        jsonConfig.put("config", new JSONObject("{\"Algorithm\":\"apache-simple\"}"));

        Estimator est = run(new Config(jsonConfig.toString()), 0.05);
        Assert.assertEquals(est.getEstimator().getClass(), ApacheSimpleRegression.class);
    }

    @Test
    public void MoaFIMTDD() throws Exception {
        JSONObject jsonConfig = new JSONObject(getConfig());
        jsonConfig.put("config", new JSONObject("{\"Algorithm\":\"moa-fimtdd\"}"));

        Estimator est = run(new Config(jsonConfig.toString()), 1);
        Assert.assertEquals(est.getEstimator().getClass(), MoaFIMTDDRegression.class);
    }

    @Test
    public void MoaARF() throws Exception {
        JSONObject jsonConfig = new JSONObject(getConfig());
        jsonConfig.put("config", new JSONObject("{\"Algorithm\":\"moa-arf\"}"));

        Estimator est = run(new Config(jsonConfig.toString()), 1);
        Assert.assertEquals(est.getEstimator().getClass(), MoaARF.class);
    }


    @Test
    public void IgnoreOldValues() {
        DateTimeUtils.setCurrentMillisFixed(1594362930000L);
        Config config = new Config(getConfig());
        BaseOperator est = EstimatorFactory.createNewInstanceWithConfig(config);
        String topicName = config.getInputTopicsConfigs().get(0).getName();
        JSONArray messages = new JSONHelper().parseFile("sample-data-no-expected.json");
        Message m = new Message();
        est.configMessage(m);
        MessageModel model = new MessageModel();
        for (Object mo : messages) {
            DeviceMessageModel deviceMessageModel = JSONHelper.getObjectFromJSONString(mo.toString(), DeviceMessageModel.class);
            assert deviceMessageModel != null;
            model.putMessage(topicName, Helper.deviceToInputMessageModel(deviceMessageModel, topicName));
            m.setMessage(model);
            est.run(m);
            try {
                double actual = (double) m.getMessage().getOutputMessage().getAnalytics().get("DayPrediction");

                Assert.fail("Found value where none was expected: " + actual);
            } catch (NullPointerException | IndexOutOfBoundsException ignored) {
            } catch (NumberFormatException e) {
                Assert.fail("Failed test: Operator did not provide prediction");
            }

            try {
                double actual = (double) m.getMessage().getOutputMessage().getAnalytics().get("MonthPrediction");
                Assert.fail("Found value where none was expected: " + actual);
            } catch (NullPointerException | IndexOutOfBoundsException ignored) {
            } catch (NumberFormatException e) {
                Assert.fail("Failed test: Operator did not provide prediction");
            }

            try {
                double actual = (double) m.getMessage().getOutputMessage().getAnalytics().get("YearPrediction");
                Assert.fail("Found value where none was expected: " + actual);
            } catch (NullPointerException | IndexOutOfBoundsException ignored) {
            } catch (NumberFormatException e) {
                Assert.fail("Failed test: Operator did not provide prediction");
            }

        }
    }

    private static String getConfig() {
        return "{\n" +
                "  \"inputTopics\": [\n" +
                "    {\n" +
                "      \"name\": \"iot_bc59400c-405c-4c84-9862-a791daa82b60\",\n" +
                "      \"filterType\": \"DeviceId\",\n" +
                "      \"filterValue\": \"0\",\n" +
                "      \"mappings\": [\n" +
                "        {\n" +
                "          \"dest\": \"value\",\n" +
                "          \"source\": \"value.reading.value\"\n" +
                "        },\n" +
                "        {\n" +
                "          \"dest\": \"timestamp\",\n" +
                "          \"source\": \"value.reading.timestamp\"\n" +
                "        }\n" +
                "      ]\n" +
                "    }\n" +
                "  ]\n" +
                "}";
    }
}
