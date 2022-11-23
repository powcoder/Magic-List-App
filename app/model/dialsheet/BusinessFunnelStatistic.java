https://powcoder.com
代写代考加微信 powcoder
Assignment Project Exam Help
Add WeChat powcoder
package model.dialsheet;

import java.text.NumberFormat;

/**
 * Created by Corey Caplan on 9/12/17.
 */
public class BusinessFunnelStatistic {

    private final String category;
    private final int actual;
    private final int target;
    private final double actualRatio;
    private final double targetRatio;
    private final boolean isInverse;

    /**
     * @param category    The UI text that represents this category
     * @param actual      The actual number
     * @param target      The target number
     * @param actualRatio The actual ratio
     * @param targetRatio The target ratio
     * @param isInverse   True if the actual # should be below the target #; false otherwise
     */
    public BusinessFunnelStatistic(String category, int actual, int target, double actualRatio, double targetRatio,
                                   boolean isInverse) {
        this.category = category;
        this.actual = actual;
        this.target = target;
        this.actualRatio = actualRatio;
        this.targetRatio = targetRatio;
        this.isInverse = isInverse;
    }

    public String getCategory() {
        return category;
    }

    public int getActual() {
        return actual;
    }

    public int getTarget() {
        return target;
    }

    public String getActualRatio() {
        return NumberFormat.getPercentInstance().format(actualRatio);
    }

    public String getTargetRatio() {
        return NumberFormat.getPercentInstance().format(targetRatio);
    }

    public double getActualRatioLiteral() {
        return actualRatio;
    }

    public double getTargetRatioLiteral() {
        return targetRatio;
    }

    public boolean isInverse() {
        return isInverse;
    }
}
