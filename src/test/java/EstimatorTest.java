import org.infai.seits.sepl.operators.Message;
import org.junit.Rule;
import org.junit.Test;
import org.junit.contrib.java.lang.system.EnvironmentVariables;

import java.util.List;

public class EstimatorTest {

    @Rule
    public final EnvironmentVariables environmentVariables
            = new EnvironmentVariables();


    public void run(boolean[][] skipTests, double acurracy) throws Exception{
        Estimator est = new Estimator();
        List<Message> messages = TestMessageProvider.getTestMesssagesSet();
        long startMill, endMill;
        for (int i = 0; i < messages.size(); i++) {
            Message m = messages.get(i);
            est.config(m);
            startMill = System.currentTimeMillis();
            est.run(m);
            endMill = System.currentTimeMillis();
            System.err.println("Message "+ i +" took " + (endMill - startMill) + " millis");
            System.out.println("\t" + m.getMessageString());
        }
    }

    public void run(double acurracy) throws Exception{
        run(new boolean[30][4], acurracy);
    }

    @Test
    public void Benchmark() throws Exception{
        run(0.05);
    }
}
