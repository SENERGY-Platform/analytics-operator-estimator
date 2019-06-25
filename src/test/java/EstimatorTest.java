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
}
