https://powcoder.com
代写代考加微信 powcoder
Assignment Project Exam Help
Add WeChat powcoder
package database;

import akka.japi.Pair;
import model.graph.*;
import org.joda.time.DateTime;
import org.joda.time.Days;
import org.joda.time.ReadableInstant;
import play.Logger;
import play.db.Database;
import utilities.DateUtility;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by Corey on 6/21/2017.
 * Project: magic_list_maker-server
 * <p></p>
 * Purpose of Class:
 */
@SuppressWarnings("WeakerAccess")
public class GraphDBAccessor extends DBAccessor {

    private static final double DAYS_IN_WEEK = 7.0;
    private static final double DAYS_IN_YEAR = 365.0;
    private static final double WEEKS_IN_MONTH = 4.35;
    private static final double MONTHS_IN_QUARTER = 3.0;
    private static final double QUARTERS_IN_YEAR = 4.0;

    static final String KEY_DATA = "data";
    static final String KEY_LABEL = "label";
    static final String KEY_DATA_X = "data_x";
    static final String KEY_DATA_Y = "data_y";

    private static final double MAX_DATA_POINTS = 50;

    public GraphDBAccessor(Database database) {
        super(database);
    }

    /**
     * @param startEndDatePair The start/end date formatted as 'yyyy-MM-DD'
     */
    public static Pair<GraphOutputDateFormat, String> convertGraphOutputDateFormatToRangeAsLabel(Pair<String, String> startEndDatePair,
                                                                                                 long startTime, long endTime,
                                                                                                 boolean filterWeekends,
                                                                                                 GraphOutputDateFormat format) {
        if (format == null || getNumberOfDataPointsBetween(startTime, endTime, filterWeekends, format) > MAX_DATA_POINTS) {
            format = getBestGraphOutputFormat(startTime, endTime, filterWeekends);
        }

        String date;
        switch (format) {
            case DAYS:
                date = "to_char(date_trunc(\'week\', " + startEndDatePair.first() + "), \'FMMM/FMDD/yyyy\') " +
                        "|| \' - \' || " +
                        "to_char(date_trunc(\'week\', " + startEndDatePair.second() + "), \'FMMM/FMDD/yyyy\') AS " + KEY_LABEL;
                break;
            case WEEKS:
                date = "to_char(date_trunc(\'month\', " + startEndDatePair.first() + "), \'FMMM/FMDD/yyyy\') " +
                        "|| \' - \' || " +
                        "to_char(date_trunc(\'month\', " + startEndDatePair.second() + "), \'FMMM/FMDD/yyyy\') AS " + KEY_LABEL;
                break;
            case MONTHS:
                date = "to_char(date_trunc(\'quarter\', " + startEndDatePair.first() + "), \'FMMonth\') " +
                        "|| \' - \' || " +
                        "to_char(date_trunc(\'quarter\', " + startEndDatePair.second() + "), \'FMMonth\') AS " + KEY_LABEL;
                break;
            case QUARTERS:
                date = "to_char(date_trunc(\'year\', " + startEndDatePair.first() + "), \'FMYYYY\') AS " + KEY_LABEL;
                break;
            case YEARS:
                date = "to_char(date_trunc(\'year\', " + startEndDatePair.first() + "), \'FMYYYY\') " +
                        "|| \' - \' || " +
                        "to_char(date_trunc(\'year\', " + startEndDatePair.second() + "), \'FMYYYY\')" + " AS " + KEY_LABEL;
                break;
            default:
                throw new IllegalArgumentException("Invalid format, found: " + format);
        }

        return new Pair<>(format, date);
    }

