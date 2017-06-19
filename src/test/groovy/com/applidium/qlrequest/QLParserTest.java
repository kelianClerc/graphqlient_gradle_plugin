package com.applidium.qlrequest;

import com.applidium.qlrequest.Query.QLFragment;
import com.applidium.qlrequest.Query.QLQuery;
import com.applidium.qlrequest.Query.QLType;
import com.applidium.qlrequest.Query.QLVariablesElement;
import com.applidium.qlrequest.Tree.QLElement;
import com.applidium.qlrequest.Tree.QLFragmentNode;
import com.applidium.qlrequest.Tree.QLLeaf;
import com.applidium.qlrequest.Tree.QLNode;
import com.applidium.qlrequest.Tree.QLParser;
import com.applidium.qlrequest.exceptions.QLParserException;

import org.junit.Test;

import java.util.List;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertTrue;
import static junit.framework.Assert.fail;
import static org.hamcrest.core.IsInstanceOf.instanceOf;
import static org.junit.Assert.assertThat;

public class QLParserTest {

    @Test
    public void initClass() throws Exception {
        QLParser parser = new QLParser();
        try {
            assertEquals(parser.buildQuery(), null);
            fail("QLParserException should have been thrown");
        } catch (QLParserException e) {
            assertEquals(e.getMessage(), "No string provided to be parsed");
        }

        QLParser parser2 = new QLParser("string");
        try {
            assertEquals(parser2.buildQuery().getQueryFields().size(), 0);
            fail("QLParserException should have been thrown");
        } catch (QLParserException e) {
            assertEquals(e.getMessage(), "No block found in the string provided : \"string\", " +
                "cannot create QLQuery");
        }
    }

    @Test
    public void checkQueryHeader() throws Exception {
        QLParser parser = new QLParser();

        parser.setToParse("query hello {}");
        QLQuery query = parser.buildQuery();
        assertEquals(query.getName(), "hello");

        QLParser parser2 = new QLParser();
        parser2.setToParse("{}");
        QLQuery response2 = parser2.buildQuery();
        assertEquals(response2.getName(), null);
    }

    @Test
    public void checkQueryHeaderTest() throws Exception {
        QLParser parser = new QLParser();

        parser.setToParse("query hello {}");
        QLQuery query = parser.buildQuery();
        assertEquals(query.getName(), "hello");

        QLParser parser2 = new QLParser();
        parser2.setToParse("{}");
        QLQuery response2 = parser2.buildQuery();
        assertEquals(response2.getName(), null);


        QLParser parser3 = new QLParser();
        parser3.setToParse("query test($try: Boolean!){}");
        QLQuery response3 = parser3.buildQuery();
        assertEquals(response3.getName(), "test");
        assertEquals(response3.getParameters().getParams().size(), 1);
        assertEquals(response3.getParameters().getParams().get(0).getName(), "try");
        assertEquals(response3.getParameters().getParams().get(0).getType(), QLType.BOOLEAN);
    }

    @Test
    public void checkEndpoint() throws Exception {
        QLParser parser = new QLParser();

        parser.setToParse("query hello {user {}}");
        QLQuery query = parser.buildQuery();
        assertEquals(query.getName(), "hello");
        assertEquals(query.getQueryFields().size(), 1);
        assertEquals(query.getQueryFields().get(0).getName(), "user");


        parser.setToParse("query hello {test : user {}}");
        QLQuery query2 = parser.buildQuery();
        assertEquals(query2.getName(), "hello");
        assertEquals(query2.getQueryFields().size(), 1);
        assertEquals(query2.getQueryFields().get(0).getName(), "user");
        assertEquals(query2.getQueryFields().get(0).getAlias(), "test");

        parser.setToParse("query hello {test : user(id:\"12f\") {}}");
        QLQuery query3 = parser.buildQuery();
        assertEquals(query3.getName(), "hello");
        assertEquals(query3.getQueryFields().size(), 1);
        assertEquals(query3.getQueryFields().get(0).getName(), "user");
        assertEquals(query3.getQueryFields().get(0).getAlias(), "test");
        assertEquals(query3.getQueryFields().get(0).getParameters().size(), 1);
        assertTrue(query3.getQueryFields().get(0).getParameters().containsKey("id"));
        assertEquals(query3.getQueryFields().get(0).getParameters().get("id"), "12f");

        parser.setToParse("query hello($try: Boolean!) {test : user(id:$try) {}}");
        QLQuery query4 = parser.buildQuery();
        assertEquals(query4.getName(), "hello");

        assertEquals(query4.getParameters().getParams().size(), 1);
        assertEquals(query4.getParameters().getParams().get(0).getName(), "try");
        assertEquals(query4.getParameters().getParams().get(0).getType(), QLType.BOOLEAN);
        assertEquals(query4.getQueryFields().size(), 1);
        assertEquals(query4.getQueryFields().get(0).getName(), "user");
        assertEquals(query4.getQueryFields().get(0).getAlias(), "test");
        assertEquals(query4.getQueryFields().get(0).getParameters().size(), 1);
        assertTrue(query4.getQueryFields().get(0).getParameters().containsKey("id"));
        assertThat(query4.getQueryFields().get(0).getParameters().get("id"),instanceOf(QLVariablesElement.class));
    }

