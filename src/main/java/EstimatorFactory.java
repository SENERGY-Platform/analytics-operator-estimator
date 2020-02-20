import org.infai.seits.sepl.operators.Config;
import org.infai.seits.sepl.operators.OperatorInterface;

import java.time.ZoneId;

public class EstimatorFactory {


    public static OperatorInterface createNewInstance(){
        Config config = new Config();
        String algorithm = config.getConfigValue("Algorithm", "apache-simple");
        String configTimezone = config.getConfigValue("Timezone", "+02");
        ZoneId timezone = ZoneId.of(configTimezone); //As configured
        EstimatorInterface estimator;
        switch(algorithm) {
            case "apache-simple":
            default:
                estimator = new ApacheSimpleRegression();
        }
        return new Estimator(estimator, timezone);
    }
}
