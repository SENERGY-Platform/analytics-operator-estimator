import com.yahoo.labs.samoa.instances.*;
import moa.classifiers.Classifier;

import java.util.ArrayList;

public abstract class AbstractMOAEstimator implements EstimatorInterface {
    private Classifier classifier;
    private InstancesHeader header;

    public AbstractMOAEstimator() {
        ArrayList<Attribute> attributesList = new ArrayList<>();
        attributesList.add(new Attribute("timestamp"));
        attributesList.add(new Attribute("value"));

        Instances instances = new Instances("", attributesList, 0);
        instances.setClassIndex(attributesList.size() - 1);
        header = new InstancesHeader(instances);

        classifier = createClassifier();
        classifier.prepareForUse();
    }


    @Override
    public void addData(long timestamp, double value) {
        Instance inst = new DenseInstance(2);
        inst.setDataset(header);
        inst.setValue(0, timestamp);
        inst.setValue(1, value);
        classifier.trainOnInstance(inst);
    }

    @Override
    public double predict(long timestamp) {
        Instance inst = new DenseInstance(1);
        inst.setDataset(header);
        inst.setValue(0, timestamp);
        double[] votes = classifier.getVotesForInstance(inst);
        return votes[0];
    }

    protected abstract Classifier createClassifier();
}
