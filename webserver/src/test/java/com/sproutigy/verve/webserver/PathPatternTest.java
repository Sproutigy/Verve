package com.sproutigy.verve.webserver;

import org.junit.Test;

import java.util.Collections;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public class PathPatternTest {

    @Test
    public void testSingleParam() {
        PathPattern pathPattern = new PathPattern("/article/:id");
        assertEquals(Collections.singletonMap("id", "128"), pathPattern.matchParams("/article/128"));
        assertNull(pathPattern.matchParams("/article/128/helloWorld"));
    }

    @Test
    public void testMultipleSingleParams() {
        PathPattern pathPattern = new PathPattern("/:section/:id");

        Map params = pathPattern.matchParams("/sport/555");
        assertEquals("sport", params.get("section"));
        assertEquals("555", params.get("id"));

        assertNull(pathPattern.matchParams("/hello"));
    }

    @Test
    public void testSingleGreedyParam() {
        PathPattern pathPattern = new PathPattern("/article/*id");
        assertEquals(pathPattern.matchParams("/article/128"), Collections.singletonMap("id", "128"));
        assertEquals(pathPattern.matchParams("/article/128/helloWorld"), Collections.singletonMap("id", "128/helloWorld"));

        assertNull(pathPattern.matchParams("/hello"));
    }

    @Test
    public void testMultipleGreedyParams() {
        PathPattern pathPattern = new PathPattern("/*path/*name");

        Map params = pathPattern.matchParams("/world/europe/poland/Warsaw");
        assertEquals(params.get("path"), "world/europe/poland");
        assertEquals(params.get("name"), "Warsaw");

        assertNull(pathPattern.matchParams("/hello"));
    }
}
