package com.applidium.qlrequest.Tree

import com.applidium.qlrequest.Query.*
import com.applidium.qlrequest.exceptions.QLParserException

import java.util.regex.Matcher
import java.util.regex.Pattern

public class QLParser {

    public static final String QUERY_KEYWORD = "query";
    public static final String FRAGMENT_KEYWORD = "fragment";
    private String initialString;
    private String toParse;
    private QLQuery query;
    private List<QLFragment> fragments;
    private List<QLElement> toUpdate;
    private List<QLElement> parentOfToUpdate;
    private final Map<Integer, QLNode> currentPosition = new HashMap<>();
    private int elevation = 0;
    private QueryDelimiter delimiter;
    boolean isFragmentField;
    private QLType typeBuffer;
    private boolean isList;
    private boolean shouldAvoidReset = false;

    public static QLVariables parseVariables (String variables) {
        Map<String, Object> map = new HashMap<>();
        if (variables.length() > 2) {
            variables = variables.replace("[\n\r]", "");
            variables = variables.replace(" ", "");
            variables = variables.substring(1, variables.length()-1);
            String[] keyValue = variables.split(",");
            for(String pair : keyValue) {
                String[] pairElement = pair.split(":");
                String toAnalyze = pairElement[1];
                if (isInteger(toAnalyze)) {
                    map.put(pairElement[0], Integer.valueOf(toAnalyze));
                } else if (toAnalyze.startsWith("\"") && toAnalyze.endsWith("\"")){
                    map.put(pairElement[0], toAnalyze);
                } else if (toAnalyze.equals("true") || toAnalyze.equals("false")) {
                    map.put(pairElement[0], Boolean.valueOf(toAnalyze));
                } else {
                    try {
                        map.put(pairElement[0], Double.parseDouble(toAnalyze));
                    } catch (NumberFormatException e) {
                        map.put(pairElement[0], toAnalyze);
                    }
                }
            }
        }

        return new QLVariables(map);
    }

    public QLParser() {
        delimiter = new QueryDelimiter();
        fragments = new ArrayList<>();
        toUpdate = new ArrayList<>();
        parentOfToUpdate = new ArrayList<>();
    }

    public QLParser(String toParse) {
        delimiter = new QueryDelimiter();
        fragments = new ArrayList<>();
        toUpdate = new ArrayList<>();
        parentOfToUpdate = new ArrayList<>();
        toParse = cleanComments(toParse)
        toParse = toParse.replaceAll("[\n\r]", "");
        this.toParse = initialString = toParse;
    }

    private static String cleanComments(String toClean) {
        if (toClean.contains(System.getProperty("line.separator"))) {
            String[] lines = toClean.split(System.getProperty("line.separator"));
            for(int i = 0; i < lines.length; i++) {
                String s = lines[i]
                s = s.replaceAll(" ", "");
                if (s.startsWith("#-type-")) {
                    if (!s.endsWith(";")) {
                        lines[i] += ";";
                    }
                } else if (s.startsWith("#-list-")) {
                    if (!s.endsWith(";")) {
                        lines[i] += ";";
                    }
                } else if (s.startsWith("#")) {
                    lines[i] = "";
                }
            }

            return lines.join(System.getProperty("line.separator"));
        }
        return toClean;
    }

    public void setToParse(String toParse) {
        toParse = cleanComments(toParse)
        toParse = toParse.replaceAll("[\n\r]", "");
        this.toParse = initialString = toParse;
    }

    public QLQuery buildQuery() throws QLParserException {
        currentPosition.clear();
        elevation = 0;
        if (toParse == null || toParse.isEmpty()) {
            throw new QLParserException("No string provided to be parsed");
        }
        parseFragments(initialString);
        parseQuery();

        return query;
    }

    private void parseFragments(String searchString) throws QLParserException {
        Pattern pattern = Pattern.compile(FRAGMENT_KEYWORD + "\\s[a-zA-Z0-9]*\\son\\s[a-zA-Z0-9]*[\\s]?\\{");
        Matcher matcher = pattern.matcher(searchString);
        if (matcher.find()) {
            int beginIndex = matcher.start();
            String fragmentString = blockFetch(searchString, beginIndex);
            processFragment(fragmentString);

            fragments.get(fragments.size() - 1).setChildren(currentPosition.get(0).getChildren());
            String endString = initialString.substring(beginIndex + fragmentString.length());
            initialString = initialString.substring(0, beginIndex) + endString;
            parseFragments(initialString);
        }
    }

