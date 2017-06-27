package com.applidium.qlrequest;

import com.applidium.qlrequest.Query.QLQuery;
import com.applidium.qlrequest.Query.QLType;
import com.applidium.qlrequest.Query.QLVariablesElement;
import com.applidium.qlrequest.Tree.QLParser;
import com.applidium.qlrequest.annotations.AliasFor;
import com.applidium.qlrequest.annotations.Argument;
import com.applidium.qlrequest.annotations.Parameters;
import com.applidium.qlrequest.exceptions.QLException;
import com.applidium.qlrequest.model.QLModel;
import com.applidium.qlrequest.Tree.QLElement;
import com.applidium.qlrequest.Tree.QLLeaf;
import com.applidium.qlrequest.Tree.QLNode;
import com.applidium.qlrequest.Tree.QLTreeBuilder;

import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertTrue;
import static junit.framework.Assert.fail;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.junit.Assert.assertThat;

public class QLQueryTest {

    private QLNode rootNode;

    @Before
    public void setUp() throws Exception {
        QLNode rootNode = new QLNode("1");
        QLLeaf firstChild = new QLLeaf("2");
        QLNode secondChild = new QLNode("3");
        QLLeaf thridChild = new QLLeaf("4");

        secondChild.addChild(thridChild);
        rootNode.addChild(firstChild);
        rootNode.addChild(secondChild);
        this.rootNode = rootNode;
    }

    @Test public void treeConstruction() {
        QLQuery query = new QLQuery();
        query.append(rootNode);

        List<QLElement> getRootElement = query.getQueryFields();
        assertEquals(1, 1);
        assertEquals(getRootElement.size(), 1);

        assertEquals(((QLNode)getRootElement.get(0)).getElementInfo(), "1");
        List<QLElement> rootChildren = ((QLNode)getRootElement.get(0)).getChildren();
        assertEquals(rootChildren.size(), 2);
        assertEquals(rootChildren.get(0).print(), "2");
        assertThat(rootChildren.get(1), instanceOf(QLNode.class));
        QLNode node = (QLNode) rootChildren.get(1);
        assertEquals(node.getElementInfo(), "3");
        List<QLElement> secondeChildren = node.getChildren();
        assertEquals(secondeChildren.size(), 1);
        assertEquals(secondeChildren.get(0).print(), "4");
    }

    @Test public void treePrint() {
        QLQuery query = new QLQuery();
        query.append(rootNode);
        assertEquals(query.printQuery(), "{1{2,3{4}}}");
    }

    @Test public void treePrintWithName() {
        String aRandomName = "aRandomName";
        QLQuery query = new QLQuery(aRandomName);
        query.append(rootNode);
        assertEquals(query.printQuery(), "query " + aRandomName +"{1{2,3{4}}}");
    }

    @Test
    public void queryParamsTest() throws Exception {

        String aRandomName = "aRandomName";
        String aRandomName2 = "aRandomName2";
        QLQuery query = new QLQuery(aRandomName);
        QLVariablesElement param = new QLVariablesElement("test", QLType.STRING, true);
        QLVariablesElement param2 = new QLVariablesElement("test2", QLType.INT);
        query.setParameters(Arrays.asList(param, param2));
        query.append(rootNode);

        assertEquals(query.printQuery(), "query " + aRandomName +"($test:String!,$test2:Int){1{2,3{4}}}");


        QLQuery query1 = new QLQuery(aRandomName, Arrays.asList(param, param2));
        List<QLElement> fields = new ArrayList<>();
        fields.add(rootNode);
        query1.setQueryFields(fields);
        query1.setName(aRandomName2);
        assertEquals(query1.printQuery(), "query " + aRandomName2 +"($test:String!,$test2:Int){1{2,3{4}}}");


        Map<String,Object> map = new HashMap<>();
        map.put("id", new QLVariablesElement("test"));
        QLNode node = new QLNode(rootNode.getElement());
        node.setParameters(map);
        node.addAllChild(rootNode.getChildren());
        QLQuery query2 = new QLQuery(aRandomName, Arrays.asList(param, param2));
        fields.clear();
        fields.add(node);
        query2.setQueryFields(fields);
        query2.setName(aRandomName2);
        assertEquals(query2.printQuery(), "query " + aRandomName2 +"($test:String!,$test2:Int){1(id:$test){2,3{4}}}");

    }

    @Test
    public void queryFromObjectTest() throws Exception {
        QLQuery qlQuery = new QLQuery("name");
        QueryTest queryTest = new QueryTest();
        qlQuery.append(queryTest.getUser());
        assertEquals(qlQuery.printQuery(), "query name{user(id:\"1\"){id,name,essai:email,posts{id,title}}}");
    }

    private class QueryTest implements QLModel {
        @Parameters(table={
            @Argument(argumentName = "id", argumentValue = "1")
        })
        private UserTest user;
        private List<UserTest> users;
        private QLTreeBuilder treeBuilder = new QLTreeBuilder();

        public QueryTest() {
        }

        public QLNode getUser() {
            try {
                return treeBuilder.createNodeFromField(getClass().getDeclaredField("user"));
            } catch (NoSuchFieldException e) {
                e.printStackTrace();
            }
            return null;
        }
    }

    @Test
    public void addVariableTest() throws Exception {
        QLParser parser = new QLParser();
        parser.setToParse("query hello($try: Boolean!, $try2:String, $try3:Int, $try4:Float, $try5:ID!) {user{id}}");
        QLQuery qlQuery = parser.buildQuery();
        assertEquals(qlQuery.getParameters().getParams().size(), 5);

        assertTrue(qlQuery.isVariableEmpty());

        int numberOfVariables = qlQuery.getVariables().getVariables().size();
        try {
            qlQuery.addVariable("randomString", 2);
            fail("QLException should have been thrown");
        } catch (QLException e) {
            assertTrue(e.getMessage().startsWith("The variable being added : \"randomString\" is " +
                "not present in the query parameter list : ["));
        }
        assertEquals(qlQuery.getVariables().getVariables().size(), numberOfVariables);
        qlQuery.addVariable("try3", 2);
        assertEquals(qlQuery.getVariables().getVariables().size(), numberOfVariables + 1);
        assertEquals(qlQuery.areAllParametersGiven(), false);
        assertEquals(qlQuery.isVariableEmpty(), false);
        qlQuery.addVariable("try", 2);
        assertEquals(qlQuery.getVariables().getVariables().size(), numberOfVariables + 2);
        qlQuery.addVariable("try5", 2);
        assertEquals(qlQuery.getVariables().getVariables().size(), numberOfVariables + 3);
        assertEquals(qlQuery.areAllParametersGiven(), true);

    }

    private class UserTest implements QLModel {
        private String id;
        private String name;
        @AliasFor(name = "email") private String essai;
        private PostTest posts;
    }

    private class PostTest implements QLModel {
        private String id;
        private String title;
    }
}
