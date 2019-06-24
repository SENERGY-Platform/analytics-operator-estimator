import org.infai.seits.sepl.operators.Message;
import org.joda.time.DateTimeUtils;
import org.junit.Assert;
import org.junit.Test;

import java.util.List;

public class EstimatorTest {

    @Test
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
                Assert.assertEquals(m.getInput("DayPrediction").getValue(), dactual, 0.1);
                Assert.assertEquals(m.getInput("MonthPrediction").getValue(), mactual, 0.1);
                Assert.assertEquals(m.getInput("YearPrediction").getValue(), yactual, 0.1);
                System.out.println("Successfully finished a test instance.");
            }catch (NullPointerException|IndexOutOfBoundsException e){
                System.out.println("Skipped test instance because no expected values were provided.");
            }
        }
    }
}
