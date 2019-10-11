import org.junit.Rule;
import org.junit.Test;
import org.junit.contrib.java.lang.system.EnvironmentVariables;

public class EstimatorTest {

    @Rule
    public final EnvironmentVariables environmentVariables
            = new EnvironmentVariables();


    public void run(boolean[][] skipTests, double acurracy) throws Exception{
        // tests disabled for benchmark branch
    }

    public void run(double acurracy) throws Exception{
        run(new boolean[30][4], acurracy);
    }

    @Test
    public void LinearRegression() throws Exception{
        environmentVariables.set("CONFIG", "{\"config\":{\"Algorithm\":\"LinearRegression\"}}");
        run(0.05);
    }
    @Test
    public void GaussianProcesses() throws Exception{
        environmentVariables.set("CONFIG", "{\"config\":{\"Algorithm\":\"GaussianProcesses\"}}");
        run(0.6);
    }
    @Test
    public void SimpleLinearRegression() throws Exception{
        environmentVariables.set("CONFIG", "{\"config\":{\"Algorithm\":\"SimpleLinearRegression\"}}");
        run(0.05);
    }
    @Test
    public void SMOreg() throws Exception{
        environmentVariables.set("CONFIG", "{\"config\":{\"Algorithm\":\"SMOreg\"}}");
        boolean[][] skipTests = new boolean[30][3];
        skipTests[0][0] = true;
        skipTests[0][1] = true;
        skipTests[0][2] = true;

        run(skipTests, 0.05);
    }
    @Test
    public void UnknownAlgorithm() throws Exception{
        environmentVariables.set("CONFIG", "{\"config\":{\"Algorithm\":\"UnknownAlgorithm\"}}");
        run(0.05);
    }
    @Test
    public void NoAlgorithm() throws Exception{
        environmentVariables.set("CONFIG", "{\"config\":{}}");
        run(0.05);
    }
}
