package com.applidium.qlrequest;

import com.applidium.qlrequest.Query.QLQuery;
import com.applidium.qlrequest.Tree.QLParser;
import com.squareup.javapoet.TypeSpec;

import org.junit.Test;

import static junit.framework.TestCase.assertEquals;

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
}
