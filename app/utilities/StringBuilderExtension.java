https://powcoder.com
代写代考加微信 powcoder
Assignment Project Exam Help
Add WeChat powcoder
package utilities;

/**
 *
 */
public class StringBuilderExtension {

    private final StringBuilder builder;

    public StringBuilderExtension() {
        builder = new StringBuilder();
    }

    public StringBuilderExtension append(String s) {
        builder.append(s);
        return this;
    }

    public StringBuilderExtension append(int i) {
        builder.append(i);
        return this;
    }

    public StringBuilderExtension append(boolean b) {
        builder.append(b);
        return this;
    }

    public StringBuilderExtension appendTableWithQualifier(String table, String qualifier) {
        builder.append(table).append(".").append(qualifier);
        return this;
    }

    @Override
    public String toString() {
        return builder.toString();
    }
}