    public static String convertGraphOutputDateFormatToRangeAsLabel(String column, GraphOutputDateFormat format) {
        switch (format) {
            case DAYS:
                return "to_char(date_trunc(\'week\', " + column + "), \'FMMM/FMDD/yyyy\') " +
                        "|| \' - \' || " +
                        "to_char(date_trunc(\'week\', " + column + ") + interval \'6 days\', \'FMMM/FMDD/yyyy\') AS " + KEY_LABEL;
            case WEEKS:
                return "to_char(date_trunc(\'month\', " + column + "), \'FMMM/FMDD/yyyy\') " +
                        "|| \' - \' || " +
                        "to_char(date_trunc(\'month\', " + column + ") + interval \'1 month\', \'FMMM/FMDD/yyyy\') AS " + KEY_LABEL;
            case MONTHS:
                return "to_char(date_trunc(\'quarter\', " + column + "), \'FMMonth\') " +
                        "|| \' - \' || " +
                        "to_char(date_trunc(\'quarter\', " + column + ") + interval \'2 months\', \'FMMonth\') AS " + KEY_LABEL;
            case QUARTERS:
                return "to_char(date_trunc(\'year\', " + column + "), \'FMYYYY\') AS " + KEY_LABEL;
            case YEARS:
                return "to_char(date_trunc(\'year\', " + column + "), \'FMYYYY\') " +
                        "|| \' - \' || " +
                        "to_char(date_trunc(\'year\', " + column + ") + interval \'1 years\', \'FMYYYY\')" + " AS " + KEY_LABEL;
            default:
                throw new IllegalArgumentException("Invalid format, found: " + format);
        }
    }

    public static String getSelectionForDateFormat(GraphOutputDateFormat format, String column) {
        switch (format) {
            case DAYS:
                return getSelectionForDaysOfYear(column);
            case WEEKS:
                return getSelectionForWeeksOfYear(column);
            case MONTHS:
                return getSelectionForMonthsOfYear(column);
            case QUARTERS:
                return getSelectionForQuarters(column);
            case YEARS:
                return getSelectionForYears(column);
            default:
                throw new IllegalArgumentException("Invalid date format, found: " + format);
        }
    }

    /**
     * @param column         The table's date column from which the date will be extracted
     * @param startTime      The graph's start time in UNIX millis
     * @param endTime        The graph's end time in UNIX millis
     * @param format         The output format of the graph or null if it should be automatically calculated
     * @param filterWeekends True to filter out weekends, false otherwise
     * @return A pair containing the outputted date format, and a sql clause that is formatted for the given time span
     * (in UNIX epoch millis) that can be appended to a SELECT statement
     */
    public static Pair<GraphOutputDateFormat, String> getSelectionForCustomDate(String column, long startTime, long endTime,
                                                                                GraphOutputDateFormat format,
                                                                                boolean filterWeekends) {
        if (format == null || getNumberOfDataPointsBetween(startTime, endTime, filterWeekends, format) > MAX_DATA_POINTS) {
            format = getBestGraphOutputFormat(startTime, endTime, filterWeekends);
        }

        switch (format) {
            case DAYS:
                return new Pair<>(format, getSelectionForDaysOfYear(column));
            case WEEKS:
                return new Pair<>(format, getSelectionForWeeksOfYear(column));
            case MONTHS:
                return new Pair<>(format, getSelectionForMonthsOfYear(column));
            case QUARTERS:
                return new Pair<>(format, getSelectionForQuarters(column));
            case YEARS:
                return new Pair<>(format, getSelectionForYears(column));
            default:
                throw new RuntimeException("Invalid GraphOutputDateFormat, found: " + format);
        }
    }

    public static String getSelectionForDateFormatWithEpoch(GraphOutputDateFormat format, String column) {
        switch (format) {
            case DAYS:
                return getSelectionForDaysOfYearWithEpoch(column);
            case WEEKS:
                return getSelectionForWeeksOfYearWithEpoch(column);
            case MONTHS:
                return getSelectionForMonthsOfYearWithEpoch(column);
            case QUARTERS:
                return getSelectionForQuartersWithEpoch(column);
            case YEARS:
                return getSelectionForYearsWithEpoch(column);
            default:
                throw new IllegalArgumentException("Invalid date format, found: " + format);
        }
    }

