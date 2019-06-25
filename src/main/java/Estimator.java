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
import weka.filters.Filter;
import weka.filters.unsupervised.instance.RemoveWithValues;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAccessor;
import java.util.ArrayList;


public class Estimator implements OperatorInterface {
    protected Instances instances;
    protected Classifier classifier;
    protected ArrayList<Attribute> attributesList;


    public Estimator(){
        attributesList = new ArrayList<>();
        attributesList.add(new Attribute("timestamp"));
        attributesList.add(new Attribute("value"));
        instances =  new Instances("", attributesList, 1);
        instances.setClassIndex(1);
        classifier = new LinearRegression();
    }

    @Override
    public void run(Message message) {
        //Extract year, month and day from message
        TemporalAccessor temporalAccessor = DateTimeFormatter.ISO_OFFSET_DATE_TIME.parse(DateParser.parseDate(message.getInput("timestamp").getString()));


        //Prepare values from message
        final long timestamp = Instant.from(temporalAccessor).toEpochMilli();
        final double value = message.getInput("value").getValue();


        //Insert message values in Instances for year, month and day
        Instance instance = new DenseInstance(2);
        instance.setDataset(instances);
        instance.setValue(0, timestamp);
        instance.setValue(1, value);
        instances.add(instance);

        //Build classifiers
        try {

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
        String tsEODs = DateParser.parseDate(zdt.withHour(0).withMinute(0).withSecond(0).withNano(0).toString());
        String tsEOMs = DateParser.parseDate(zdt.withDayOfMonth(1).withHour(0).withMinute(0).withSecond(0).withNano(0).toString());
        String tsEOYs = DateParser.parseDate(zdt.withDayOfYear(1).withHour(0).withMinute(0).withSecond(0).withNano(0).toString());
        String tsEOD = DateParser.parseDate(zdt.plusDays(1).withHour(0).withMinute(0).withSecond(0).withNano(0).toString());
        String tsEOM = DateParser.parseDate(zdt.plusMonths(1).withDayOfMonth(1).withHour(0).withMinute(0).withSecond(0).withNano(0).toString());
        String tsEOY = DateParser.parseDate(zdt.plusYears(1).withDayOfYear(1).withHour(0).withMinute(0).withSecond(0).withNano(0).toString());

        long tsEODls = DateParser.parseDateMills(tsEODs);
        long tsEOMls = DateParser.parseDateMills(tsEOMs);
        long tsEOYls = DateParser.parseDateMills(tsEOYs);
        long tsEODl = DateParser.parseDateMills(tsEOD);
        long tsEOMl = DateParser.parseDateMills(tsEOM);
        long tsEOYl = DateParser.parseDateMills(tsEOY);

        eod.setValue(0, tsEODl);
        eom.setValue(0, tsEOMl);
        eoy.setValue(0, tsEOYl);

        try {
            classifier.buildClassifier(filter(tsEODls,tsEODl,instances));
            double predEOD = classifier.classifyInstance(eod);

            classifier.buildClassifier(filter(tsEOMls, tsEOMl, instances));
            double predEOM = classifier.classifyInstance(eom);

            classifier.buildClassifier(filter(tsEOYls, tsEOYl, instances));
            double predEOY = classifier.classifyInstance(eoy);

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

    protected Instances filter(long start, long end, Instances data) throws Exception {
        RemoveWithValues filter = new RemoveWithValues();
        filter.setSplitPoint(end);
        filter.setInvertSelection(true);
        filter.setAttributeIndex("1");
        filter.setInputFormat(data);
        Instances dataEndTruncated = Filter.useFilter(data, filter);
        RemoveWithValues filter2 = new RemoveWithValues();
        filter2.setSplitPoint(start);
        filter2.setAttributeIndex("1");
        filter2.setInputFormat(dataEndTruncated);
        return Filter.useFilter(dataEndTruncated, filter2);
    }
}
