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
            est.run(m);
        }
    }

    public void run(double acurracy) throws Exception{
        run(new boolean[30][4], acurracy);
    }

    @Test
    public void Benchmark() throws Exception{
        environmentVariables.set("CONFIG", "{\"config\":{\"log_console\":\"true\",\"log_file\":\"true\"}}");
        run(0.05);
    }
}