    @Test
    public void checkFields() throws Exception {
        QLParser parser = new QLParser();

        parser.setToParse("query hello {user {aa, ab:bb, cc(aze: \"a\"), abc:dd(azerr:\"b\", zef:\"c\")} bib{dsf}}");
        QLQuery query = parser.buildQuery();
        assertQueryIsComplete(query);
    }

    private void assertQueryIsComplete(QLQuery query) {
        assertEquals(query.getName(), "hello");
        assertEquals(query.getQueryFields().size(), 2);
        assertThat(query.getQueryFields().get(0), instanceOf(QLNode.class));
        List<QLElement> children = ((QLNode)query.getQueryFields().get(0)).getChildren();
        assertEquals(children.size(), 4);
        QLElement firstChild = children.get(0);
        QLElement secondChild = children.get(1);
        QLElement thirdChild = children.get(2);
        QLElement fourthChild = children.get(3);

        assertEquals(firstChild.getName(), "aa");
        assertThat(firstChild, instanceOf(QLLeaf.class));

        assertEquals(secondChild.getName(), "bb");
        assertThat(secondChild, instanceOf(QLLeaf.class));
        assertEquals(secondChild.getAlias(), "ab");

        assertEquals(thirdChild.getName(), "cc");
        assertEquals(thirdChild.getParameters().size(), 1);
        assertTrue(thirdChild.getParameters().containsKey("aze"));
        assertEquals(thirdChild.getParameters().get("aze"), "a");
        assertThat(thirdChild, instanceOf(QLLeaf.class));

        assertEquals(fourthChild.getName(), "dd");
        assertEquals(fourthChild.getAlias(), "abc");
        assertEquals(fourthChild.getParameters().size(), 2);
        assertTrue(fourthChild.getParameters().containsKey("azerr"));
        assertTrue(fourthChild.getParameters().containsKey("zef"));
        assertEquals(fourthChild.getParameters().get("zef"), "c");
        assertEquals(fourthChild.getParameters().get("azerr"), "b");
        assertThat(fourthChild, instanceOf(QLLeaf.class));

        assertThat(query.getQueryFields().get(1), instanceOf(QLNode.class));
        assertEquals(query.getQueryFields().get(1).getName(), "bib");
        assertEquals(((QLNode)query.getQueryFields().get(1)).getChildren().size(), 1);
        assertEquals(((QLNode)query.getQueryFields().get(1)).getChildren().get(0).getName(), "dsf");
    }

    @Test
    public void checkObjectAsField() throws Exception {
        QLParser parser = new QLParser();

        parser.setToParse("query hello {user {aa, bb{cc,dd(id:\"v\", vf:\"d\"){ee}}}}");
        QLQuery query = parser.buildQuery();
        assertEquals(query.getName(), "hello");
        assertEquals(query.getQueryFields().size(), 1);
        List<QLElement> children = ((QLNode)query.getQueryFields().get(0)).getChildren();
        assertEquals(children.size(), 2);
        assertEquals(children.get(0).getName(), "aa");
        assertThat(children.get(0), instanceOf(QLLeaf.class));
        assertThat(children.get(1), instanceOf(QLNode.class));
        QLNode element = (QLNode) children.get(1);
        assertEquals(element.getName(), "bb");
        assertEquals(element.getChildren().size(), 2);
        assertEquals(element.getChildren().get(0).getName(), "cc");

        assertThat(element.getChildren().get(1), instanceOf(QLNode.class));
        QLNode childNode = (QLNode) element.getChildren().get(1);
        assertEquals(childNode.getName(), "dd");
        assertEquals(childNode.getParameters().size(), 2);
        assertEquals(childNode.getParameters().get("id"), "v");
        assertEquals(childNode.getParameters().get("vf"), "d");
        assertEquals(childNode.getChildren().size(), 1);
        assertEquals(childNode.getChildren().get(0).getName(), "ee");
    }