    /**
     * @param column         The table's date column from which the date will be extracted
     * @param startTime      The graph's start time in UNIX millis
     * @param endTime        The graph's end time in UNIX millis
     * @param format         The output format of the graph or null if it should be automatically calculated
     * @param filterWeekends True to filter out weekends, false otherwise
     * @return A pair containing the outputted date format, and a sql clause that is formatted for the given time span
     * (in UNIX epoch millis) that can be appended to a SELECT statement
     */
    public static Pair<GraphOutputDateFormat, String> getSelectionForCustomDateWithEpoch(String column, long startTime, long endTime,
                                                                                         GraphOutputDateFormat format,
                                                                                         boolean filterWeekends) {
        if (format == null || getNumberOfDataPointsBetween(startTime, endTime, filterWeekends, format) > MAX_DATA_POINTS) {
            format = getBestGraphOutputFormat(startTime, endTime, filterWeekends);
        }

        switch (format) {
            case DAYS:
                return new Pair<>(format, getSelectionForDaysOfYearWithEpoch(column));
            case WEEKS:
                return new Pair<>(format, getSelectionForWeeksOfYearWithEpoch(column));
            case MONTHS:
                return new Pair<>(format, getSelectionForMonthsOfYearWithEpoch(column));
            case QUARTERS:
                return new Pair<>(format, getSelectionForQuartersWithEpoch(column));
            case YEARS:
                return new Pair<>(format, getSelectionForYearsWithEpoch(column));
            default:
                throw new RuntimeException("Invalid GraphOutputDateFormat, found: " + format);
        }
    }

    public static String getPredicateForDateFormat(GraphOutputDateFormat format, String column, boolean filterWeekends) {
        switch (format) {
            case DAYS:
                return getPredicateForWeek(column, true, filterWeekends);
            case WEEKS:
                return getPredicateForMonth(column, true, filterWeekends);
            case MONTHS:
                return getPredicateForQuarter(column, true, filterWeekends);
            case QUARTERS:
                return getPredicateForYear(column, true, filterWeekends);
            case YEARS:
                return getPredicateForYears(column, true, filterWeekends);
            default:
                throw new IllegalArgumentException("Invalid GraphOutputDateFormat, found: " + format);
        }
    }

    /**
     * @param column         The column that the date will be used for comparison
     * @param format         The desired (but not guaranteed) output format for the graph
     * @param startTime      The custom date's start time
     * @param endTime        The custom date's end time
     * @param filterWeekends True to filter out weekends, false otherwise
     * @return An empty optional if startTime > endTime, or a pair containing the outputted graph format and the
     * predicate otherwise to be used in a SQL where clause
     */
    public static Pair<GraphOutputDateFormat, String> getPredicateForCustomDate(String column, GraphOutputDateFormat format,
                                                                                long startTime, long endTime, boolean filterWeekends) {
        if (format == null || getNumberOfDataPointsBetween(startTime, endTime, filterWeekends, format) > MAX_DATA_POINTS) {
            format = getBestGraphOutputFormat(startTime, endTime, filterWeekends);
        }

        final boolean useCurrentDate = false;
        switch (format) {
            case DAYS:
                return new Pair<>(format, getPredicateForWeek(column, useCurrentDate, filterWeekends));
            case WEEKS:
                return new Pair<>(format, getPredicateForMonth(column, useCurrentDate, filterWeekends));
            case MONTHS:
                return new Pair<>(format, getPredicateForQuarter(column, useCurrentDate, filterWeekends));
            case QUARTERS:
                return new Pair<>(format, getPredicateForYear(column, useCurrentDate, filterWeekends));
            case YEARS:
                return new Pair<>(format, getPredicateForYears(column, useCurrentDate, filterWeekends));
            default:
                throw new RuntimeException("Invalid GraphOutputDateFormat, found: " + format);
        }

    }

    // Mark - Package-Private Methods

    interface GraphDataRetrieval<T> {

        T retrieveData(String graphTitle, ResultSet resultSet) throws SQLException;
    }

    static LineGraph getLineGraphFromResultSet(String title, ResultSet resultSet) throws SQLException {
        List<Pair<Long, Double>> coordinatePairList = new ArrayList<>();

        while (resultSet.next()) {
            long date = resultSet.getDate(KEY_DATA_X).getTime();
            double data = resultSet.getDouble(KEY_DATA_Y);

            coordinatePairList.add(new Pair<>(date, data));
        }

        return new LineGraph(title, coordinatePairList);
    }

    static CategorizedBarGraph getCategorizedBarGraphFromResultSet(String title, ResultSet resultSet) throws SQLException {
        return new CategorizedBarGraph(title, getCoordinatePairsFromResultSet(resultSet));
    }

