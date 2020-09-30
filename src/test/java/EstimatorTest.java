import org.infai.ses.senergy.operators.Message;
import org.infai.ses.senergy.operators.OperatorInterface;
import org.joda.time.DateTimeUtils;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.contrib.java.lang.system.EnvironmentVariables;

import java.util.List;

public class EstimatorTest {

    @Rule
    public final EnvironmentVariables environmentVariables
            = new EnvironmentVariables();


    public void run(boolean[][] skipTests, double accuracy) throws Exception {
        OperatorInterface est = EstimatorFactory.createNewInstance();
        List<Message> messages = TestMessageProvider.getTestMesssagesSet("src/test/resources/sample-data-small.json");
        for (int i = 0; i < messages.size(); i++) {
            Message m = messages.get(i);
            est.configMessage(m);
            DateTimeUtils.setCurrentMillisFixed(DateParser.parseDateMills(m.getInput("timestamp").getString()));
            est.run(m);

            if (!skipTests[i][0]) {
                try {
                    m.addInput("DayPrediction");
                    double actual = Double.parseDouble(m.getMessageString().split("DayPrediction\":")[1].split(",")[0]);
                    double expected = m.getInput("DayPrediction").getValue();
                    Assert.assertEquals(expected, actual, expected * accuracy);
                    System.out.println("Successfully predicted for day.");
                } catch (NullPointerException | IndexOutOfBoundsException e) {
                    System.out.println("Skipped test for day because no expected values were provided.");
                } catch (NumberFormatException e) {
                    Assert.fail("Failed test: Operator did not provide prediction");
                }
            } else {
                System.out.println("Skipped test for day as requested");
            }
            if (!skipTests[i][1]) {
                try {
                    m.addInput("MonthPrediction");
                    double actual = Double.parseDouble(m.getMessageString().split("MonthPrediction\":")[1].split(",")[0]);
                    double expected = m.getInput("MonthPrediction").getValue();
                    Assert.assertEquals(expected, actual, expected * accuracy);
                    System.out.println("Successfully predicted for month.");
                } catch (NullPointerException | IndexOutOfBoundsException e) {
                    System.out.println("Skipped test for month because no expected values were provided.");
                } catch (NumberFormatException e) {
                    Assert.fail("Failed test: Operator did not provide prediction");
                }
            } else {
                System.out.println("Skipped test for month as requested");
            }
            if (!skipTests[i][2]) {
                try {
                    m.addInput("YearPrediction");
                    double actual = Double.parseDouble(m.getMessageString().split("YearPrediction\":")[1].split(",")[0]);
                    double expected = m.getInput("YearPrediction").getValue();
                    Assert.assertEquals(expected, actual, expected * accuracy);
                    System.out.println("Successfully predicted for year.");
                } catch (NullPointerException | IndexOutOfBoundsException e) {
                    System.out.println("Skipped test for year because no expected values were provided.");
                } catch (NumberFormatException e) {
                    Assert.fail("Failed test: Operator did not provide prediction");
                }
            } else {
                System.out.println("Skipped test for year as requested");
            }
        }
    }

    public void run(double acurracy) throws Exception {
        run(new boolean[30][4], acurracy);
    }

    @Test
    public void UnknownAlgorithm() throws Exception {
        environmentVariables.set("CONFIG", "{\"config\":{\"Algorithm\":\"UnknownAlgorithm\"}}");
        run(0.05);
    }

    @Test
    public void NoAlgorithm() throws Exception {
        environmentVariables.set("CONFIG", "{\"config\":{}}");
        run(0.05);
    }

    @Test
    public void ApacheSimple() throws Exception {
        environmentVariables.set("CONFIG", "{\"config\":{\"Algorithm\":\"apache-simple\"}}");
        run(0.05);
    }

    @Test
    public void IgnoreOldValues() throws Exception {
        DateTimeUtils.setCurrentMillisFixed(1594362930000L);
        OperatorInterface est = EstimatorFactory.createNewInstance();
        List<Message> messages = TestMessageProvider.getTestMesssagesSet("src/test/resources/sample-data-no-expected.json");
        for (int i = 0; i < messages.size(); i++) {
            Message m = messages.get(i);
            est.configMessage(m);
            est.run(m);
            try {
                double actual = Double.parseDouble(m.getMessageString().split("DayPrediction\":")[1].split(",")[0]);
                Assert.fail("Found value where none was expected: " + actual);
            } catch (NullPointerException | IndexOutOfBoundsException ignored) {
            } catch (NumberFormatException e) {
                Assert.fail("Failed test: Operator did not provide prediction");
            }

            try {
                double actual = Double.parseDouble(m.getMessageString().split("MonthPrediction\":")[1].split(",")[0]);
                Assert.fail("Found value where none was expected: " + actual);
            } catch (NullPointerException | IndexOutOfBoundsException ignored) {
            } catch (NumberFormatException e) {
                Assert.fail("Failed test: Operator did not provide prediction");
            }

            try {
                double actual = Double.parseDouble(m.getMessageString().split("YearPrediction\":")[1].split(",")[0]);
                Assert.fail("Found value where none was expected: " + actual);
            } catch (NullPointerException | IndexOutOfBoundsException ignored) {
            } catch (NumberFormatException e) {
                Assert.fail("Failed test: Operator did not provide prediction");
            }

        }
    }
}
