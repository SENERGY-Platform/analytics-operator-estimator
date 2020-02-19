public interface EstimatorInterface {
    void addData(long timestamp, double value);
    double predict(long timestamp);
}