    static BarGraph getBarGraphFromResultSet(String title, ResultSet resultSet) throws SQLException {
        return getGraphFromResultSet(title, resultSet, BarGraph::new);
    }

    static DoughnutGraph getDoughnutGraphFromResultSet(String title, ResultSet resultSet) throws SQLException {
        return getGraphFromResultSet(title, resultSet, DoughnutGraph::new);
    }

    <T> T getDataFromQuery(final String graphTitle, final String sql, final List<Object> statementParameters,
                           final GraphDataRetrieval<T> dataRetrieval) {
        return getDatabase().withConnection(connection -> {
            PreparedStatement statement = null;
            ResultSet resultSet = null;
            try {
                Logger.trace("Executing SQL: " + sql);
                statement = connection.prepareStatement(sql);

                for (int i = 0; i < statementParameters.size(); i++) {
                    Object o = statementParameters.get(i);
                    int offset = i + 1;
                    if (o instanceof String) {
                        statement.setString(offset, (String) o);
                    } else if (o instanceof Double) {
                        statement.setDouble(offset, (Double) o);
                    } else if (o instanceof Float) {
                        statement.setFloat(offset, (Float) o);
                    } else if (o instanceof Integer) {
                        statement.setInt(offset, (Integer) o);
                    } else if (o instanceof Long) {
                        statement.setLong(offset, (Long) o);
                    } else if (o instanceof java.sql.Date) {
                        statement.setDate(offset, (java.sql.Date) o);
                    } else if (o instanceof Timestamp) {
                        statement.setTimestamp(offset, (Timestamp) o);
                    } else if (o instanceof Byte) {
                        statement.setByte(offset, (Byte) o);
                    } else if (o instanceof Blob) {
                        statement.setBlob(offset, (Blob) o);
                    } else if (o instanceof Boolean) {
                        statement.setBoolean(offset, (Boolean) o);
                    } else {
                        String error = String.format("Invalid parameter type found for offset(%d): %s", offset, o.getClass().getSimpleName());
                        Logger.error(error, new IllegalArgumentException());
                    }
                }

                resultSet = statement.executeQuery();

                return dataRetrieval.retrieveData(graphTitle, resultSet);
            } catch (SQLException e) {
                e.printStackTrace();
                return null;
            } finally {
                closeConnections(statement, resultSet);
            }
        });
    }

    // MARK - Private Methods

    private static String getSelectionForDaysOfYear(String column) {
        return "to_char(" + column + ", \'FMMM/FMDD/yyyy\') AS " + KEY_LABEL;
    }

    private static String getSelectionForWeeksOfYear(String column) {
        return "to_char(date_trunc(\'week\', " + column + " ), \'\"Week of \"FMMM/FMDD\') AS " + KEY_LABEL;
    }

    private static String getSelectionForMonthsOfYear(String column) {
        return "to_char(date_trunc(\'month\', " + column + " ), \'Month\') AS " + KEY_LABEL;
    }

    private static String getSelectionForQuarters(String column) {
        return "CASE " +
                "  WHEN extract(QUARTER FROM date_trunc(\'quarter\', " + column + " )) = 1 THEN \'1st qr.\' " +
                "  WHEN extract(QUARTER FROM date_trunc(\'quarter\', " + column + " )) = 2 THEN \'2nd qr.\' " +
                "  WHEN extract(QUARTER FROM date_trunc(\'quarter\', " + column + " )) = 3 THEN \'3rd qr.\' " +
                "  WHEN extract(QUARTER FROM date_trunc(\'quarter\', " + column + " )) = 4 THEN \'4th qr.\' " +
                "  END AS " + KEY_LABEL;
    }

    private static String getSelectionForYears(String column) {
        return "to_char(date_trunc(\'year\', " + column + " ), \'YYYY\') AS " + KEY_LABEL;
    }

    private static String getSelectionForDaysOfYearWithEpoch(String column) {
        return "extract(EPOCH FROM " + column + ") * 1000 AS " + KEY_DATA_X;
    }

    private static String getSelectionForWeeksOfYearWithEpoch(String column) {
        return "extract(EPOCH FROM date_trunc(\'week\', " + column + " )) * 1000 AS " + KEY_DATA_X;
    }

