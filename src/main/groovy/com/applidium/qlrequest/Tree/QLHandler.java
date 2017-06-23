package com.applidium.qlrequest.Tree;

import com.applidium.qlrequest.Query.QLFragment;
import com.applidium.qlrequest.Query.QLType;
import com.applidium.qlrequest.Query.QLVariablesElement;
import com.applidium.qlrequest.exceptions.QLParserException;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class QLHandler {
    private String toParse;
    private final Map<Integer, QLNode> currentPosition = new HashMap<>();
    private int elevation = 0;
    private QueryDelimiter delimiter;
    private QLType typeBuffer;
    private String enumTypeBuffer;
    private boolean isList;
    private boolean shouldAvoidReset = false;
    private boolean isFragmentField;
    private List<QLFragment> fragments;

    public QLHandler(String toParse, boolean isFragmentField) {
        this.toParse = toParse;
        this.isFragmentField = isFragmentField;
        delimiter = new QueryDelimiter();
    }

    public void initTreeBuilding() {
        elevation = 0;
        currentPosition.clear();
    }

    public QLNode parseBody(int endIndex, QLNode node) throws QLParserException {
        currentPosition.put(elevation,  node);
        elevation ++;
        trimString(endIndex + 1);

        processNextField();

        return currentPosition.get(0);
    }

    private void trimString(int start) {
        this.toParse = toParse.substring(start);
    }

    private void processNextField() throws QLParserException {

        if (toParse.startsWith(",")) {
            toParse = toParse.substring(1);
        }

        if (toParse.length() <= 0) {
            return;
        }

        if (!shouldAvoidReset) {
            typeBuffer = null;
            isList = false;
            enumTypeBuffer = null;
        } else {
            shouldAvoidReset = false;
        }

        delimiter.analyze(toParse);

        if (delimiter.isNextCommentary()) {
            handleCommentary(delimiter.endCarret);
        } else {
            if (delimiter.isNextCloseCurly()) {
                handleClosingCurly();
            } else if (delimiter.isNextLeaf()) {
                handleSimpleField(delimiter.endCarret);
            } else if (delimiter.isNextNode()) {
                handleNode(delimiter.endCarret);
            } else if (delimiter.isNextLastField()) {
                handleLastField(delimiter.endCarret);
            } else if (delimiter.isNextFragmentImport()) {
                handleFragmentImport();
            }
        }
    }


    private void handleCommentary(int endCommentary) throws QLParserException {
        String typeString = toParse.subSequence(0, endCommentary).toString();
        if(typeString.contains("#-type-")) {
            typeString = typeString.replace("#", "");
            typeString = typeString.replace("-type-", "");
            typeString = typeString.replace(";", "");
            typeString = typeString.replaceAll(" ", "");
        } else if (typeString.contains("#-list-")) {
            isList = true;
        } else {
            isList = false;
        }
        typeBuffer = parseType(typeString);
        if (typeBuffer == QLType.ENUM) {
            enumTypeBuffer = typeString;
        }
        shouldAvoidReset = true;
        trimString(endCommentary + 1);

        processNextField();
    }

    private void handleClosingCurly() throws QLParserException {
        elevation--;
        if (elevation <= 0) {
            return;
        }
        trimString(1);

        processNextField();
    }

    private void handleSimpleField(int nextCommaIndex) throws QLParserException {
        QLElement field = createElementFromString(toParse.substring(0, nextCommaIndex));
        field.setList(isList);
        field = checkIfDirective(field);
        trimString(nextCommaIndex + 1);
        QLLeaf child = new QLLeaf(field, typeBuffer);
        if (typeBuffer == QLType.ENUM) {
            child.setEnumName(enumTypeBuffer);
        }
        currentPosition.get(elevation - 1).addChild(child);
        processNextField();
    }

    QLElement checkIfDirective(QLElement element) {
        Pattern pattern = Pattern.compile("@include|@skip");
        Matcher matcher = pattern.matcher(toParse);
        if (matcher.find()) {
            element = computeDirectives(element);
        }
        return element;
    }

    QLElement computeDirectives(QLElement qlElement) {
        String includeArtifact = "@include(if:";
        String skipArtifact = "@skip(if:";
        String currentToParse = toParse;
        if (currentToParse.contains(includeArtifact)) {
            int startOfInclude = currentToParse.indexOf(includeArtifact);
            int closeIncludeIndex = currentToParse.indexOf(")", startOfInclude);
            String variableName = currentToParse.substring(startOfInclude + includeArtifact.length(), closeIncludeIndex);
            qlElement.setInclude(variableName);
        }
        if (currentToParse.contains(skipArtifact)) {
            int startOfSkip = currentToParse.indexOf(skipArtifact);
            int closeSkipIndex = currentToParse.indexOf(")", startOfSkip);
            String variableName = currentToParse.substring(currentToParse.indexOf(skipArtifact) + skipArtifact.length(), closeSkipIndex);
            qlElement.setSkip(variableName);
        }


        return qlElement;
    }


    private void handleFragmentImport() throws QLParserException {

        Pattern inlinePattern = Pattern.compile("(...\\s*on\\s*[A-Z]\\w*\\s*\\{)");
        Matcher matcher = inlinePattern.matcher(toParse);

        if (matcher.find()) {
            handleInlineFragment();
        } else {
            handleFragment();
        }
    }

    void handleInlineFragment() throws QLParserException {
        String inlineFragment = QLParser.blockFetch(toParse, "...");
        int blockLength = inlineFragment.length();
        Pattern pattern = Pattern.compile("...\\s*on\\s*([A-Z]\\w*)\\s*\\{");
        Matcher matcher = pattern.matcher(inlineFragment);
        if (matcher.find()) {
            QLFragmentNode result = new QLFragmentNode("");
            result.setInlineFragment(true);
            result.setTarget(matcher.group(1));
            inlineFragment = inlineFragment.replaceAll(pattern.pattern(), "");
            QLHandler qlFragmentHandler = new QLHandler(inlineFragment, false);
            QLNode holder = new QLNode("holder");
            result.setChildren(qlFragmentHandler.parseBody(-1, holder).getChildren());

            currentPosition.get(elevation - 1).addChild(result);

            trimString(blockLength);
            processNextField();
        } else {
            throw new QLParserException("Bad inline fragment syntax. Expecting: \"... on [A-Z][a-zA-Z0-9]* { fileds }\", found: " + inlineFragment);
        }
    }

    void handleFragment() throws QLParserException {
        int begin = toParse.indexOf("...");
        System.out.println(toParse);
        String fragmentName = toParse.substring(begin + 3, delimiter.endCarret);
        currentPosition.get(elevation - 1).addChild(new QLFragmentNode(fragmentName));

        trimString(delimiter.endCarret);
        processNextField();
    }

    private void handleNode(int nextCurlyIndex) throws QLParserException {
        QLElement field = createElementFromString(toParse.substring(0, nextCurlyIndex));
        field.setList(isList);
        field = checkIfDirective(field);
        QLNode childNode = new QLNode(field);
        if (elevation > 0) {
            currentPosition.get(elevation - 1).addChild(childNode);
        }

        parseBody(nextCurlyIndex, childNode);
    }


    private void handleLastField(int endCarret) throws QLParserException {
        QLElement field = createElementFromString(toParse.substring(0, endCarret));
        field.setList(isList);
        field = checkIfDirective(field);
        QLLeaf child = new QLLeaf(field, typeBuffer);
        if (typeBuffer == QLType.ENUM) {
            child.setEnumName(enumTypeBuffer);
        }
        currentPosition.get(elevation - 1).addChild(child);
        trimString(endCarret);

        processNextField();
    }

    public static QLElement createElementFromString(String substring) {
        substring = substring.replaceAll(" ", "");
        Pattern pattern = Pattern.compile("(?<!@include|@skip)\\(");
        if (substring.length() > 0) {
            String[] stringList = substring.split(pattern.pattern());

            QLElement element;
            element = getFieldName(stringList[0]);
            if (stringList.length > 1) {
                element.setParameters(getParameters(stringList[1]));
            }
            return element;
        } else {
            return new QLElement("");
        }
    }

    private static QLElement getFieldName(String s) {
        QLElement element;
        String name = s;


        Pattern pattern = Pattern.compile("@include|@skip");
        Matcher matcher = pattern.matcher(name);
        if (matcher.find()) {
            name = name.substring(0, matcher.start());
        }

        if (name.indexOf(":")>0) {
            String[] aliasName = name.split("[:]");
            element = new QLElement(aliasName[1]);
            element.setAlias(aliasName[0]);
        } else {
            element = new QLElement(name);
        }
        return element;
    }

    private static Map<String, Object> getParameters(String stringParameters) {
        Pattern patternSkip = Pattern.compile("@skip\\((.[^)]*)\\)");
        Pattern patternInclude = Pattern.compile("@include\\((.[^)]*)\\)");
        stringParameters = stringParameters.replaceAll(patternSkip.pattern(), "");
        stringParameters = stringParameters.replaceAll(patternInclude.pattern(), "");
        stringParameters = stringParameters.replaceAll("[)]", "");
        Map<String, Object> params = new HashMap<>();
        String[] stringParametersSplit = stringParameters.split("[,]");
        for (String param : stringParametersSplit) {
            String[] unit = param.split("[:]");
            if (unit.length > 1) {
                Pattern pattern = Pattern.compile("#-param-(\\w+);");
                Matcher matcher = pattern.matcher(unit[0]);
                QLType paramType = null;
                if (matcher.find()) {
                    paramType = parseType(matcher.group(1));
                }
                unit[0] = unit[0].replaceAll(pattern.pattern(), "");

                if (unit[0].charAt(0) == '$') {
                    params.put(unit[0], parseVariableType(unit));
                } else if (unit[1].charAt(0) == '$') {
                    params.put(unit[0], new QLVariablesElement(unit[1].replace("$", "")));
                } else {
                    if (paramType != null) {
                        switch (paramType) {
                            case ID:
                            case STRING:
                            case ENUM:
                                params.put(unit[0], unit[1]);
                                break;
                            case INT:
                                params.put(unit[0], Integer.valueOf(unit[1]));
                                break;
                            case BOOLEAN:
                                params.put(unit[0], Boolean.valueOf(unit[1]));
                                break;
                            case FLOAT:
                                params.put(unit[0], Float.valueOf(unit[1]));
                                break;
                        }
                    } else {
                        if (unit[1].indexOf("\"")  >= 0) {
                            unit[1]= unit[1].replaceAll("\"", "");
                            params.put(unit[0], unit[1]);
                        } else if (unit[1].equals("true")||unit[1].equals("false")) {
                            params.put(unit[0], Boolean.valueOf(unit[1]));
                        } else if (unit[1].indexOf(".")>= 0) {
                            params.put(unit[0], Float.valueOf(unit[1]));
                        } else {
                            params.put(unit[0], Integer.valueOf(unit[1]));
                        }
                    }
                }
            }
        }
        return params;
    }

    private static QLVariablesElement parseVariableType(String[] unit) {
        QLVariablesElement element = new QLVariablesElement();
        int endTypeIndex = unit[1].length();
        if (unit[1].contains("!")) {
            endTypeIndex = unit[1].indexOf("!");
            element.setMandatory(true);
            unit[1] = unit[1].replace("!", "");
        } else {
            element.setMandatory(false);
        }
        if (unit[1].contains("=")) {
            endTypeIndex = Math.min(unit[1].indexOf("="), endTypeIndex);
            element.setDefaultValue(unit[1].substring(unit[1].indexOf("=") + 1));
        }
        element.setName(unit[0].replace("$",""));
        QLType type = parseType(unit[1].substring(0, endTypeIndex));
        if (type == QLType.ENUM) {
            element.setEnumName(unit[1].substring(0, endTypeIndex));
        }
        element.setType(type);
        return element;
    }

    private static QLType parseType(String type) {
        switch (type) {
            case "Boolean":
                return QLType.BOOLEAN;
            case "String":
                return QLType.STRING;
            case "Int":
                return QLType.INT;
            case "ID":
                return QLType.ID;
            case "Float":
                return QLType.FLOAT;
            default:
                return QLType.ENUM;
        }
    }

    private class QueryDelimiter {

        public static final int MAX_VALUE = 1010100202;
        private int nextCloseCurlyIndex;
        private int nextCurlyIndex;
        private int nextCommaIndex;
        private int endCarret;
        private int nextFragmentImportIndex;
        private int nextCommentary;
        private int nextEndCommentary;

        public QueryDelimiter() {
        }

        public void analyze(String toAnalyze) {
            Pattern pattern = Pattern.compile(",(?![^(]*\\))");
            Matcher matcher = pattern.matcher(toAnalyze);
            if (matcher.find()) {
                nextCommaIndex = matcher.start();
            } else {
                nextCommaIndex = -1;
            }
            nextCurlyIndex = toAnalyze.indexOf("{");
            nextCloseCurlyIndex = toAnalyze.indexOf("}");
            nextFragmentImportIndex = toAnalyze.indexOf("...");
            nextEndCommentary = toAnalyze.indexOf(";");

            nextCommaIndex = ifNegativeMakeGreat(nextCommaIndex);
            nextCurlyIndex = ifNegativeMakeGreat(nextCurlyIndex);
            nextCloseCurlyIndex = ifNegativeMakeGreat(nextCloseCurlyIndex);
            nextCommentary = Math.min(ifNegativeMakeGreat(toAnalyze.indexOf("#-type-")),ifNegativeMakeGreat(toAnalyze.indexOf("#-list-")));
            nextEndCommentary = ifNegativeMakeGreat(nextEndCommentary);
            nextFragmentImportIndex = ifNegativeMakeGreat(nextFragmentImportIndex);
        }


        private int ifNegativeMakeGreat(int toCheck) {
            if (toCheck < 0) {
                return MAX_VALUE;
            }
            return toCheck;
        }

        public boolean isNextCloseCurly() {
            boolean b = nextCloseCurlyIndex == 0;
            if(b) {
                endCarret = 0;
            }
            return b;
        }

        public boolean isNextLeaf() {
            boolean b = isTheNextOccurance(nextCommaIndex);
            if (b) {
                endCarret = nextCommaIndex;
            }
            return b;
        }

        public boolean isNextNode() {
            boolean b = isTheNextOccurance(nextCurlyIndex);
            if (b) {
                endCarret = nextCurlyIndex;
            }
            return b;
        }

        public boolean isNextLastField() {
            boolean b = isTheNextOccurance(nextCloseCurlyIndex);
            if (b) {
                endCarret = nextCloseCurlyIndex;
            }
            return b;
        }

        public boolean isNextFragmentImport() {
            boolean b = isTheNextOccurance(nextFragmentImportIndex);
            if (b) {
                endCarret = Math.min(nextCloseCurlyIndex, nextCommaIndex);
            }
            return b;
        }

        public boolean isNextCommentary() {
            boolean b = isTheNextOccurance(nextCommentary);
            if (b) {
                endCarret = nextEndCommentary;
            }
            return b;
        }

        private boolean isTheNextOccurance(int target) {
            return Math.min(
                target,
                Math.min(nextCommaIndex,
                    Math.min(nextCurlyIndex,
                        Math.min(nextCommentary,
                            Math.min(nextCloseCurlyIndex, nextFragmentImportIndex)
                        )
                    )
                )
            ) == target;
        }
    }

    public static boolean isInteger(String str) {
        if (str == null) {
            return false;
        }
        int length = str.length();
        if (length == 0) {
            return false;
        }
        int i = 0;
        if (str.charAt(0) == '-') {
            if (length == 1) {
                return false;
            }
            i = 1;
        }
        for (; i < length; i++) {
            char c = str.charAt(i);
            if (c < '0' || c > '9') {
                return false;
            }
        }
        return true;
    }

    public static boolean isFloat(String str) {
        if (str == null) {
            return false;
        }
        int length = str.length();
        if (length == 0) {
            return false;
        }
        int i = 0;
        if (str.charAt(0) == '-') {
            if (length == 1) {
                return false;
            }
            i = 1;
        }
        for (; i < length; i++) {
            char c = str.charAt(i);
            if (c < '0' || c > '9') {
                return false;
            }
        }
        return true;
    }
}
