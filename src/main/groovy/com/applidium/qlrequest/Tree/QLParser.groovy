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
        //getRootElement();
    }

    private void parseFragmentHeader(String substring) throws QLParserException {
        String[] fragmentHeader = substring.split(" ");

        if (fragmentHeader.length != 4)  {
            throw new QLParserException("Not a valid fragment header : should be : \"framgent [name] on [target]\", found : \"" + substring + "\"");
        }
        fragments.add(new QLFragment(fragmentHeader[1], fragmentHeader[3]));
    }

    private void getRootElement() {
        if (toParse.startsWith("#-list-")) {
            def closing = toParse.indexOf(";")
            isList = true
            trimString(closing + 1)
        }


        int endIndex = toParse.indexOf("{");
        if (endIndex < 0) {
            return;
        }

        String substring = toParse.substring(0, endIndex);
        substring = substring.replaceAll(" ", "");

        QLElement element = createElementFromString(substring);
        element.setList(isList);
        isList = false;
        QLNode node = new QLNode(element);
        parseBody(endIndex, node);
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
            }
            else if (delimiter.isNextSimpleField()) {
                handleSimpleField(delimiter.endCarret);
            }
            else if (delimiter.isNextFieldWithParameters()) {
                handleFieldWithParameters(delimiter.endCarret);
            }
            else if (delimiter.isNextNodeWithoutParams()) {
                handleNodeWithoutParameter(delimiter.endCarret);
            }
            else if (delimiter.isNextLastField()) {
                handleLastField(delimiter.endCarret);
            }
            else if (delimiter.isNextFragmentImport()) {
                handleFragmentImport();
            }
        }
    }

    private void handleFragmentImport() {
        int begin = toParse.indexOf("...");
        String fragmentName = toParse.substring(begin + 3, delimiter.endCarret);
        currentPosition.get(elevation - 1).addChild(new QLFragmentNode(fragmentName));

        trimString(delimiter.endCarret);
        processNextField();
    }

    private List<QLElement> findFragmentByName(String fragmentName) {
        fragmentName = fragmentName.replace("...", "");
        for (QLFragment frag: fragments) {
            if (frag.getName().equals(fragmentName)) {
                return frag.getChildren();
            }
        }
        return Collections.emptyList();
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
        trimString(nextCommaIndex + 1);
        currentPosition.get(elevation - 1).addChild(new QLLeaf(field, typeBuffer));
        processNextField();
    }

    private void handleFieldWithParameters(int nextCloseBraceIndex) {
        QLElement field = createElementFromString(toParse.substring(0, nextCloseBraceIndex + 1));
        field.setList(isList);
        trimString(nextCloseBraceIndex + 1);
        if (toParse.charAt(0) == '{') {
            QLNode childNode = new QLNode(field);
            if (elevation > 0) {
                currentPosition.get(elevation - 1).addChild(childNode);
            }
            currentPosition.put(elevation, childNode);
            elevation++;
            trimString(1);
        }
        else {
            currentPosition.get(elevation - 1).addChild(new QLLeaf(field, typeBuffer));
            if (toParse.charAt(0) == ',') {
                trimString(1);
            }
        }

        processNextField();
    }

    private void handleNodeWithoutParameter(int nextCurlyIndex) {
        QLElement field = createElementFromString(toParse.substring(0, nextCurlyIndex));
        field.setList(isList);
        QLNode childNode = new QLNode(field);
        if (elevation > 0) {
            currentPosition.get(elevation - 1).addChild(childNode);
        }

        parseBody(nextCurlyIndex, childNode);
    }

    private void handleLastField(int endCarret) {
        QLElement field = createElementFromString(toParse.substring(0, endCarret));
        field.setList(isList);
        currentPosition.get(elevation - 1).addChild(new QLLeaf(field, typeBuffer));
        trimString(endCarret);

        processNextField();
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

    private QLElement createElementFromString(String substring) {
        substring.replaceAll(" ", "");
        if (substring.length() > 0) {
            String[] stringList = substring.split("[(]");

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
        stringParameters = stringParameters.replace(")", "");
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
        private int nextCloseBraceIndex;
        private int nextBraceIndex;
        private int nextCommaIndex;
        private int endCarret;
        private int nextFragmentImportIndex;
        private int nextCommentary;
        private int nextEndCommentary;

        public QueryDelimiter() {
        }

        public void analyze(String toAnalyze) {
            nextCommaIndex = toAnalyze.indexOf(",");
            nextBraceIndex = toAnalyze.indexOf("(");
            nextCloseBraceIndex = toAnalyze.indexOf(")");
            nextCurlyIndex = toAnalyze.indexOf("{");
            nextCloseCurlyIndex = toAnalyze.indexOf("}");
            nextFragmentImportIndex = toAnalyze.indexOf("...");
            nextEndCommentary = toAnalyze.indexOf(";");

            nextCommaIndex = ifNegativeMakeGreat(nextCommaIndex);
            nextBraceIndex = ifNegativeMakeGreat(nextBraceIndex);
            nextCurlyIndex = ifNegativeMakeGreat(nextCurlyIndex);
            nextCloseBraceIndex = ifNegativeMakeGreat(nextCloseBraceIndex);
            nextCloseCurlyIndex = ifNegativeMakeGreat(nextCloseCurlyIndex);
            nextFragmentImportIndex = ifNegativeMakeGreat(nextFragmentImportIndex);
            nextCommentary = Math.min(ifNegativeMakeGreat(toAnalyze.indexOf("#-type-")),ifNegativeMakeGreat(toAnalyze.indexOf("#-list-")));
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

        public boolean isNextSimpleField() {
            boolean b = isTheNextOccurance(nextCommaIndex);
            if (b) {
                endCarret = nextCommaIndex;
            }
            return b;
        }

        public boolean isNextFieldWithParameters() {
            boolean b = isTheNextOccurance(nextBraceIndex);
            if (b) {
                endCarret = nextCloseBraceIndex;
            }
            return b;
        }

        public boolean isNextNodeWithoutParams() {
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

        private boolean isTheNextOccurance(int target) {
            return Math.min(
                    target,
                    Math.min(nextCommaIndex,
                            Math.min(nextBraceIndex,
                                    Math.min(nextCloseBraceIndex,
                                            Math.min(nextCurlyIndex,
                                                    Math.min(nextCommentary,
                                                        Math.min(nextCloseCurlyIndex, nextFragmentImportIndex)
                                                    )
                                            )
                                    )
                            )
                    )
            ) == target;
        }

        public boolean isNextCommentary() {
            boolean b = isTheNextOccurance(nextCommentary);
            if (b) {
                endCarret = nextEndCommentary;
            }
            return b;
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