    @Test
    public void checkFullAnonymousQuery() throws Exception {
        QLParser parser = new QLParser();
        parser.setToParse("{user {aa, bb{cc,dd(id:\"v\", vf:\"d\"){ee}}}}");
        QLQuery query = parser.buildQuery();
        assertEquals(query.getName(), null);
        assertEquals(query.getQueryFields().size(), 1);
        List<QLElement> children = ((QLNode)query.getQueryFields().get(0)).getChildren();
        assertEquals(children.size(), 2);
        assertEquals(children.get(0).getName(), "aa");
        assertThat(children.get(0), instanceOf(QLLeaf.class));
        assertThat(children.get(1), instanceOf(QLNode.class));
        QLNode element = (QLNode) children.get(1);
        assertEquals(element.getName(), "bb");
        assertEquals(element.getChildren().size(), 2);
        assertEquals(element.getChildren().get(0).getName(), "cc");

        assertThat(element.getChildren().get(1), instanceOf(QLNode.class));
        QLNode childNode = (QLNode) element.getChildren().get(1);
        assertEquals(childNode.getName(), "dd");
        assertEquals(childNode.getParameters().size(), 2);
        assertEquals(childNode.getParameters().get("id"), "v");
        assertEquals(childNode.getParameters().get("vf"), "d");
        assertEquals(childNode.getChildren().size(), 1);
        assertEquals(childNode.getChildren().get(0).getName(), "ee");

    }

    @Test
    public void queryParameterTest() throws Exception {

        QLParser parser = new QLParser();
        parser.setToParse("query hello($try: Boolean!, $try2:String, $try3:Int, $try4:Float, $try5:ID!) {user(id:$try) {}}");
        QLQuery query = parser.buildQuery();
        assertEquals(query.getName(), "hello");
        assertEquals(query.getParameters().getParams().size(), 5);
        assertEquals(query.getQueryFields().size(), 1);

        QLElement node = query.getQueryFields().get(0);
        assertEquals(node.getName(), "user");
        assertEquals(node.getParameters().size(), 1);
        assertTrue(node.getParameters().containsKey("id"));
        assertThat(node.getParameters().get("id"),instanceOf(QLVariablesElement.class));
        QLVariablesElement id = (QLVariablesElement) node.getParameters().get("id");
        assertEquals(id.getName(), "try");
        assertEquals(id.getType(), null);
        assertEquals(id.isMandatory(), false);
    }

    @Test
    public void fragmentDeclarationTest() throws Exception {
        QLParser parser = new QLParser();

        parser.setToParse("query hello {#-list-;user {aa, ab:bb, #-list-;cc(aze: \"a\"), abc:dd(azerr:\"b\", zef:\"c\")} bib{dsf}}fragment test on User{name, email, posts{id}}");
        QLQuery query = parser.buildQuery();
        assertQueryIsComplete(query);
        assertEquals(query.getFragments().size(), 1);
        QLFragment fragment = query.getFragments().get(0);
        assertEquals(fragment.getName(), "test");
        assertEquals(fragment.getTargetObject(), "User");
        assertEquals(fragment.getChildren().size(), 3);
        assertEquals(fragment.getChildren().get(0).getName(), "name");
        assertThat(fragment.getChildren().get(0), instanceOf(QLLeaf.class));
        assertEquals(fragment.getChildren().get(1).getName(), "email");
        assertThat(fragment.getChildren().get(1), instanceOf(QLLeaf.class));
        assertEquals(fragment.getChildren().get(2).getName(), "posts");
        assertThat(fragment.getChildren().get(2), instanceOf(QLNode.class));
        QLNode node = (QLNode) fragment.getChildren().get(2);
        assertEquals(node.getChildren().size(), 1);
        assertThat(node.getChildren().get(0), instanceOf(QLLeaf.class));
        assertEquals(node.getChildren().get(0).getName(), "id");
    }

