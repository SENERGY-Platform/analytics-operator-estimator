/*
 * Copyright 2018 InfAI (CC SES)
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


import org.infai.seits.sepl.operators.Config;
import org.infai.seits.sepl.operators.Message;
import org.infai.seits.sepl.operators.OperatorInterface;
import weka.classifiers.Classifier;
import weka.classifiers.functions.SMOreg;
import weka.core.Attribute;
import weka.core.DenseInstance;
import weka.core.Instance;
import weka.core.Instances;

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAccessor;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;


public class Estimator implements OperatorInterface {
    protected Classifier classifier;
    protected ArrayList<Attribute> attributesList;
    protected ZoneId timezone;
    protected Map<String, Instances> map;


    public Estimator(){
        attributesList = new ArrayList<>();
        attributesList.add(new Attribute("timestamp"));
        attributesList.add(new Attribute("value"));
        map = new HashMap<>();
        Config config = new Config();
        String configTimezone = config.getConfigValue("Timezone", "+02");
        timezone = ZoneId.of(configTimezone); //As configured
        System.out.println("Using SMOreg");
        classifier = new SMOreg();
    }

    @Override
    public void run(Message message) {
        String METER_ID = message.getInput("device_id").getString();

        Instances instances;
        if (map.containsKey(METER_ID)) {
            instances = map.get(METER_ID);
        } else {
            instances =  new Instances("", attributesList, 1);
            instances.setClassIndex(1);
        }

        TemporalAccessor temporalAccessor = DateTimeFormatter.ISO_OFFSET_DATE_TIME.parse(DateParser.parseDate(message.getInput("timestamp").getString()));

        //Prepare values from message
        final long timestamp = Instant.from(temporalAccessor).toEpochMilli();
        final double value = message.getInput("value").getValue();

        //Calculate timestamp for prediction
        double tsEOY = ZonedDateTime.now().withDayOfYear(0).withHour(0).withMinute(0).withSecond(0).withNano(0).plusYears(1).minusSeconds(1).toInstant().toEpochMilli();

        //Insert message values in Instances and save back to the map
        Instance instance = new DenseInstance(2);
        instance.setDataset(instances);
        instance.setValue(0, timestamp);
        instance.setValue(1, value);
        instances.add(instance);
        map.put(METER_ID, instances);

        //prepare instances for prediction
        Instance eoy = new DenseInstance(1); //End Of Year
        eoy.setValue(0, tsEOY);

        try {
            classifier.buildClassifier(instances);
            double predEOY = classifier.classifyInstance(eoy);
            //Submit results
            message.output("PredictionTimestamp", tsEOY);
            message.output("Prediction", predEOY);
        } catch (Exception e) { //Building, filtering and predicting may throw exception
            System.err.println("Could not calculate prediction: " + e.getMessage());
        }
    }

    @Override
    public void config(Message message) {
        message.addInput("value");
        message.addInput("timestamp");
        message.addInput("device_id");
    }
}
