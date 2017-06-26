package com.applidium.qlrequest;

import com.applidium.qlrequest.Query.QLQuery;
import com.applidium.qlrequest.Tree.QLParser;
import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.TypeSpec;

import org.junit.Test;

import static junit.framework.TestCase.assertEquals;
import static junit.framework.TestCase.assertTrue;

public class QLClassGeneratorTest {
    @Test
    public void generateQueryTest() throws Exception {
        QLQuery qlQuery;
        String query = "{users {...userInfo,email}}fragment userInfo on User {...postInfo}fragment postInfo on Post {id,body}";

        QLParser parser = new QLParser();
        parser.setToParse(query);
        qlQuery = parser.buildQuery();
        QLClassGenerator classGenerator = new QLClassGenerator();
        classGenerator.setQlQuery(qlQuery);
        classGenerator.setPackage("com.applidium.qlrequest");
        TypeSpec method = classGenerator.generateQuery("Test");
        testRequest(method);

        TypeSpec response = classGenerator.generateResponse("Test");

        assertEquals(method.name, "TestRequest");
    }

    private void testRequest(TypeSpec request) {
        assertEquals(request.name, "TestRequest");
        assertEquals(request.fieldSpecs.size(), 2);
    }

    @Test
    public void generateEnumTest() throws Exception {
        QLQuery qlQuery;
        String query = "AA,BB,CC";

        QLClassGenerator classGenerator = new QLClassGenerator();
        classGenerator.setPackage("com.applidium.qlrequest");
        TypeSpec method = classGenerator.generateEnum("test.qlenum", "CT,TERRO,SPEC");

        assertEquals(method.name, "TestQLEnum");
        assertTrue(method.enumConstants.containsKey("CT"));
        assertTrue(method.enumConstants.containsKey("TERRO"));
        assertTrue(method.enumConstants.containsKey("SPEC"));
    }

    @Test
    public void generateTreeQueryTest() throws Exception {
        QLQuery qlQuery;
        String query = "{users {id(id:3,name:4),email(id:5)}}";

        QLParser parser = new QLParser();
        parser.setToParse(query);
        qlQuery = parser.buildQuery();
        QLClassGenerator classGenerator = new QLClassGenerator();
        classGenerator.setQlQuery(qlQuery);
        classGenerator.setPackage("com.applidium.qlrequest");

        TypeSpec method = classGenerator.generateQuery("test.query");

        TypeSpec response = method;

        assertEquals(response.fieldSpecs.size(), 3);
        assertEquals(response.typeSpecs.size(), 1);
        assertEquals(response.typeSpecs.get(0).fieldSpecs.size(), 2);
        assertEquals(response.typeSpecs.get(0).typeSpecs.size(), 2);
        assertEquals(response.typeSpecs.get(0).typeSpecs.get(0).name, "Id");
        assertEquals(response.typeSpecs.get(0).typeSpecs.get(0).fieldSpecs.size(),2);
        assertEquals(response.typeSpecs.get(0).typeSpecs.get(0).fieldSpecs.get(0).name, "id");
        assertEquals(response.typeSpecs.get(0).typeSpecs.get(0).fieldSpecs.get(1).name, "name");
        for(FieldSpec fieldSpecSpec : response.fieldSpecs) {
            System.out.println(fieldSpecSpec.name);
            if (fieldSpecSpec.name.equals("query")) {
                System.out.println(fieldSpecSpec.toString());
            }
        }

        System.out.println(response.toString());

    }
}