    @Test
    public void multipleFragmentDeclarationTest() throws Exception {
        QLParser parser = new QLParser();

        parser.setToParse("fragment test1 on User{alias : name1, email1(size: 2), posts1{id1}}query hello {user {aa, ab:bb, cc(aze: \"a\"), abc:dd(azerr:\"b\", zef:\"c\")} bib{dsf}}fragment test on User{name, email, posts{id}}");
        QLQuery query = parser.buildQuery();
        assertEquals(query.getFragments().size(), 2);
        QLFragment fragment = query.getFragments().get(0);
        assertEquals(fragment.getName(), "test1");
        assertEquals(fragment.getTargetObject(), "User");
        assertEquals(fragment.getChildren().size(), 3);
        QLElement element = fragment.getChildren().get(0);
        assertEquals(element.getName(), "name1");
        assertEquals(element.getAlias(), "alias");
        assertThat(element, instanceOf(QLLeaf.class));
        QLElement element1 = fragment.getChildren().get(1);
        assertEquals(element1.getName(), "email1");
        assertEquals(element1.getParameters().size(), 1);
        assertEquals(element1.getParameters().get("size"), 2);
        assertThat(element1, instanceOf(QLLeaf.class));
        assertEquals(fragment.getChildren().get(2).getName(), "posts1");
        assertThat(fragment.getChildren().get(2), instanceOf(QLNode.class));
        QLNode node = (QLNode) fragment.getChildren().get(2);
        assertEquals(node.getChildren().size(), 1);
        assertThat(node.getChildren().get(0), instanceOf(QLLeaf.class));
        assertEquals(node.getChildren().get(0).getName(), "id1");
        QLFragment fragment1 = query.getFragments().get(1);
        assertEquals(fragment1.getName(), "test");
        assertEquals(fragment1.getTargetObject(), "User");
        assertEquals(fragment1.getChildren().size(), 3);
        assertEquals(fragment1.getChildren().get(0).getName(), "name");
        assertThat(fragment1.getChildren().get(0), instanceOf(QLLeaf.class));
        assertEquals(fragment1.getChildren().get(1).getName(), "email");
        assertThat(fragment1.getChildren().get(1), instanceOf(QLLeaf.class));
        assertEquals(fragment1.getChildren().get(2).getName(), "posts");
        assertThat(fragment1.getChildren().get(2), instanceOf(QLNode.class));
        QLNode node1 = (QLNode) fragment1.getChildren().get(2);
        assertEquals(node1.getChildren().size(), 1);
        assertThat(node1.getChildren().get(0), instanceOf(QLLeaf.class));
        assertEquals(node1.getChildren().get(0).getName(), "id");
    }

    @Test
    public void testFragmentImport() throws Exception {
        QLParser parser = new QLParser();

        parser.setToParse("fragment test1 on User{alias : name1, email1(size: 2), posts1{id1}}query hello {user {...test}}fragment test on User{...test1}");
        // TODO (kelianclerc) 31/5/17 test this
        QLQuery query = parser.buildQuery();

        assertEquals(query.getFragments().size(), 2);
        QLFragment fragment = query.getFragments().get(0);
        assertEquals(fragment.getName(), "test1");
        assertEquals(fragment.getTargetObject(), "User");
        assertEquals(fragment.getChildren().size(), 3);
        QLFragment fragment1 = query.getFragments().get(1);
        assertEquals(fragment1.getName(), "test");
        assertEquals(fragment1.getChildren().size(), 1);
        assertThat(fragment1.getChildren().get(0), instanceOf(QLFragmentNode.class));
    }

    @Test
    public void testMultipleFragmentImport() throws Exception {
        QLParser parser = new QLParser();

        parser.setToParse("query hello {user {...test, ...test1}, testze}fragment test on User{name}fragment test1 on User{alias : name1, email1(size: 2), posts1{id1}}");
        // TODO (kelianclerc) 31/5/17 test this
        QLQuery query = parser.buildQuery();

        assertEquals(query.getFragments().size(), 2);
        QLFragment fragment = query.getFragments().get(1);
        assertEquals(fragment.getName(), "test1");

        assertEquals(fragment.getTargetObject(), "User");
        assertEquals(fragment.getChildren().size(), 3);
        QLFragment fragment1 = query.getFragments().get(0);
        assertEquals(fragment1.getName(), "test");
        assertEquals(fragment1.getChildren().size(), 1);
        System.out.println(query.getQueryFields().get(0).print());
        System.out.println(query.getQueryFields().get(1).print());
        assertEquals(query.getQueryFields().size(), 2);
        assertEquals(query.getName(), "hello");
        QLElement node = query.getQueryFields().get(0);
        assertEquals(node.getName(), "user");
        assertEquals(node.getParameters().size(), 0);
        assertEquals(((QLNode)node).getChildren().size(), 2);
        assertThat(((QLNode)node).getChildren().get(0), instanceOf(QLFragmentNode.class));
        assertThat(((QLNode)node).getChildren().get(1), instanceOf(QLFragmentNode.class));
        QLElement staticField = query.getQueryFields().get(1);
        assertThat(staticField, instanceOf(QLLeaf.class));
        assertEquals(staticField.getName(), "testze");
    }

