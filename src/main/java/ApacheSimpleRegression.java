import org.apache.commons.lang3.SerializationUtils;
import org.apache.commons.math3.stat.regression.SimpleRegression;

import java.io.*;

public class ApacheSimpleRegression implements EstimatorInterface {

    protected SimpleRegression simpleRegression;

    public ApacheSimpleRegression() {
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

    @Override
    public void loadSaved(FileInputStream f) throws IOException, ClassNotFoundException {
        ObjectInputStream ois = new ObjectInputStream(f);
        simpleRegression = (SimpleRegression) ois.readObject();
        ois.close();
    }

    @Override
    public void save(FileOutputStream f) throws IOException {
        ObjectOutputStream oos = new ObjectOutputStream(f);
        oos.writeObject(simpleRegression);
        oos.close();
    }

    @Override
    public boolean equals(Object other) {
        if (other == null) return false;
        if (other == this) return true;
        if (other.getClass() != getClass()) return false;
        ApacheSimpleRegression otherMe = (ApacheSimpleRegression) other;
        byte[] otherSerialized = SerializationUtils.serialize(otherMe.simpleRegression);
        byte[] meSerialized = SerializationUtils.serialize(simpleRegression);
        if (otherSerialized.length != meSerialized.length) return false;
        for (int i = 0; i < meSerialized.length; i++) {
            if (meSerialized[i] != otherSerialized[i]) return false;
        }
        return true;
    }
}
