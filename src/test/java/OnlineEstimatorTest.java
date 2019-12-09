import org.infai.seits.sepl.operators.Message;
import org.joda.time.DateTimeUtils;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.contrib.java.lang.system.EnvironmentVariables;

import java.util.List;

public class OnlineEstimatorTest {

    @Rule
    public final EnvironmentVariables environmentVariables
            = new EnvironmentVariables();


    public void run(boolean[][] skipTests, double acurracy) throws Exception{
        OnlineEstimator est = new OnlineEstimator();
        List<Message> messages = TestMessageProvider.getTestMesssagesSet();
        for (int i = 0; i < messages.size(); i++) {
            Message m = messages.get(i);
            est.config(m);
            DateTimeUtils.setCurrentMillisFixed(DateParser.parseDateMills(m.getInput("timestamp").getString()));
            est.run(m);

            if(!skipTests[i][0]) {
                try {
                    m.addInput("DayPrediction");
                    double actual = Double.parseDouble(m.getMessageString().split("DayPrediction\":")[1].split(",")[0]);
                    double expected = m.getInput("DayPrediction").getValue();
                    if(expected != 0)
                        Assert.assertEquals(expected, actual, expected * acurracy);
                    else
                        Assert.assertEquals(expected, actual, 10 * acurracy);
                    System.out.println("Successfully predicted for day.");
                }catch (NullPointerException|IndexOutOfBoundsException e){
                    System.out.println("Skipped test for day because no expected values were provided.");
                } catch(NumberFormatException e) {
                    Assert.fail("Failed test: Operator did not provide prediction");
                }
            } else {
                System.out.println("Skipped test for day as requested");
            }
            if(!skipTests[i][1]) {
                try {
                    m.addInput("MonthPrediction");
                    double actual = Double.parseDouble(m.getMessageString().split("MonthPrediction\":")[1].split(",")[0]);
                    double expected = m.getInput("MonthPrediction").getValue();
                    if(expected != 0)
                        Assert.assertEquals(expected, actual, expected * acurracy);
                    else
                        Assert.assertEquals(expected, actual, 10 * acurracy);
                    System.out.println("Successfully predicted for month.");
                } catch (NullPointerException | IndexOutOfBoundsException e) {
                    System.out.println("Skipped test for month because no expected values were provided.");
                } catch(NumberFormatException e) {
                    Assert.fail("Failed test: Operator did not provide prediction");
                }
            } else {
                System.out.println("Skipped test for month as requested");
            }
            if(!skipTests[i][2]) {
                try {
                    m.addInput("YearPrediction");
                    double actual = Double.parseDouble(m.getMessageString().split("YearPrediction\":")[1].split("}")[0]);
                    double expected = m.getInput("YearPrediction").getValue();
                    if(expected != 0)
                        Assert.assertEquals(expected, actual, expected * acurracy);
                    else
                        Assert.assertEquals(expected, actual, 10 * acurracy);
                    System.out.println("Successfully predicted for year.");
                } catch (NullPointerException | IndexOutOfBoundsException e) {
                    System.out.println("Skipped test for year because no expected values were provided.");
                } catch(NumberFormatException e) {
                    Assert.fail("Failed test: Operator did not provide prediction");
                }
            }else {
                System.out.println("Skipped test for year as requested");
            }
        }
    }

    public void run(double acurracy) throws Exception{
        run(new boolean[90][4], acurracy);
    }

    @Test
    public void AdaGrad() throws Exception{
        environmentVariables.set("CONFIG", "{\"config\":{\"Algorithm\":\"online-AdaGrad\"}}");
        //run(0.5);TEST DISABLED
    }
    @Test
    public void SGD() throws Exception{
        environmentVariables.set("CONFIG", "{\"config\":{\"Algorithm\":\"online-SGD\"}}");
        //run(0.5);TEST DISABLED
    }
    @Test
    public void AdaptiveRandomForestRegressor() throws Exception{
        environmentVariables.set("CONFIG", "{\"config\":{\"Algorithm\":\"online-AdaptiveRandomForestRegressor\"}}");
        //run(0.5);TEST DISABLED
    }
    @Test
    public void UnknownAlgorithm() throws Exception{
        environmentVariables.set("CONFIG", "{\"config\":{\"Algorithm\":\"online-UnknownAlgorithm\"}}");
        //run(0.5);TEST DISABLED
    }
    @Test
    public void NoAlgorithm() throws Exception{
        environmentVariables.set("CONFIG", "{\"config\":{}}");
        //run(0.5);TEST DISABLED
    }
}
