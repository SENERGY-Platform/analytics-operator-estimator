import org.infai.seits.sepl.operators.Message;
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


    public void run() throws Exception{
        Estimator est = new Estimator();
        List<Message> messages = TestMessageProvider.getTestMesssagesSet();
        for (Message m : messages) {
            est.config(m);
            DateTimeUtils.setCurrentMillisFixed(DateParser.parseDateMills(m.getInput("timestamp").getString()));
            est.run(m);
            double dactual = Double.parseDouble(m.getMessageString().split("DayPrediction\":")[1].split(",")[0]);
            double mactual = Double.parseDouble(m.getMessageString().split("MonthPrediction\":")[1].split(",")[0]);
            double yactual = Double.parseDouble(m.getMessageString().split("YearPrediction\":")[1].split("}")[0]);
            m.addInput("DayPrediction");
            m.addInput("MonthPrediction");
            m.addInput("YearPrediction");
            try {
                double expected = m.getInput("DayPrediction").getValue();
                Assert.assertEquals(expected, dactual, expected * 0.05);
                System.out.println("Successfully predicted for day.");
            }catch (NullPointerException|IndexOutOfBoundsException e){
                System.out.println("Skipped test for day because no expected values were provided.");
            }
            try{
                double expected = m.getInput("MonthPrediction").getValue();
                Assert.assertEquals(expected, mactual, expected * 0.05);
                System.out.println("Successfully predicted for month.");
            }catch (NullPointerException|IndexOutOfBoundsException e){
                System.out.println("Skipped test for month because no expected values were provided.");
            }
            try{
                double expected = m.getInput("YearPrediction").getValue();
                Assert.assertEquals(expected, yactual, expected * 0.05);
                System.out.println("Successfully predicted for year.");
            }catch (NullPointerException|IndexOutOfBoundsException e){
                System.out.println("Skipped test for year because no expected values were provided.");
            }
        }
    }

    @Test
    public void LinearRegression() throws Exception{
        environmentVariables.set("CONFIG", "{\"config\":{\"Algorithm\":\"LinearRegression\"}}");
        run();
    }
    @Test
    public void GaussianProcesses() throws Exception{
        environmentVariables.set("CONFIG", "{\"config\":{\"Algorithm\":\"GaussianProcesses\"}}");
        run();
    }
    @Test
    public void Logistic() throws Exception{
        environmentVariables.set("CONFIG", "{\"config\":{\"Algorithm\":\"Logistic\"}}");
        run();
    }
    @Test
    public void MultilayerPerceptron() throws Exception{
        environmentVariables.set("CONFIG", "{\"config\":{\"Algorithm\":\"MultilayerPerceptron\"}}");
        run();
    }
    @Test
    public void SGD() throws Exception{
        environmentVariables.set("CONFIG", "{\"config\":{\"Algorithm\":\"SGD\"}}");
        run();
    }
    @Test
    public void SimpleLinearRegression() throws Exception{
        environmentVariables.set("CONFIG", "{\"config\":{\"Algorithm\":\"SimpleLinearRegression\"}}");
        run();
    }
    @Test
    public void SimpleLogistic() throws Exception{
        environmentVariables.set("CONFIG", "{\"config\":{\"Algorithm\":\"SimpleLogistic\"}}");
        run();
    }
    @Test
    public void SMO() throws Exception{
        environmentVariables.set("CONFIG", "{\"config\":{\"Algorithm\":\"SMO\"}}");
        run();
    }
    @Test
    public void SMOreg() throws Exception{
        environmentVariables.set("CONFIG", "{\"config\":{\"Algorithm\":\"SMOreg\"}}");
        run();
    }
    @Test
    public void VotedPerceptron() throws Exception{
        environmentVariables.set("CONFIG", "{\"config\":{\"Algorithm\":\"VotedPerceptron\"}}");
        run();
    }
    @Test
    public void UnknownAlgorithm() throws Exception{
        environmentVariables.set("CONFIG", "{\"config\":{\"Algorithm\":\"UnknownAlgorithm\"}}");
        run();
    }
    @Test
    public void NoAlgorithm() throws Exception{
        environmentVariables.set("CONFIG", "{\"config\":{}}");
        run();
    }
}