    private static String getSelectionForMonthsOfYearWithEpoch(String column) {
        return "extract(EPOCH FROM date_trunc(\'month\', " + column + " )) * 1000 AS " + KEY_DATA_X;
    }

    private static String getSelectionForQuartersWithEpoch(String column) {
        return "extract(EPOCH FROM date_trunc(\'quarter\', " + column + " )) * 1000 AS " + KEY_DATA_X;
    }

    private static String getSelectionForYearsWithEpoch(String column) {
        return "to_char(date_trunc(\'year\', " + column + " ), \'YYYY\') AS " + KEY_DATA_X;
    }

    /**
     * @param column               The column that the date will be used for comparison
     * @param useCurrentDate       True to set the truncated date to the current date or false to keep it confined
     *                             between a start and end value.
     * @param shouldFilterWeekends True to filter out weekends, false otherwise
     * @return A date_trunc predicate that can be applied to the WHERE clause in a SQL query.
     */
    private static String getPredicateForDay(String column, boolean useCurrentDate, boolean shouldFilterWeekends) {
        return getPredicateForValue(column, "day", useCurrentDate, shouldFilterWeekends);
    }

    /**
     * @param column               The column that the date will be used for comparison
     * @param useCurrentDate       True to set the truncated date to the current date or false to keep it confined
     *                             between a start and end value.
     * @param shouldFilterWeekends True to filter out weekends, false otherwise
     * @return A date_trunc predicate that can be applied to the WHERE clause in a SQL query.
     */
    private static String getPredicateForWeek(String column, boolean useCurrentDate, boolean shouldFilterWeekends) {
        return getPredicateForValue(column, "week", useCurrentDate, shouldFilterWeekends);
    }

    /**
     * @param column               The column that the date will be used for comparison
     * @param useCurrentDate       True to set the truncated date to the current date or false to keep it confined
     *                             between a start and end value.
     * @param shouldFilterWeekends True to filter out weekends, false otherwise
     * @return A date_trunc predicate that can be applied to the WHERE clause in a SQL query.
     */
    private static String getPredicateForMonth(String column, boolean useCurrentDate, boolean shouldFilterWeekends) {
        return getPredicateForValue(column, "month", useCurrentDate, shouldFilterWeekends);
    }

    /**
     * @param column               The column that the date will be used for comparison
     * @param useCurrentDate       True to set the truncated date to the current date or false to keep it confined
     *                             between a start and end value.
     * @param shouldFilterWeekends True to filter out weekends, false otherwise
     * @return A date_trunc predicate that can be applied to the WHERE clause in a SQL query.
     */
    private static String getPredicateForQuarter(String column, boolean useCurrentDate, boolean shouldFilterWeekends) {
        return getPredicateForValue(column, "quarter", useCurrentDate, shouldFilterWeekends);
    }

    /**
     * @param column               The column that the date will be used for comparison
     * @param useCurrentDate       True to set the truncated date to the current date or false to keep it confined
     *                             between a start and end value.
     * @param shouldFilterWeekends True to filter out weekends, false otherwise
     * @return A date_trunc predicate that can be applied to the WHERE clause in a SQL query.
     */
    private static String getPredicateForYear(String column, boolean useCurrentDate, boolean shouldFilterWeekends) {
        return getPredicateForValue(column, "year", useCurrentDate, shouldFilterWeekends);
    }

    /**
     * @param column         The column that the date will be used for comparison
     * @param useCurrentDate True to set the truncated date to the current date or false to keep it confined
     *                       between a start and end value.
     * @param filterWeekends True to filter out weekends, false otherwise
     * @return A predicate that can be applied to the WHERE clause in a SQL query.
     */
    private static String getPredicateForYears(String column, boolean useCurrentDate, boolean filterWeekends) {
        String predicate;
        if (!useCurrentDate) {
            predicate = column + " <= ? AND " + column + " >= ? ";
        } else {
            predicate = column + " <= current_date ";
        }

        if (filterWeekends) {
            predicate += " AND " + getPredicateForFilterWeekends(column) + " ";
        }

        return predicate;
    }


