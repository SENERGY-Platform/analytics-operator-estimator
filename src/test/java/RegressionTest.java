import org.junit.Test;

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Random;

public class RegressionTest {

    public void testRandom(EstimatorInterface est) {
        test(est, getRandomSamples(745));
    }

    public double[] test(EstimatorInterface est, double[] samples) {
        double[] acc = new double[samples.length];
        Instant instant = Instant.ofEpochMilli(System.currentTimeMillis());
        ZonedDateTime zdt = ZonedDateTime.ofInstant(instant, ZoneId.of("Europe/Berlin"));

        ZonedDateTime movingTimestamp = zdt.withDayOfYear(1).withHour(0).withMinute(0).withSecond(0).withNano(0);
        double targetValue = 0;
        for (double sample : samples) {
            targetValue += sample;
            movingTimestamp = movingTimestamp.plusHours(1);
        }

        long targetTimestamp = DateParser.parseDateMills(DateParser.parseDate(movingTimestamp.toString()));
        movingTimestamp = zdt.withDayOfYear(1).withHour(0).withMinute(0).withSecond(0).withNano(0);


        double val = 0;
        for (int i = 0; i < samples.length; i++) {
            val += samples[i];
            long timestamp = DateParser.parseDateMills(DateParser.parseDate(movingTimestamp.toString()));
            est.addData(timestamp, val);

            double pred = est.predict(targetTimestamp);
            acc[i] = pred / targetValue;
            movingTimestamp = movingTimestamp.plusHours(1);
        }
        return acc;
    }

    @Test
    public void apacheSimpleRegression() {
        testRandom(new ApacheSimpleRegression());
    }

    @Test
    public void moaFIMTDD() {
        testRandom(new MoaFIMTDDRegression());
    }

    @Test
    public void moaARF() {
        testRandom(new MoaARF());
    }

    @Test
    public void compare() {
        double[] samples = getRandomSamples(1000);
        double[] accFIMTDD = test(new MoaFIMTDDRegression(), samples);
        double[] accApache = test(new ApacheSimpleRegression(), samples);
        double[] accARF = test(new MoaARF(), samples);

        for (int i = 0; i < samples.length; i++) {
            System.out.println("****** i=" + i + " ******");
            System.out.println("FIMTDD:\t" + accFIMTDD[i]);
            System.out.println("ARF:\t" + accARF[i]);
            System.out.println("APACHE:\t" + accApache[i]);
        }
    }


    private double[] getRandomSamples(int count) {
        double[] samples = new double[count];
        Random r = new Random();
        for (int i = 0; i < count; i++) {
            samples[i] = r.nextDouble();
        }
        return samples;
    }
}
