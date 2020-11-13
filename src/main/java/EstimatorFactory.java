import org.infai.ses.senergy.operators.Config;
import org.infai.ses.senergy.utils.ConfigProvider;

import java.time.ZoneId;

public class EstimatorFactory {

    public static Estimator createNewInstance() {
        Config config = ConfigProvider.getConfig();
        return createNewInstanceWithConfig(config);
    }

    public static Estimator createNewInstanceWithConfig(Config config){
        String algorithm = config.getConfigValue("Algorithm", "apache-simple");
        String configTimezone = config.getConfigValue("Timezone", "+02");
        ZoneId timezone = ZoneId.of(configTimezone); //As configured
        long ignoreValuesOlderThanMs = Long.parseLong(config.getConfigValue("ignoreValuesOlderThanMs", "31557600000"));
        EstimatorInterface estimator;
        switch(algorithm) {
            case "moa-fimtdd":
                estimator = new MoaFIMTDDRegression();
                break;
            case "moa-arf":
                estimator = new MoaARF();
                break;
            case "apache-simple":
            default:
                estimator = new ApacheSimpleRegression();
        }
        return new Estimator(estimator, timezone, ignoreValuesOlderThanMs);
    }
}
