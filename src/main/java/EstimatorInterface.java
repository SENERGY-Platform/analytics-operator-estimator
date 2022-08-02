import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;

public interface EstimatorInterface {
    void addData(long timestamp, double value);

    double predict(long timestamp);

    default void loadSaved(FileInputStream f) throws Exception {}

    default void save(FileOutputStream f) throws Exception {}
}
