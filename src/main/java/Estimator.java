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
import weka.classifiers.Classifier;
import weka.classifiers.functions.LinearRegression;
import weka.core.Attribute;
import weka.core.DenseInstance;
import weka.core.Instance;
import weka.core.Instances;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoField;
import java.time.temporal.TemporalAccessor;
import java.util.ArrayList;


public class Estimator implements OperatorInterface {
    protected Instances[][][] instances;
    protected Classifier[][][] classifiers;
    protected ArrayList<Attribute> attributesList;


    public Estimator(){
        attributesList = new ArrayList<>();
        attributesList.add(new Attribute("timestamp"));
        attributesList.add(new Attribute("value"));
        instances = new Instances[3000][][];
        classifiers = new Classifier[3000][][];
    }

    @Override
    public void run(Message message) {
        //Extract year, month and day from message
        TemporalAccessor temporalAccessor = DateTimeFormatter.ISO_OFFSET_DATE_TIME.parse(DateParser.parseDate(message.getInput("timestamp").getString()));
        final int year = temporalAccessor.get(ChronoField.YEAR);
        final int month = temporalAccessor.get(ChronoField.MONTH_OF_YEAR);
        final int day = temporalAccessor.get(ChronoField.DAY_OF_MONTH);

        //Initialize Arrays for month and day if needed
        if(instances[year] == null || classifiers[year] == null){
            instances[year] = new Instances[13][]; //need one for the whole year
            classifiers[year] = new Classifier[13][];
        }
        if(instances[year][month] == null || classifiers[year][month] == null){
            instances[year][month] = new Instances[32]; //need one for the whole month
            classifiers[year][month] = new Classifier[32];
        }

        //Initialize Instances and Linear Regression for year, month and day if needed
        if(instances[year][0] == null || classifiers[year][0] == null){ //[year][0][0] is the for the whole year
            instances[year][0] = new Instances[1];
            classifiers[year][0] = new LinearRegression[1];
            instances[year][0][0] = getInstances();
            classifiers[year][0][0] = getClassifier();
        }
        if(instances[year][month][0] == null || classifiers[year][month][0] == null){ //[year][month][0] is for the whle month
            instances[year][month][0] = getInstances();
            classifiers[year][month][0] = getClassifier();
        }
        if(instances[year][month][day] == null || classifiers[year][month][day] == null){ //[year][month][day] is for the day
            instances[year][month][day] = getInstances();
            classifiers[year][month][day] = getClassifier();
        }

        //Prepare values from message
        final long timestamp = Instant.from(temporalAccessor).toEpochMilli();
        final double value = message.getInput("value").getValue();


        //Insert message values in Instances for year, month and day
        Instance instanceYear = new DenseInstance(2);
        instanceYear.setDataset(instances[year][0][0]);
        instanceYear.setValue(0, timestamp);
        instanceYear.setValue(1, value);
        instances[year][0][0].add(instanceYear);

        Instance instanceMonth = new DenseInstance(2);
        instanceMonth.setDataset(instances[year][month][0]);
        instanceMonth.setValue(0, timestamp);
        instanceMonth.setValue(1, value);
        instances[year][month][0].add(instanceMonth);

        Instance instanceDay = new DenseInstance(2);
        instanceDay.setDataset(instances[year][month][day]);
        instanceDay.setValue(0, timestamp);
        instanceDay.setValue(1, value);
        instances[year][month][day].add(instanceYear);

        //Build classifiers
        try {
            classifiers[year][0][0].buildClassifier(instances[year][0][0]);
            classifiers[year][month][0].buildClassifier(instances[year][month][0]);
            classifiers[year][month][day].buildClassifier(instances[year][month][day]);
        } catch (Exception e) {
            System.err.println("Could not build Classifier: " + e.getMessage());
            e.printStackTrace();
        }

        //prepare instances for predction
        Instance eod = new DenseInstance(1); //End Of Day
        Instance eom = new DenseInstance(1); //End Of Month
        Instance eoy = new DenseInstance(1); //End Of Year

        //Calculate timestamps for prediction
        Instant instant = Instant.ofEpochMilli(DateTimeUtils.currentTimeMillis()); //Needs to use this method for testing
        ZoneId zoneId = ZoneId.of( OffsetDateTime.now().getOffset().toString() ); //Assumes local
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
            double predEOD = classifiers[year][month][day].classifyInstance(eod);
            double predEOM = classifiers[year][month][0].classifyInstance(eom);
            double predEOY = classifiers[year][0][0].classifyInstance(eoy);

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

    protected Classifier getClassifier(){
        Classifier linreg = new LinearRegression();
        return linreg;
    }

    protected Instances getInstances(){
        Instances instances = new Instances("", attributesList, 1);
        instances.setClassIndex(1);
        return instances;
    }
}