    private static String getPredicateForValue(String column, String truncateValue, boolean useCurrentDate, boolean filterWeekends) {
        String predicate;
        if (!useCurrentDate) {
            predicate = " date_trunc(\'" + truncateValue + "\', " + column + " ) >= date_trunc(\'" + truncateValue + "\', ?::date) " +
                    "AND date_trunc(\'" + truncateValue + "\', " + column + " ) <= date_trunc(\'" + truncateValue + "\', ?::date) ";
        } else {
            predicate = " date_trunc(\'" + truncateValue + "\', " + column + " ) = date_trunc(\'" + truncateValue + "\', current_date) ";
        }

        if (filterWeekends) {
            predicate += " AND " + getPredicateForFilterWeekends(column);
        }

        return predicate;
    }

    private static String getPredicateForFilterWeekends(String column) {
        return " extract(DOW FROM " + column + " ) NOT IN (0, 6) ";
    }

    private static GraphOutputDateFormat getBestGraphOutputFormat(long startTime, long endTime,
                                                                  boolean shouldFilterWeekends) {
        ReadableInstant start = new DateTime(startTime);
        ReadableInstant end = new DateTime(endTime);

        double daysBetween = Days.daysBetween(start, end)
                .getDays();

        if (shouldFilterWeekends) {
            daysBetween = daysBetween * 5 / 7;
        }

        if (daysBetween <= DAYS_IN_WEEK) {
            return GraphOutputDateFormat.DAYS;
        } else if (daysBetween < (DAYS_IN_WEEK * WEEKS_IN_MONTH) + 1) {
            // If it's less than 31 days
            return GraphOutputDateFormat.WEEKS;
        } else if (daysBetween < DAYS_IN_WEEK * WEEKS_IN_MONTH * MONTHS_IN_QUARTER) {
            return GraphOutputDateFormat.MONTHS;
        } else if (daysBetween < DAYS_IN_WEEK * WEEKS_IN_MONTH * MONTHS_IN_QUARTER * QUARTERS_IN_YEAR) {
            return GraphOutputDateFormat.QUARTERS;
        } else {
            return GraphOutputDateFormat.YEARS;
        }
    }

    private static int getNumberOfDataPointsBetween(long startTime, long endTime, boolean filterWeekends, GraphOutputDateFormat format) {
        double daysBetween = Days.daysBetween(new DateTime(startTime), new DateTime(endTime))
                .getDays();

        int unitsBetween;
        switch (format) {
            case DAYS:
                unitsBetween = (int) daysBetween;
                break;
            case WEEKS:
                unitsBetween = (int) Math.ceil(daysBetween / DAYS_IN_WEEK);
                break;
            case MONTHS:
                unitsBetween = (int) Math.ceil(daysBetween / (DAYS_IN_WEEK * WEEKS_IN_MONTH));
                break;
            case QUARTERS:
                unitsBetween = (int) Math.ceil(daysBetween / (DAYS_IN_WEEK * WEEKS_IN_MONTH * MONTHS_IN_QUARTER));
                break;
            case YEARS:
                unitsBetween = (int) Math.ceil(daysBetween / DAYS_IN_YEAR);
                break;
            default:
                throw new IllegalArgumentException("Invalid GraphOutputDateFormat, found: " + format.toString());
        }

        if (filterWeekends) {
            unitsBetween = unitsBetween * 5 / 7;
        }

        return unitsBetween;
    }

    private static <T extends GraphStatistic> T getGraphFromResultSet(String graphTitle, ResultSet resultSet,
                                                                      GraphStatisticRetrievalFunction<T> constructor) throws SQLException {
        return constructor.construct(graphTitle, getCoordinatePairsFromResultSet(resultSet));
    }

    private static List<Pair<String, Double>> getCoordinatePairsFromResultSet(ResultSet resultSet) throws SQLException {
        List<Pair<String, Double>> coordinatePairList = new ArrayList<>();

        while (resultSet.next()) {
            String label = resultSet.getString(KEY_LABEL);
            double data = resultSet.getDouble(KEY_DATA);

            coordinatePairList.add(new Pair<>(label, data));
        }

        return coordinatePairList;
    }

    private interface GraphStatisticRetrievalFunction<T extends GraphStatistic> {

        T construct(String graphTitle, List<Pair<String, Double>> coordinatePairs);
    }

}
