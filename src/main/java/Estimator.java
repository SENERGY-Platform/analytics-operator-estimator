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
import org.joda.time.DateTimeUtils;
import weka.classifiers.Classifier;
import weka.classifiers.functions.GaussianProcesses;
import weka.classifiers.functions.LinearRegression;
import weka.classifiers.functions.SMOreg;
import weka.classifiers.functions.SimpleLinearRegression;
import weka.core.*;
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
        String configValue = new Config().getConfigValue("Algorithm", "LinearRegression");
        switch (configValue){
            case "LinearRegression":
                System.out.println("Using LinearRegression");
                classifier = new LinearRegression();
                break;
            case "GaussianProcesses":
                System.out.println("Using GaussianProcesses");
                classifier  = new GaussianProcesses();
                ((GaussianProcesses) classifier).setFilterType(new SelectedTag(GaussianProcesses.FILTER_STANDARDIZE, GaussianProcesses.TAGS_FILTER));
                break;
            case "SimpleLinearRegression":
                System.out.println("Using SimpleLinearRegression");
                classifier = new SimpleLinearRegression();
                break;
            case "SMOreg":
                System.out.println("Using SMOreg");
                classifier = new SMOreg();
                break;
            default:
                System.out.println("Your specified algorithm is not implemented, falling back to Linear Regression!");
                classifier = new LinearRegression();
                break;
        }
    }

    @Override
    public void run(Message message) {
        TemporalAccessor temporalAccessor = DateTimeFormatter.ISO_OFFSET_DATE_TIME.parse(DateParser.parseDate(message.getInput("timestamp").getString()));

        //Prepare values from message
        final long timestamp = Instant.from(temporalAccessor).toEpochMilli();
        final double value = message.getInput("value").getValue();


        //Insert message values in Instances
        Instance instance = new DenseInstance(2);
        instance.setDataset(instances);
        instance.setValue(0, timestamp);
        instance.setValue(1, value);
        instances.add(instance);

        //Calculate timestamps for prediction
        Instant instant = Instant.ofEpochMilli(DateTimeUtils.currentTimeMillis()); //Needs to use this method for testing
        ZoneId zoneId = ZoneId.of( OffsetDateTime.now().getOffset().toString() ); //Assumes local
        ZonedDateTime zdt = ZonedDateTime.ofInstant( instant , zoneId );

        //Create Strings representing start and end of day, month and year
        String tsEODs = DateParser.parseDate(zdt.withHour(0).withMinute(0).withSecond(0).withNano(0).toString());
        String tsEOMs = DateParser.parseDate(zdt.withDayOfMonth(1).withHour(0).withMinute(0).withSecond(0).withNano(0).toString());
        String tsEOYs = DateParser.parseDate(zdt.withDayOfYear(1).withHour(0).withMinute(0).withSecond(0).withNano(0).toString());
        String tsEOD = DateParser.parseDate(zdt.plusDays(1).withHour(0).withMinute(0).withSecond(0).withNano(0).toString());
        String tsEOM = DateParser.parseDate(zdt.plusMonths(1).withDayOfMonth(1).withHour(0).withMinute(0).withSecond(0).withNano(0).toString());
        String tsEOY = DateParser.parseDate(zdt.plusYears(1).withDayOfYear(1).withHour(0).withMinute(0).withSecond(0).withNano(0).toString());

        //Convert created Strings into long timestamps
        long tsEODls = DateParser.parseDateMills(tsEODs);
        long tsEOMls = DateParser.parseDateMills(tsEOMs);
        long tsEOYls = DateParser.parseDateMills(tsEOYs);
        long tsEODl = DateParser.parseDateMills(tsEOD);
        long tsEOMl = DateParser.parseDateMills(tsEOM);
        long tsEOYl = DateParser.parseDateMills(tsEOY);

        //prepare instances for prediction
        Instance eod = new DenseInstance(1); //End Of Day
        Instance eom = new DenseInstance(1); //End Of Month
        Instance eoy = new DenseInstance(1); //End Of Year
        eod.setValue(0, tsEODl);
        eom.setValue(0, tsEOMl);
        eoy.setValue(0, tsEOYl);

        try {
            //Train classifiers and calculate predictions
            classifier.buildClassifier(filter(tsEODls,tsEODl,instances));
            double predEOD = classifier.classifyInstance(eod);
            //Submit results
            message.output("DayTimestamp", tsEOD);
            message.output("DayPrediction", predEOD);
        } catch (Exception e) { //Building, filtering and predicting may throw exception
            System.err.println("Could not calculate prediction: " + e.getMessage());
        }
        try {
            classifier.buildClassifier(filter(tsEOMls, tsEOMl, instances));
            double predEOM = classifier.classifyInstance(eom);
            //Submit results
            message.output("MonthTimestamp", tsEOM);
            message.output("MonthPrediction", predEOM);

        } catch (Exception e) { //Building, filtering and predicting may throw exception
            System.err.println("Could not calculate prediction: " + e.getMessage());
        }
        try {
            //Train classifiers and calculate predictions
            classifier.buildClassifier(filter(tsEOYls, tsEOYl, instances));
            double predEOY = classifier.classifyInstance(eoy);
            //Submit results
            message.output("YearTimestamp", tsEOY);
            message.output("YearPrediction", predEOY);
        } catch (Exception e) { //Building, filtering and predicting may throw exception
            System.err.println("Could not calculate prediction: " + e.getMessage());
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
        Instances returnInstances = Filter.useFilter(dataEndTruncated, filter2);
        return returnInstances;
    }
}