    @Test
    public void defaultValueTest() throws Exception {
        QLParser parser = new QLParser();
        parser.setToParse("query hello($name : String! = \"Kelian\") { name }");
        QLQuery qlQuery = parser.buildQuery();

        assertEquals(qlQuery.getParameters().getParams().get(0).getDefaultValue(), "\"Kelian\"");
        assertEquals(qlQuery.getParameters().getParams().get(0).isMandatory(), true);
        assertEquals(qlQuery.getParameters().getParams().get(0).getType(), QLType.STRING);
        assertEquals(qlQuery.getParameters().getParams().get(0).getName(), "name");
    }

    @Test
    public void directiveTest() throws Exception {
        QLParser parser = new QLParser();
        parser.setToParse("query hello{ name @skip(if: $name)}");
        QLQuery qlQuery = parser.buildQuery();

        assertEquals(qlQuery.getQueryFields().get(0).getSkip(), "$name");
        assertEquals(qlQuery.getQueryFields().get(0).getInclude(), null);
        assertEquals(qlQuery.getQueryFields().get(0).getName(), "name");
        assertEquals(qlQuery.getQueryFields().get(0).getAlias(),null);
        assertEquals(qlQuery.getQueryFields().get(0).getParameters().size(), 0);
        assertEquals(qlQuery.getQueryFields().get(0).isList(), false);
    }

    @Test
    public void directiveAliasTest() throws Exception {
        QLParser parser = new QLParser();
        parser.setToParse("query hello{ test:name @include(if: $name)}");
        QLQuery qlQuery = parser.buildQuery();

        assertEquals(qlQuery.getQueryFields().get(0).getInclude(), "$name");
        assertEquals(qlQuery.getQueryFields().get(0).getSkip(), null);
        assertEquals(qlQuery.getQueryFields().get(0).getName(), "name");
        assertEquals(qlQuery.getQueryFields().get(0).getAlias(),"test");
        assertEquals(qlQuery.getQueryFields().get(0).getParameters().size(), 0);
        assertEquals(qlQuery.getQueryFields().get(0).isList(), false);
    }

    @Test
    public void directiveParamTest() throws Exception {
        QLParser parser = new QLParser();
        parser.setToParse("query hello{ test:name (id:3) @include(if: $name){azdad}}");
        QLQuery qlQuery = parser.buildQuery();

        assertEquals(qlQuery.getQueryFields().get(0).getInclude(), "$name");
        assertEquals(qlQuery.getQueryFields().get(0).getSkip(), null);
        assertEquals(qlQuery.getQueryFields().get(0).getName(), "name");
        assertEquals(qlQuery.getQueryFields().get(0).getAlias(),"test");
        assertEquals(qlQuery.getQueryFields().get(0).getParameters().size(), 1);
        assertEquals(qlQuery.getQueryFields().get(0).getParameters().get("id"), 3);
        assertEquals(qlQuery.getQueryFields().get(0).isList(), false);
        assertThat(qlQuery.getQueryFields().get(0), instanceOf(QLNode.class));
        QLNode node = (QLNode) qlQuery.getQueryFields().get(0);
        assertEquals(node.getChildren().size(),1);

    }

    @Test
    public void directiveBothTest() throws Exception {
        QLParser parser = new QLParser();
        parser.setToParse("query hello{ test:name @include(if: $name) @skip(if: $name2) (id:3) {azdad}}");
        QLQuery qlQuery = parser.buildQuery();

        assertEquals(qlQuery.getQueryFields().get(0).getInclude(), "$name");
        assertEquals(qlQuery.getQueryFields().get(0).getSkip(), "$name2");
        assertEquals(qlQuery.getQueryFields().get(0).getName(), "name");
        assertEquals(qlQuery.getQueryFields().get(0).getAlias(),"test");
        assertEquals(qlQuery.getQueryFields().get(0).getParameters().size(), 1);
        assertEquals(qlQuery.getQueryFields().get(0).getParameters().get("id"), 3);
        assertEquals(qlQuery.getQueryFields().get(0).isList(), false);
        assertThat(qlQuery.getQueryFields().get(0), instanceOf(QLNode.class));
        QLNode node = (QLNode) qlQuery.getQueryFields().get(0);
        assertEquals(node.getChildren().size(),1);

    }
}
