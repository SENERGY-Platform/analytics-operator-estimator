import org.infai.ses.senergy.exceptions.NoValueException;
import org.infai.ses.senergy.operators.BaseOperator;
import org.infai.ses.senergy.operators.Message;
import org.joda.time.DateTimeUtils;

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.temporal.TemporalAccessor;

public class Estimator extends BaseOperator {
    private EstimatorInterface estimator;
    private ZoneId timezone;
    private long ignoreValuesOlderThanMs;

    public Estimator(EstimatorInterface estimator, ZoneId timezone, long ignoreValuesOlderThanMs) {
        this.estimator = estimator;
        this.timezone = timezone;
        this.ignoreValuesOlderThanMs = ignoreValuesOlderThanMs;
    }

    public EstimatorInterface getEstimator() {
        return estimator;
    }

    @Override
    public void run(Message message) {
        TemporalAccessor temporalAccessor;
        String messageTimestamp;
        try {
            messageTimestamp = message.getInput("timestamp").getString();
        } catch (NullPointerException npe) {
            System.err.println("Message does not have a timestamp!");
            return;
        }
        try {
            temporalAccessor = DateTimeFormatter.ISO_OFFSET_DATE_TIME.parse(DateParser.parseDate(messageTimestamp));
        } catch (DateTimeParseException e) {
            System.err.println("Skipping message: Could not parse date: " + message.getInput("timestamp").getString());
            e.printStackTrace();
            return;
        }

        //Prepare values from message
        final long timestamp = Instant.from(temporalAccessor).toEpochMilli();
        final double value;
        try {
            value = message.getInput("value").getValue();
        } catch (NoValueException e) {
            System.err.println(e.getMessage());
            return;
        }

        //Calculate timestamps for prediction
        long currentMillis = DateTimeUtils.currentTimeMillis(); //Needs to use this method for testing
        if (currentMillis - timestamp > ignoreValuesOlderThanMs) {
            System.err.println("Skipping message: Value too old: " + messageTimestamp + " was more than one year ago");
            return;
        }

        //Add data to regression
        estimator.addData(timestamp, value);

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


        //Calculate predictions
        double predEOD = estimator.predict(tsEODl);
        double offset = estimator.predict(tsSODl);
        //Submit results
        message.output("DayTimestamp", tsEOD);
        message.output("DayPrediction", predEOD - offset);
        message.output("DayPredictionTotal", predEOD);

        double predEOM = estimator.predict(tsEOMl);
        offset = estimator.predict(tsSOMl);
        message.output("MonthTimestamp", tsEOM);
        message.output("MonthPrediction", predEOM - offset);
        message.output("MonthPredictionTotal", predEOM);

        double predEOY = estimator.predict(tsEOYl);
        offset = estimator.predict(tsSOYl);
        message.output("YearTimestamp", tsEOY);
        message.output("YearPrediction", predEOY - offset);
        message.output("YearPredictionTotal", predEOY);
    }

    @Override
    public Message configMessage(Message message) {
        message.addInput("value");
        message.addInput("timestamp");
        return message;
    }
}
