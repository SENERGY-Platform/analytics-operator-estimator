import moa.classifiers.Classifier;
import moa.classifiers.trees.FIMTDD;

public class MoaFIMTDDRegression extends AbstractMOAEstimator {

    @Override
    protected Classifier createClassifier() {
        FIMTDD tree = new FIMTDD();
        tree.regressionTreeOption.set();
        tree.gracePeriodOption.setValue(200);
        tree.splitConfidenceOption.setValue(0.0000001);
        tree.tieThresholdOption.setValue(0.05);
        tree.learningRatioOption.setValue(0.02);
        tree.learningRateDecayFactorOption.setValue(0.001);
        tree.learningRatioConstOption.set();
        return tree;
    }

}
