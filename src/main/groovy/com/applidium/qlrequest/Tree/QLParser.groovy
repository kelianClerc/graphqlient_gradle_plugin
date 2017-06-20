package com.applidium.qlrequest.Tree

import com.applidium.qlrequest.Query.*
import com.applidium.qlrequest.exceptions.QLParserException

import java.util.regex.Matcher
import java.util.regex.Pattern

public class QLParser {

    public static final String QUERY_KEYWORD = "query";
    public static final String MUTATION_KEYWORD = "mutation";
    public static final String FRAGMENT_KEYWORD = "fragment";
    private String initialString;
    private String toParse;
    private QLQuery query;
    private List<QLElement> toUpdate;
    private List<QLElement> parentOfToUpdate;
    private List<QLFragment> fragments;

    public QLParser() {
        fragments = new ArrayList<>();
        toUpdate = new ArrayList<>();
        parentOfToUpdate = new ArrayList<>();
    }

    public QLParser(String toParse) {
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
            QLNode fragmentContent = processFragment(fragmentString);

            fragments.get(fragments.size() - 1).setChildren(fragmentContent.getChildren());
            String endString = initialString.substring(beginIndex + fragmentString.length());
            initialString = initialString.substring(0, beginIndex) + endString;
            parseFragments(initialString);
        }
    }

    private QLNode processFragment(String fragmentString) throws QLParserException {
        toParse = fragmentString;
        getHeader();
        QLNode childrenPlaceHolder = new QLNode("tmp");
        QLHandler handler = new QLHandler(toParse, true);
        handler.initTreeBuilding();
        return handler.parseBody(-1, childrenPlaceHolder);
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

        Pattern pattern = Pattern.compile("(" + QUERY_KEYWORD + "| " + MUTATION_KEYWORD + ")");
        if (substring.startsWith(pattern.pattern())) {
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
        boolean isMutation;

        if (substring.startsWith(QUERY_KEYWORD)) {
            substring = substring.replace(QUERY_KEYWORD, "");
            isMutation = false;
        } else if (substring.startsWith(MUTATION_KEYWORD)) {
            substring = substring.replace(MUTATION_KEYWORD, "");
            isMutation = true;
        }
        this.query.isMutation(isMutation);

        substring = substring.replaceAll(" ", "");
        QLElement element = QLHandler.createElementFromString(substring);
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
        getHeader();

        QLHandler queryHandler = new QLHandler(toParse, false);
        queryHandler.initTreeBuilding();

        QLNode childrePlaceHolder = new QLNode("tmp");
        queryHandler.parseBody(-1, childrePlaceHolder);
        query.setQueryFields(childrePlaceHolder.getChildren());
    }

    private void parseFragmentHeader(String substring) throws QLParserException {
        String[] fragmentHeader = substring.split(" ");

        if (fragmentHeader.length != 4)  {
            throw new QLParserException("Not a valid fragment header : should be : \"framgent [name] on [target]\", found : \"" + substring + "\"");
        }
        fragments.add(new QLFragment(fragmentHeader[1], fragmentHeader[3]));
    }

    public static String blockFetch(String globalString, String beginString) {
        int beginIndex = globalString.indexOf(beginString);
        return blockFetch(globalString, beginIndex);
    }

    public static String blockFetch(String globalString, int beginIndex) {
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


}
