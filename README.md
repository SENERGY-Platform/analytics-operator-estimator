# operator-estimator

Collects numeric values from a device and estimates the values for the end of the current day, the current month and the current year.
Estimations are only based on data of current day/month/year.
Since the operator is mainly written to predict consumption data it will provide estimations in an offset-format,
 which means an estimation of the value of the beginning of the day/month/year will be subtracted from the real estimation.    

## Inputs

* value (float): Reading from device
* timestamp (string): Timestamp from reading

## Outputs

* DayTimestamp (string): String representation of the timestamp of the end of the current day
* DayPrediction (float): Prediction of the value at DayTimestamp with subtraction of estimated value at beginning of day
* DayPredictionTotal (float): Prediction of the value at DayTimestamp
* MonthTimestamp (string): String representation of the timestamp of the end of the current month
* MonthPrediction (float): Prediction of the value at MonthTimestamp
* MonthPredictionTotal (float): Prediction of the value at MonthTimestamp with subtraction of estimated value at beginning of month
* YearTimestamp (string): String representation of the timestamp of the end of the current year
* YearPrediction (float): Prediction of the value at YearTimestamp
* YearPredictionTotal (float): Prediction of the value at YearTimestamp with subtraction of estimated value at beginning of month

## Configs
 * Algorithm (string): Can be either one of
   + 'LinearRegression' (default)
   + 'SimpleLinearRegression'
   + 'GaussianProcesses'
   + 'SMOreg'
   + 'online-AdaptiveRandomForestRegressor' (untested)
   + 'online-AdaGrad' (untested)
   + 'online-SGD' (untested)
 * Timezone (string): Used to determine end of day/month/year. Can be anything able to be parsed by [ZoneId.of(String)](https://docs.oracle.com/javase/8/docs/api/java/time/ZoneId.html#of-java.lang.String-).
   Default value is '+02:00'.
