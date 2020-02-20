import org.apache.commons.math3.stat.regression.SimpleRegression;

public class ApacheSimpleRegression implements EstimatorInterface {

    protected SimpleRegression simpleRegression;

    public ApacheSimpleRegression(){
        simpleRegression = new SimpleRegression();
    }


    @Override
    public void addData(long timestamp, double value) {
        simpleRegression.addData(timestamp, value);
    }

    @Override
    public double predict(long timestamp) {
        return simpleRegression.predict(timestamp);
    }
}
