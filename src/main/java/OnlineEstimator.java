/*
 * Copyright 2019 InfAI (CC SES)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */


import com.yahoo.labs.samoa.instances.*;
import moa.classifiers.Classifier;
import moa.classifiers.functions.AdaGrad;
import moa.classifiers.functions.SGD;
import moa.classifiers.meta.AdaptiveRandomForestRegressor;
import org.infai.seits.sepl.operators.Config;
import org.infai.seits.sepl.operators.Message;
import org.infai.seits.sepl.operators.OperatorInterface;
import org.joda.time.DateTimeUtils;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAccessor;
import java.util.ArrayList;


public class OnlineEstimator implements OperatorInterface {
    protected ZoneId timezone;
    protected Classifier classifier;
    protected InstancesHeader header;


    public OnlineEstimator(){
        ArrayList<Attribute> attributesList = new ArrayList<>();
        attributesList.add(new Attribute("timestamp", "yyyy-MM-dd'T'HH:mm:ssZ"));
        attributesList.add(new Attribute("value"));
        Config config = new Config();
        String configValue = config.getConfigValue("Algorithm", "");
        String configTimezone = config.getConfigValue("Timezone", "+02");
        timezone = ZoneId.of(configTimezone); //As configured
        Instances instances = new Instances("", attributesList, 0);
        instances.setClassIndex(attributesList.size() - 1);
        header = new InstancesHeader(instances);
        switch (configValue) {
            case "online-AdaptiveRandomForestRegressor":
                classifier = new AdaptiveRandomForestRegressor();
                break;
            case "online-AdaGrad":
                classifier = new AdaGrad();
                ((AdaGrad) classifier).setEpsilon(0.9);
                break;
            case "online-SGD":
                classifier = new SGD();
                ((SGD) classifier).setLearningRate(0.5);
                ((SGD) classifier).setLossFunction(2);
                break;
            default:
                System.out.println("Your specified algorithm is not implemented, falling back to AdaptiveRandomForestRegressor!");
                classifier = new AdaptiveRandomForestRegressor();
                break;
        }
        classifier.prepareForUse();
    }

    @Override
    public void run(Message message) {
        TemporalAccessor temporalAccessor = DateTimeFormatter.ISO_OFFSET_DATE_TIME.parse(DateParser.parseDate(message.getInput("timestamp").getString()));

        //Prepare values from message
        final long timestamp = Instant.from(temporalAccessor).toEpochMilli();
        final double value = message.getInput("value").getValue();

        //Calculate timestamps for prediction
        long currentMillis = DateTimeUtils.currentTimeMillis(); //Needs to use this method for testing
        Instant instant = Instant.ofEpochMilli(currentMillis);

        ZonedDateTime zdt = ZonedDateTime.ofInstant(instant, timezone);

        //Create Strings representing start and end of day, month and year
        String tsSOD = DateParser.parseDate(zdt.withHour(0).withMinute(0).withSecond(0).withNano(0).toString());
        String tsSOM = DateParser.parseDate(zdt.withDayOfMonth(1).withHour(0).withMinute(0).withSecond(0).withNano(0).toString());
        String tsSOY = DateParser.parseDate(zdt.withDayOfYear(1).withHour(0).withMinute(0).withSecond(0).withNano(0).toString());
        String tsEOD = DateParser.parseDate(zdt.plusDays(1).withHour(0).withMinute(0).withSecond(0).withNano(0).minusSeconds(1).toString());
        String tsEOM = DateParser.parseDate(zdt.plusMonths(1).withDayOfMonth(1).withHour(0).withMinute(0).withSecond(0).withNano(0).minusSeconds(1).toString());
        String tsEOY = DateParser.parseDate(zdt.plusYears(1).withDayOfYear(1).withHour(0).withMinute(0).withSecond(0).withNano(0).minusSeconds(1).toString());

        //Convert created Strings into long timestamps
        long tsSODl = DateParser.parseDateMills(tsSOD);
        long tsSOMl = DateParser.parseDateMills(tsSOM);
        long tsSOYl = DateParser.parseDateMills(tsSOY);
        long tsEODl = DateParser.parseDateMills(tsEOD);
        long tsEOMl = DateParser.parseDateMills(tsEOM);
        long tsEOYl = DateParser.parseDateMills(tsEOY);

        //Train on new data
        Instance instance = new DenseInstance(2);
        instance.setDataset(header);
        instance.setValue(0, timestamp);
        instance.setValue(1, value);
        classifier.trainOnInstance(instance);

        //prepare instances for prediction
        Instance eod = new DenseInstance(1); //End Of Day
        Instance eom = new DenseInstance(1); //End Of Month
        Instance eoy = new DenseInstance(1); //End Of Year
        Instance sod = new DenseInstance(1); //Start Of Day
        Instance som = new DenseInstance(1); //Start Of Month
        Instance soy = new DenseInstance(1); //Start Of Year
        eod.setValue(0, tsEODl);
        eom.setValue(0, tsEOMl);
        eoy.setValue(0, tsEOYl);
        sod.setValue(0, tsSODl);
        som.setValue(0, tsSOMl);
        soy.setValue(0, tsSOYl);
        eod.setDataset(header);
        eom.setDataset(header);
        eoy.setDataset(header);
        sod.setDataset(header);
        som.setDataset(header);
        soy.setDataset(header);

        //Calculate predictions
        double predEOD = classifier.getPredictionForInstance(eod).getVotes()[0];
        double offset = classifier.getPredictionForInstance(sod).getVotes()[0];
        //Submit results
        message.output("DayTimestamp", tsEOD);
        message.output("DayPrediction", predEOD - offset);

        double predEOM = classifier.getPredictionForInstance(eom).getVotes()[0];
        offset = classifier.getPredictionForInstance(som).getVotes()[0];
        //Submit results
        message.output("MonthTimestamp", tsEOM);
        message.output("MonthPrediction", predEOM - offset);

        double predEOY = classifier.getPredictionForInstance(eoy).getVotes()[0];
        offset = classifier.getPredictionForInstance(soy).getVotes()[0];
        //Submit results
        message.output("YearTimestamp", tsEOY);
        message.output("YearPrediction", predEOY - offset);
    }

    @Override
    public void config(Message message) {
        message.addInput("value");
        message.addInput("timestamp");
    }
}
