import org.junit.Assert;
import org.junit.Test;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;

public class SaveLoadTest {

    @Test
    public void apacheSimpleRegression() throws Exception {
        EstimatorInterface e = new ApacheSimpleRegression();
        e.addData(0, 0);
        e.addData(1, 1);
        e.addData(2, 2);
        e.addData(3, 3);
        new File("data/test/").mkdirs();
        e.save(new FileOutputStream("data/test/" + e.getClass().getName() + ".bin"));

        EstimatorInterface e2 = new ApacheSimpleRegression();
        e2.loadSaved(new FileInputStream("data/test/" + e.getClass().getName() + ".bin"));

        boolean eq = e.equals(e2);
        Assert.assertTrue(eq);
    }
}
