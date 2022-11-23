https://powcoder.com
代写代考加微信 powcoder
Assignment Project Exam Help
Add WeChat powcoder
package utilities;

import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.Before;
import org.junit.Test;
import play.libs.Json;

import static org.junit.Assert.*;

/**
 * Created by Corey Caplan on 10/9/16.
 */
public class ValidationTest {

    private ObjectNode objectNode;

    @Before
    public void setUp() throws Exception {
        objectNode = Json.newObject();
        objectNode.put("test_1", 123);
        objectNode.put("test_2", "123");
        objectNode.put("test_3", "Hello World!");
        objectNode.put("test_4", "");
        objectNode.put("test_5", "null");
        objectNode.put("test_6", (String) null);
    }

    @Test
    public void intFromJson() throws Exception {
        int result;

        result = Validation.integer("test_1", objectNode);
        assertEquals(result, 123);

        result = Validation.integer("test_2", objectNode);
        assertEquals(result, 123);

        result = Validation.integer("test_3", objectNode);
        assertEquals(result, -1);

        result = Validation.integer("test_4", objectNode);
        assertEquals(result, -1);

        result = Validation.integer("test_5", objectNode);
        assertEquals(result, -1);

        result = Validation.integer("test_6", objectNode);
        assertEquals(result, -1);
    }

    @Test
    public void stringFromJson() throws Exception {
        String result;

        result = Validation.string("test_1", objectNode);
        assertNotEquals(result, null);

        result = Validation.string("test_2", objectNode);
        assertEquals(result, "123");

        result = Validation.string("test_3", objectNode);
        assertEquals(result, "Hello World!");

        result = Validation.string("test_4", objectNode);
        assertEquals(result, null);

        result = Validation.string("test_5", objectNode);
        assertNotEquals(result, null);
        assertEquals(result, "null");

        result = Validation.string("test_6", objectNode);
        assertEquals(result, null);

    }

    @Test
    public void getLong() throws Exception {
        assertEquals(123, Validation.getLong("test_1", objectNode));
    }

}