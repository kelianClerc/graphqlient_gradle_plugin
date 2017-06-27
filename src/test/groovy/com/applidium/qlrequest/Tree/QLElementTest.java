package com.applidium.qlrequest.Tree;

import com.applidium.qlrequest.Query.QLVariablesElement;

import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

import static junit.framework.Assert.assertEquals;
import static junit.framework.TestCase.assertTrue;

public class QLElementTest {
    @Test
    public void elementOnlyNamed() throws Exception {
        String name = "name";
        QLElement element = new QLElement(name);
        assertEquals(element.print(), name);
    }

    @Test
    public void elementWithAlias() throws Exception {
        String name = "name";
        String alias = "alias";
        QLElement element = new QLElement(name, alias);
        assertEquals(element.print(), alias + ":" + name);

    }

    @Test
    public void elementWithParams() throws Exception {
        String name = "name";
        String key = "id";
        String key2 = "id2";
        String key3 = "id3";
        int value = 1;
        String value2 = "2";
        String name3 = "name";
        QLVariablesElement value3 = new QLVariablesElement(name3);
        HashMap<String, Object> params = new HashMap<>();
        params.put(key, value);
        QLElement element = new QLElement(name, params);
        assertEquals(element.print(), name + "(" + key + ":" + value +")");


        HashMap<String, Object> params2 = new HashMap<>();
        params2.putAll(params);
        params2.put(key2, value2);
        QLElement element2 = new QLElement(name, params2);
        assertTrue(element2.print().contains(name + "("));
        assertTrue(element2.print().contains(key2 + ":\"" + value2 + "\""));
        assertTrue(element2.print().contains(key + ":" + value));

        HashMap<String, Object> params3 = new HashMap<>();
        params3.putAll(params2);
        params3.put(key3, value3);
        QLElement element3 = new QLElement(name, params3);

        assertTrue(element3.print().contains(name + "("));
        assertTrue(element3.print().contains(key2 + ":\"" + value2 + "\""));
        assertTrue(element3.print().contains(key + ":" + value));
        assertTrue(element3.print().contains(key3 + ":" + value3.printVariableName()));

    }

    @Test
    public void elementWithParamsAndAlias() throws Exception {
        String name = "name";
        String alias = "alias";
        String key = "id";
        int value = 1;
        HashMap<String, Object> params = new HashMap<>();
        params.put(key, value);
        QLElement element = new QLElement(name, alias, params);
        assertEquals(element.print(), alias + ":" + name + "(" + key + ":" + value +")");
    }

    @Test
    public void testNodeConstructor() throws Exception {
        QLElement element = new QLElement("name");
        Map<String, Object> test = new HashMap<>();
        test.put("testA", "value");
        QLNode a = new QLNode(element);
        assertEquals(a.getName(), "name");
        QLNode b = new QLNode("name");
        assertEquals(b.getName(), "name");
        QLNode c = new QLNode("name", "alias");
        assertEquals(c.getName(), "name");
        assertEquals(c.getAlias(), "alias");
        QLNode d = new QLNode("name", test);
        assertEquals(d.getName(), "name");
        assertEquals(d.getParameters().size(), 1);
        QLNode e = new QLNode("name", "alias", test);
        assertEquals(e.getName(), "name");
        assertEquals(e.getAlias(), "alias");
        assertEquals(e.getParameters().size(), 1);
        QLLeaf child = new QLLeaf("test");
        e.addChild(child);
        assertEquals(e.getChildren().size(), 1);
        e.removeChild(child);
        assertEquals(e.getChildren().size(), 0);
    }

    @Test
    public void testLeafConstructor() throws Exception {
        Map<String, Object> test = new HashMap<>();
        test.put("testA", "value");
        QLElement element = new QLElement("name");
        QLLeaf a = new QLLeaf(element);
        assertEquals(a.getName(), "name");
        QLLeaf b = new QLLeaf("name");
        assertEquals(b.getName(), "name");
        QLLeaf c = new QLLeaf("name", "alias");
        assertEquals(c.getName(), "name");
        assertEquals(c.getAlias(), "alias");
        QLLeaf d = new QLLeaf("name", test);
        assertEquals(d.getName(), "name");
        assertEquals(d.getParameters().size(), 1);
        QLLeaf e = new QLLeaf("name", "alias", test);
        assertEquals(e.getName(), "name");
        assertEquals(e.getAlias(), "alias");
        assertEquals(e.getParameters().size(), 1);
    }
}
