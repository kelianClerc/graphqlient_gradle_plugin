package com.applidium.qlrequest;

import com.applidium.qlrequest.Query.QLVariables;

import org.junit.Test;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertTrue;

public class QLVariablesTest {

    @Test
    public void testInit() throws Exception {
        QLVariables variables = new QLVariables();
        assertEquals(variables.getVariables(), Collections.emptyMap());
    }

    @Test
    public void testPrint() throws Exception {
        Map<String, Object> params = new HashMap<>();
        params.put("postId", 1);
        params.put("postName", "Hello");
        params.put("postLikeAverage", 1.12);
        params.put("postIsVisible", true);

        QLVariables variables = new QLVariables(params);
        String print = variables.print();
        System.out.println(print);
        assertTrue(print.contains("\"postId\":1"));
        assertTrue(print.contains("\"postName\":\"Hello\""));
        assertTrue(print.contains("\"postLikeAverage\":1.12"));
        assertTrue(print.contains("\"postIsVisible\":true"));
        //String regex = "[{(\"[a-zA-Z0-9]+\":[^,]+,)+(\"[a-zA-Z]+\":[^,]+)}]";
        //assertTrue(print.matches(regex));
    }
}
