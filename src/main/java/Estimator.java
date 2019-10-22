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


import com.yahoo.labs.samoa.instances.Attribute;
import com.yahoo.labs.samoa.instances.DenseInstance;
import com.yahoo.labs.samoa.instances.Instance;
import com.yahoo.labs.samoa.instances.Prediction;
import org.infai.seits.sepl.operators.Config;
import org.infai.seits.sepl.operators.Message;
import org.infai.seits.sepl.operators.OperatorInterface;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAccessor;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;


public class Estimator implements OperatorInterface {
    protected ArrayList<Attribute> attributesList;
    protected Map<String, EstimatorContainer> map;
    protected boolean log_console, log_file;
    protected BufferedWriter bw;

    public Estimator(){
        attributesList = new ArrayList<>();
        attributesList.add(new Attribute("timestamp"));
        attributesList.add(new Attribute("value"));
        Config config = new Config();
        log_console = Boolean.parseBoolean(config.getConfigValue("log_console", "false"));
        log_file = Boolean.parseBoolean(config.getConfigValue("log_file", "false"));
        if (log_file) {
            try {
                String filename = config.getConfigValue("log_filename", "./log.csv");
                bw = new BufferedWriter(new FileWriter(filename));
                bw.write("Device,MessageNo,TimeInMs\n");
                bw.flush();
            } catch (IOException e) {
                System.err.println("Problem when creating/opening file. Logs will not be written!");
                e.printStackTrace();
            }
        }
        map = new HashMap<>();
    }

    @Override
    public void run(Message message) {
        long startTime = System.currentTimeMillis();

        String METER_ID = message.getInput("device_id").getString();

        EstimatorContainer container;
        if (map.containsKey(METER_ID)) {
            container = map.get(METER_ID);
        } else {
            container = new EstimatorContainer(METER_ID, attributesList);
        }

        TemporalAccessor temporalAccessor = DateTimeFormatter.ISO_OFFSET_DATE_TIME.parse(DateParser.parseDate(message.getInput("timestamp").getString()));

        //Prepare values from message
        final long timestamp = Instant.from(temporalAccessor).toEpochMilli();
        final double value = message.getInput("value").getValue();

        //Calculate timestamp for prediction
        double tsEOY = ZonedDateTime.from(temporalAccessor).withDayOfYear(1).withHour(0).withMinute(0).withSecond(0).withNano(0).plusYears(1).minusSeconds(1).toInstant().toEpochMilli();

        //Insert message values in Instances and save back to the map
        Instance instance = new DenseInstance(2);
        instance.setDataset(container.header);
        instance.setValue(0, timestamp);
        instance.setClassValue(value);


        //prepare instances for prediction
        Instance eoy = new DenseInstance(1); //End Of Year
        eoy.setValue(0, tsEOY);
        eoy.setDataset(container.header);
        try {
            container.classifier.trainOnInstance(instance);
        } catch(Exception e) {
            System.err.println("Could not train instance, skipping this message, see error below. Device ID was " + METER_ID);
            e.printStackTrace();
            return;
        }
        container.numTrained++;
        map.put(METER_ID, container);
        Prediction p;
        try {
            p = container.classifier.getPredictionForInstance(eoy);
        }catch (Exception e){
            System.err.println("Could not get prediction, skipping this message, see error below");
            e.printStackTrace();
            return;
        }

        double predcition = p.getVotes()[0];
        if(Double.isNaN(predcition)){
            System.out.println("Classifier gave NaN as prediction, skipping this message");
            return;
        }
        message.output("PredictionTimestamp", new Timestamp((long) tsEOY).toString());
        Double compareValue = message.getInput("actual_value").getValue();
        message.output("Prediction", predcition);
        message.output("ActualValue", compareValue);
        message.output("MessagesUsedForPrediction", container.numTrained);

        // LOGGING
        long endTime = System.currentTimeMillis();
        long diff = endTime - startTime;
        if(log_console) {
            System.out.println("DEVICE: " + METER_ID + "\t#Msg: " + container.numTrained + "\tTIME: " + diff);
        }
        if(log_file && bw != null) {
            try {
                bw.write(METER_ID + "," + container.numTrained + "," + diff + "\n");
                bw.flush();
            } catch (IOException e) {
                System.err.println("Could not log!");
                e.printStackTrace();
            }
        }
    }

    @Override
    public void config(Message message) {
        message.addInput("value");
        message.addInput("timestamp");
        message.addInput("device_id");
        message.addInput("actual_value");
    }
}