    private void processFragment(String fragmentString) throws QLParserException {
        isFragmentField = true;
        initTreeBulding();
        toParse = fragmentString;
        getHeader();
        QLNode childrePlaceHolder = new QLNode("tmp");
        parseBody(-1, childrePlaceHolder);
    }

    private void initTreeBulding() {
        elevation = 0;
        currentPosition.clear();
    }

    private void getHeader() throws QLParserException {
        int endIndex = toParse.indexOf("{");
        if (endIndex < 0) {
            this.query = new QLQuery();
            String message = "No block found in the string provided : \"" + toParse + "\", cannot" +
                    " create QLQuery";
            throw new QLParserException(message);
        }

        String substring = toParse.substring(0, endIndex);
        if (substring.startsWith(QUERY_KEYWORD)) {
            parseQueryHeader(substring);
        } else if (substring.startsWith(FRAGMENT_KEYWORD)) {
            parseFragmentHeader(substring);
        }
        else if (substring.length() == 0) {
            parseQueryHeader("");
        }

        trimString(endIndex + 1);
        this.toParse = toParse.replaceAll(" ", "");
    }

    private void parseQueryHeader(String substring) {
        substring = substring.replace(QUERY_KEYWORD, "");
        substring = substring.replaceAll(" ", "");
        QLElement element = createElementFromString(substring);
        element.setList(isList);
        this.query = new QLQuery(
                element.getName() != null && element.getName().length() > 0 ? element.getName() : null
        );
        query.setFragments(fragments);
        List<QLVariablesElement> params = new ArrayList<>();
        for (String key: element.getParameters().keySet()) {
            Object o = element.getParameters().get(key);
            if (o instanceof QLVariablesElement) {
                params.add((QLVariablesElement) o);
            }
        }
        this.query.setParameters(params);
    }

    private void parseQuery() throws QLParserException {
        toParse = initialString;
        isFragmentField = false;
        initTreeBulding();
        getHeader();

        QLNode childrePlaceHolder = new QLNode("tmp");
        parseBody(-1, childrePlaceHolder);
    }

    private void parseFragmentHeader(String substring) throws QLParserException {
        String[] fragmentHeader = substring.split(" ");

        if (fragmentHeader.length != 4)  {
            throw new QLParserException("Not a valid fragment header : should be : \"framgent [name] on [target]\", found : \"" + substring + "\"");
        }
        fragments.add(new QLFragment(fragmentHeader[1], fragmentHeader[3]));
    }

    private void parseBody(int endIndex, QLNode node) {
        currentPosition.put(elevation,  node);
        elevation ++;
        trimString(endIndex + 1);

        processNextField();
    }

