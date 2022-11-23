https://powcoder.com
代写代考加微信 powcoder
Assignment Project Exam Help
Add WeChat powcoder
package model;

import com.fasterxml.jackson.databind.node.ObjectNode;
import play.libs.Json;

/**
 * Created by Corey on 3/14/2017.
 * Project: Magic List Maker - Server
 * <p></p>
 * Purpose of Class:
 */
public class ImporterLimit {

    private static final String KEY_DAILY_TOTAL = "daily_total";
    private static final String KEY_MONTHLY_TOTAL = "monthly_total";
    private static final String KEY_DAILY_MAX = "daily_max";
    private static final String KEY_MONTHLY_MAX = "monthly_max";

    private final int dailyTotal;
    private final int monthlyTotal;
    private final int dailyMax;
    private final int monthlyMax;

    public ImporterLimit(int dailyTotal, int monthlyTotal, int dailyMax,
                         int monthlyMax) {
        this.dailyTotal = dailyTotal;
        this.monthlyTotal = monthlyTotal;
        this.dailyMax = dailyMax;
        this.monthlyMax = monthlyMax;
    }

    public int getDailyTotal() {
        return dailyTotal;
    }

    public int getMonthlyTotal() {
        return monthlyTotal;
    }

    public int getDailyMax() {
        return dailyMax;
    }

    public int getMonthlyMax() {
        return monthlyMax;
    }

    public static class Converter implements JsonConverter<ImporterLimit> {

        @Override
        public ObjectNode renderAsJsonObject(ImporterLimit object) {
            return Json.newObject()
                    .put(KEY_DAILY_TOTAL, object.dailyTotal)
                    .put(KEY_MONTHLY_TOTAL, object.monthlyTotal)
                    .put(KEY_DAILY_MAX, object.dailyMax)
                    .put(KEY_MONTHLY_MAX, object.monthlyMax);
        }

        @Override
        public ImporterLimit deserializeFromJson(ObjectNode objectNode) {
            int dailyTotal = objectNode.get(KEY_DAILY_TOTAL).asInt();
            int monthlyTotal = objectNode.get(KEY_MONTHLY_TOTAL).asInt();
            int dailyMax = objectNode.get(KEY_DAILY_MAX).asInt();
            int monthlyMax = objectNode.get(KEY_MONTHLY_MAX).asInt();
            return new ImporterLimit(dailyTotal, monthlyTotal, dailyMax, monthlyMax);
        }

    }

}
