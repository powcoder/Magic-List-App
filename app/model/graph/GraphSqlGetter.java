https://powcoder.com
代写代考加微信 powcoder
Assignment Project Exam Help
Add WeChat powcoder
package model.graph;

import akka.japi.Pair;
import utilities.DateUtility;

import java.util.Calendar;
import java.util.Date;
import java.util.List;

import static database.GraphDBAccessor.*;

/**
 * Created by Corey on 6/30/2017.
 * Project: magic_list_maker-server
 * <p></p>
 * Purpose of Class:
 */

/**
 * Class that chooses to create a selection/predicate clause based on a custom date (if available) or current time
 * otherwise.
 */
public class GraphSqlGetter {

    private GraphOutputDateFormat format;
    private final Pair<Date, Date> startEndDatePair;
    private final boolean filterWeekends;

    public GraphSqlGetter(GraphOutputDateFormat format, Pair<Date, Date> startEndDatePair,
                          boolean filterWeekends) {
        this.format = format;
        this.startEndDatePair = startEndDatePair;
        this.filterWeekends = filterWeekends;
    }

    public String getSelectionStatementForRange(String column) {
        if (startEndDatePair != null) {
            long startTime = this.startEndDatePair.first().getTime();
            long endTime = this.startEndDatePair.second().getTime();
            Pair<String, String> startEndDatePair = new Pair<>(DateUtility.getDateForSql(startTime), DateUtility.getDateForSql(endTime));
            Pair<GraphOutputDateFormat, String> formatDatePair =
                    convertGraphOutputDateFormatToRangeAsLabel(startEndDatePair, startTime, endTime, filterWeekends, format);
            format = formatDatePair.first();
            return formatDatePair.second();
        } else {
            return convertGraphOutputDateFormatToRangeAsLabel(column, format);
        }
    }

    public String getSelectionStatement(String column, boolean isEpochDate) {
        if (startEndDatePair != null) {
            long startDate = startEndDatePair.first().getTime();
            long endDate = startEndDatePair.second().getTime();
            Pair<GraphOutputDateFormat, String> formatSqlPair;
            if (isEpochDate) {
                formatSqlPair = getSelectionForCustomDateWithEpoch(column, startDate, endDate, format, filterWeekends);
            } else {
                formatSqlPair = getSelectionForCustomDate(column, startDate, endDate, format, filterWeekends);
            }
            this.format = formatSqlPair.first();
            return formatSqlPair.second();
        } else {
            if (isEpochDate) {
                return getSelectionForDateFormatWithEpoch(format, column);
            } else {
                return getSelectionForDateFormat(format, column);
            }
        }
    }

    public String getPredicateStatement(String column) {
        if (startEndDatePair != null) {
            long startDate = startEndDatePair.first().getTime();
            long endDate = startEndDatePair.second().getTime();
            return getPredicateForCustomDate(column, format, startDate, endDate, filterWeekends).second();
        } else {
            return getPredicateForDateFormat(format, column, filterWeekends);
        }
    }

    public void addParametersForCustomDateIfNecessary(List<Object> parameters) {
        if (startEndDatePair != null) {
            parameters.add(new java.sql.Date(startEndDatePair.first().getTime()));
            parameters.add(new java.sql.Date(startEndDatePair.second().getTime()));
        }
    }

}