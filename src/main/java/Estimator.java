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


import org.infai.seits.sepl.operators.Message;
import org.infai.seits.sepl.operators.OperatorInterface;
import org.joda.time.DateTimeUtils;
import org.json.JSONObject;
import weka.classifiers.functions.LinearRegression;
import weka.core.Attribute;
import weka.core.DenseInstance;
import weka.core.Instance;
import weka.core.Instances;

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;


public class Estimator implements OperatorInterface {
    Instances instances;
    LinearRegression linreg;



    public Estimator(){
        ArrayList<Attribute> attributesList = new ArrayList<>();
        attributesList.add(new Attribute("timestamp"));
        attributesList.add(new Attribute("value"));
        instances = new Instances("data", attributesList, 1);
        instances.setClassIndex(1);
        linreg = new LinearRegression();

    }

    @Override
    public void run(Message message) {
        Instance instance = new DenseInstance(2);
        long timestamp = DateParser.parseDateMills(message.getInput("timestamp").getString());
        instance.setDataset(instances);
        instance.setValue(0, timestamp);
        instance.setValue(1, message.getInput("value").getValue());
        instances.add(instance);

        try {
            linreg.buildClassifier(instances);
        } catch (Exception e) {
            System.err.println("Could not build Classifier: " + e.getMessage());
            e.printStackTrace();
        }
        Instance eod = new DenseInstance(1); //End Of Day
        Instance eom = new DenseInstance(1); //End Of Month
        Instance eoy = new DenseInstance(1); //End Of Year

        Instant instant = Instant.ofEpochMilli(DateTimeUtils.currentTimeMillis()); //Needs to use this method for testing
        ZoneId zoneId = ZoneId.of( "Europe/Berlin" ); //TODO
        ZonedDateTime zdt = ZonedDateTime.ofInstant( instant , zoneId );
        String tsEOD = DateParser.parseDate(zdt.plusDays(1).withHour(0).withMinute(0).withSecond(0).withNano(0).toString());
        String tsEOM = DateParser.parseDate(zdt.plusMonths(1).withDayOfMonth(1).withHour(0).withMinute(0).withSecond(0).withNano(0).toString());
        String tsEOY = DateParser.parseDate(zdt.plusYears(1).withDayOfYear(1).withHour(0).withMinute(0).withSecond(0).withNano(0).toString());

        long tsEODl = DateParser.parseDateMills(tsEOD);
        long tsEOMl = DateParser.parseDateMills(tsEOM);
        long tsEOYl = DateParser.parseDateMills(tsEOY);

        eod.setValue(0, tsEODl);
        eom.setValue(0, tsEOMl);
        eoy.setValue(0, tsEOYl);

        try {
            double predEOD = linreg.classifyInstance(eod);
            double predEOM = linreg.classifyInstance(eom);
            double predEOY = linreg.classifyInstance(eoy);

            message.output("DayTimestamp", tsEOD);
            message.output("DayPrediction", predEOD);
            message.output("MonthTimestamp", tsEOM);
            message.output("MonthPrediction", predEOM);
            message.output("YearTimestamp", tsEOY);
            message.output("YearPrediction", predEOY);

        } catch (Exception e) {
            System.err.println("Could not calculate prediction: " + e.getMessage());
            e.printStackTrace();
        }

    }

    @Override
    public void config(Message message) {
        message.addInput("value");
        message.addInput("timestamp");
    }
}
