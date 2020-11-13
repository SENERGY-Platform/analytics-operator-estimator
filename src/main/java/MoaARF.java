import moa.classifiers.Classifier;
import moa.classifiers.meta.AdaptiveRandomForestRegressor;

public class MoaARF extends AbstractMOAEstimator {

    @Override
    protected Classifier createClassifier() {
        return new AdaptiveRandomForestRegressor();
    }

}
