import com.yahoo.labs.samoa.instances.Attribute;
import com.yahoo.labs.samoa.instances.Instances;
import com.yahoo.labs.samoa.instances.InstancesHeader;
import moa.classifiers.Classifier;
import moa.classifiers.functions.AdaGrad;

import java.util.List;

public class EstimatorContainer {
    public Classifier classifier;
    public InstancesHeader header;
    public int numTrained;

    public EstimatorContainer(String name, List<Attribute> attributes) {
        Instances instances = new Instances(name, attributes, 0);
        instances.setClassIndex(attributes.size() - 1);
        header = new InstancesHeader(instances);
        classifier = new AdaGrad();
    }
}