    private void processNextField() {

        if (toParse.startsWith(",")) {
            toParse = toParse.substring(1);
        }

        if (toParse.length() <= 0) {
            return;
        }

        if (!shouldAvoidReset) {
            typeBuffer = null;
            isList = false;
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


    private void handleCommentary(int endCommentary) {
        String typeString = toParse.subSequence(0, endCommentary);
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
        shouldAvoidReset = true;
        trimString(endCommentary + 1);

        processNextField();
    }

    private void handleClosingCurly() {
        elevation--;
        if (elevation < 0) {
            return;
        }
        if (elevation == 0) {
            if (isFragmentField) {
                fragments.get(fragments.size() - 1).setChildren(currentPosition.get(0).getChildren());
            } else {
                query.setQueryFields(currentPosition.get(elevation).getChildren());
                currentPosition.clear();
            }
        }
        trimString(1);

        processNextField();
    }

    private void handleSimpleField(int nextCommaIndex) {
        QLElement field = createElementFromString(toParse.substring(0, nextCommaIndex));
        field.setList(isList);
        field = checkIfDirective(field);
        trimString(nextCommaIndex + 1);
        currentPosition.get(elevation - 1).addChild(new QLLeaf(field, typeBuffer));
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
        String includeArtifact = "@include(if:"
        String skipArtifact = "@skip(if:"
        String currentToParse = toParse;
        if (currentToParse.contains(includeArtifact)) {
            int startOfInclude = currentToParse.indexOf(includeArtifact)
            int closeIncludeIndex = currentToParse.indexOf(")", startOfInclude);
            String variableName = currentToParse.substring(startOfInclude + includeArtifact.length(), closeIncludeIndex)
            qlElement.setInclude(variableName)
        }
        if (currentToParse.contains(skipArtifact)) {
            int startOfSkip = currentToParse.indexOf(skipArtifact)
            int closeSkipIndex = currentToParse.indexOf(")", startOfSkip);
            String variableName = currentToParse.substring(currentToParse.indexOf(skipArtifact) + skipArtifact.length(), closeSkipIndex)
            qlElement.setSkip(variableName)
        }


        return qlElement;
    }


    private void handleFragmentImport() {
        int begin = toParse.indexOf("...");
        String fragmentName = toParse.substring(begin + 3, delimiter.endCarret);
        currentPosition.get(elevation - 1).addChild(new QLFragmentNode(fragmentName));

        trimString(delimiter.endCarret);
        processNextField();
    }

    private void handleNode(int nextCurlyIndex) {
        QLElement field = createElementFromString(toParse.substring(0, nextCurlyIndex));
        field.setList(isList);
        field = checkIfDirective(field);
        QLNode childNode = new QLNode(field);
        if (elevation > 0) {
            currentPosition.get(elevation - 1).addChild(childNode);
        }

        parseBody(nextCurlyIndex, childNode);
    }


    private void handleLastField(int endCarret) {
        QLElement field = createElementFromString(toParse.substring(0, endCarret));
        field.setList(isList);
        field = checkIfDirective(field);
        currentPosition.get(elevation - 1).addChild(new QLLeaf(field, typeBuffer));
        trimString(endCarret);

        processNextField();
    }

    private QLElement createElementFromString(String substring) {
        substring.replaceAll(" ", "");
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

    private QLElement getFieldName(String s) {
        QLElement element;
        String name = s;


        Pattern pattern = Pattern.compile("@include|@skip");
        Matcher matcher = pattern.matcher(name);
        if (matcher.find()) {
            name = name.substring(0, matcher.start())
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

    private Map<String, Object> getParameters(String stringParameters) {
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
                if (unit[0].charAt(0) == '$') {
                    params.put(unit[0], parseVariableType(unit));
                } else if (unit[1].charAt(0) == '$') {
                    params.put(unit[0], new QLVariablesElement(unit[1].replace('$', "")));
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
        return params;
    }

    private QLVariablesElement parseVariableType(String[] unit) {
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
        element.setName(unit[0].replace('$',""));
        element.type = parseType(unit[1].substring(0, endTypeIndex))
        return element;
    }

    private QLType parseType(String type) {
        switch (type) {
            case "Boolean":
                return QLType.BOOLEAN;
                break;
            case "String":
                return QLType.STRING;
                break;
            case "Int":
                return QLType.INT;
                break;
            case "ID":
                return QLType.ID;
                break;
            case "Float":
                return QLType.FLOAT;
                break;
            default:
                // TODO (kelianclerc) 23/5/17 error or enum
                return QLType.STRING;
                break;
        }
    }

    private String blockFetch(String globalString, String beginString) {
        int beginIndex = globalString.indexOf(beginString);
        return blockFetch(globalString, beginIndex);
    }

    private String blockFetch(String globalString, int beginIndex) {
        String substring = globalString.substring(beginIndex);
        int localElevation = 0;
        boolean firstOpening = true;
        for (int i = 0; i < substring.length(); i++) {
            if (substring.charAt(i) == '{') {
                if (firstOpening) {
                    firstOpening = false;
                }
                localElevation++;
            } else if (substring.charAt(i) == '}') {
                localElevation--;
            }

            if (!firstOpening) {
                if (localElevation == 0) {
                    return globalString.substring(beginIndex, beginIndex + i + 1);
                }
            }
        }
        return globalString;
    }

    private void trimString(int start) {
        this.toParse = toParse.substring(start);
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
