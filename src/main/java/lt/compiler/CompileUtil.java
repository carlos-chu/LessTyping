package lt.compiler;

import lt.compiler.lexical.Element;
import lt.compiler.lexical.ElementStartNode;
import lt.compiler.lexical.EndingNode;
import lt.compiler.lexical.Node;
import lt.compiler.syntactic.pre.Modifier;

import java.util.*;

/**
 * compile util<br>
 * checks the element type<br>
 * and defines keywords and operator priority
 */
public class CompileUtil {
        /**
         * check the given string is a number
         *
         * @param str string
         * @return true if the string represents a number
         */
        public static boolean isNumber(String str) {
                try {
                        Double.parseDouble(str);
                        return true;
                } catch (NumberFormatException ignore) {
                        return false;
                }
        }

        /**
         * check the given string is a boolean literal
         *
         * @param str string
         * @return true if it's a boolean.(true,false,yes,no)
         */
        public static boolean isBoolean(String str) {
                return str.equals("true") || str.equals("false") || str.equals("yes") || str.equals("no");
        }

        /**
         * check the given string is a string literal
         *
         * @param str string
         * @return true/false
         */
        public static boolean isString(String str) {
                return (str.startsWith("\"") && str.endsWith("\"")) || (str.startsWith("'") && str.endsWith("'"));
        }

        private static Set<String> keys = new HashSet<>(Arrays.asList(
                "is", "bool", "yes", "no", "type", "as", "undefined"
        ));

        private static Set<String> javaKeys = new HashSet<>(Arrays.asList(
                "abstract", "assert", "boolean", "break", "byte", "case",
                "catch", "char", "class", "const", "continue", "default",
                "do", "double", "else", "enum", "extends", "final", "finally",
                "float", "for", "if", "implements", "import", "instanceof",
                "int", "interface", "long", "native", "new", "package",
                "private", "protected", "public", "return", "short", "static",
                "strictfp", "null"
        ));

        /**
         * check whether the given string can be a valid java name
         *
         * @param str string
         * @return true/false
         */
        public static boolean isJavaValidName(String str) {
                if (str.isEmpty()) return false;
                if (javaKeys.contains(str)) return false;
                char first = str.charAt(0);
                if (isValidNameStartChar(first)) {
                        for (int i = 1; i < str.length(); ++i) {
                                char c = str.charAt(i);
                                if (!isValidNameChar(c)) return false;
                        }
                        return true;
                } else {
                        return false;
                }
        }

        /**
         * check whether the given string can be a name
         *
         * @param str string
         * @return true/false
         */
        public static boolean isValidName(String str) {
                if (str.startsWith("`") && str.endsWith("`")) {
                        return isJavaValidName(str.substring(1, str.length() - 1));
                }
                return isJavaValidName(str) && !keys.contains(str);
        }

        /**
         * check whether is defined as a package access
         *
         * @param element element
         * @return true/false (packageName::name)
         */
        public static boolean isPackage(Element element) {
                String content = element.getContent();
                if (isValidName(content) && element.hasNext()) {
                        Node next = element.next();
                        if (next instanceof Element) {
                                String nextContent = ((Element) next).getContent();
                                if (nextContent.equals("::") && next.hasNext()) {
                                        Node nextNext = next.next();
                                        if (nextNext instanceof Element) {
                                                return isValidName(((Element) nextNext).getContent());
                                        }
                                }
                        }
                }
                return false;
        }

        /**
         * check whether the given char can be one of a name
         *
         * @param c char
         * @return true/false (a-z|A-Z|$|_|0-9)
         */
        public static boolean isValidNameChar(char c) {
                return isValidNameStartChar(c) || (c >= '0' && c <= '9');
        }

        /**
         * check whether the given char can be start of a name
         *
         * @param c char
         * @return true/false (a-z|A-Z|$|_)
         */
        public static boolean isValidNameStartChar(char c) {
                return (c >= 'A' && c <= 'Z') || (c >= 'a' && c <= 'z') || c == '$' || c == '_';
        }

        private static Set<String> modifiers = new HashSet<>(Arrays.asList(
                "pub", "pro", "pri", "pkg",
                "abs", "val", "native", "sync", "transient", "volatile", "strictfp",
                "data"
        ));

        private static Set<String> accessModifiers = new HashSet<>(Arrays.asList(
                "pub", "pro", "pri", "pkg"
        ));

        public static boolean isModifier(String str) {
                return modifiers.contains(str);
        }

        /**
         * check whether the str represented modifier is compatible with existing modifiers
         *
         * @param str       str
         * @param modifiers modifiers
         * @return true/false
         */
        public static boolean modifierIsCompatible(String str, Set<Modifier> modifiers) {
                boolean isAccessMod = accessModifiers.contains(str);
                for (Modifier m : modifiers) {
                        if (m.modifier.equals(str)
                                || (isAccessMod && accessModifiers.contains(m.modifier))
                                || (str.equals("val") && m.modifier.equals("abs"))
                                || (str.equals("abs") && m.modifier.equals("val")))
                                return false;
                }
                return true;
        }

        public static final int NOT_METHOD_DEF = 0;
        public static final int METHOD_DEF_NORMAL = 1;
        public static final int METHOD_DEF_TYPE = 2;
        public static final int METHOD_DEF_EMPTY = 3;
        public static final int METHOD_DEF_ONE_STMT = 4;

        public static Node get_next_node(Node n) {
                if (n == null) return null;

                if (n.next() instanceof EndingNode) {
                        return get_next_node(n.next());
                } else {
                        return n.next();
                }
        }

        public static boolean isLambda(Element elem) throws UnexpectedEndException {
                if (elem.getContent().equals("(")) {
                        Node n = get_next_node(elem);
                        if (n instanceof ElementStartNode) {
                                n = get_next_node(n);
                        }
                        if (n instanceof Element) {
                                if (((Element) n).getContent().equals(")")) {
                                        n = get_next_node(n);
                                        if (n instanceof Element && ((Element) n).getContent().equals("->")) {
                                                return true;
                                        }
                                }
                        }
                }
                return false;
        }

        public static int checkMethodDef(Element elem) throws UnexpectedEndException {
                String content = elem.getContent();
                if (isValidName(content)) {
                        Node nodeAfterRightPar = null;

                        // method
                        Node n1 = get_next_node(elem);
                        if (n1 instanceof Element) {
                                String p = ((Element) n1).getContent();
                                if (p.equals("(")) {
                                        Node n2 = get_next_node(n1);
                                        if (n2 instanceof ElementStartNode) {
                                                // method(口
                                                Node n3 = get_next_node(n2);
                                                if (n3 instanceof Element) {
                                                        // method(口)
                                                        if (((Element) n3).getContent().equals(")")) {
                                                                nodeAfterRightPar = get_next_node(n3);
                                                        }
                                                }
                                        } else if (n2 instanceof Element) {
                                                // method()
                                                if (((Element) n2).getContent().equals(")")) {
                                                        nodeAfterRightPar = get_next_node(n2);
                                                }
                                        }
                                }
                        }

                        if (nodeAfterRightPar != null) {
                                if (nodeAfterRightPar instanceof ElementStartNode) {
                                        return METHOD_DEF_NORMAL;
                                } else if (nodeAfterRightPar instanceof Element) {
                                        String s = ((Element) nodeAfterRightPar).getContent();
                                        if (s.equals(":")) {
                                                return METHOD_DEF_TYPE;
                                        } else if (s.equals("=")) {
                                                Node nn = get_next_node(nodeAfterRightPar);
                                                if (nn instanceof Element) {
                                                        if (((Element) nn).getContent().equals("..."))
                                                                return METHOD_DEF_EMPTY;
                                                        else
                                                                return METHOD_DEF_ONE_STMT;
                                                }
                                        }
                                }
                        }
                }
                return NOT_METHOD_DEF;
        }

        private static Set<String> twoVarOperators;

        public static boolean isTwoVariableOperator(String str) {
                return twoVarOperators.contains(str);
        }

        private static Set<String> oneVarOperatorsPost = new HashSet<>(Arrays.asList(
                "++", "--"
        ));

        public static boolean isOneVariableOperatorPost(String str) {
                return oneVarOperatorsPost.contains(str);
        }

        private static Set<String> oneVarOperatorsPreWithoutCheckingExps = new HashSet<>(Arrays.asList(
                "!", "~"
        ));

        private static Set<String> oneVarOperatorsPreMustCheckExps = new HashSet<>(Arrays.asList(
                "++", "--", "!", "~", "+", "-"
        ));

        public static boolean isOneVariableOperatorPreWithoutCheckingExps(String str) {
                return oneVarOperatorsPreWithoutCheckingExps.contains(str);
        }

        public static boolean isOneVariableOperatorPreMustCheckExps(String str) {
                return oneVarOperatorsPreMustCheckExps.contains(str);
        }

        private static String[][] twoVar_priority = {
                // 1..5 means [1,2,3,4,5]
                // 1.:5 means [1,2,3,4]
                {"..", ".:"},
                {"^^"}, // pow
                {"*", "/", "%"},
                {"+", "-"},
                {"<<", ">>", ">>>"},
                {">", "<", ">=", "<="},
                {"==", "!=", "===", "!==", "=:=", "!:=", "is", "not", "in"},
                {"&"},
                {"^"},
                {"|"},
                {"&&", "and"},
                {"||", "or"}
        };

        /**
         * a higher or equal to b
         *
         * @param a a
         * @param b b
         * @return true if a higher or equal to b
         */
        public static boolean twoVar_higherOrEqual(String a, String b) {
                int indexA = find_twoVar_priority(a);
                if (indexA == -1) {
                        if (isValidName(a)) {
                                indexA = twoVar_priority.length;
                        } else throw new IllegalArgumentException(a + " is not valid two variable operator");
                }
                int indexB = find_twoVar_priority(b);
                if (indexB == -1) {
                        if (isValidName(b)) {
                                indexB = twoVar_priority.length;
                        } else throw new IllegalArgumentException(b + " is not valid two variable operator");
                }
                return indexA <= indexB;
        }

        private static int find_twoVar_priority(String s) {
                for (int i = 0; i < twoVar_priority.length; ++i) {
                        String[] arr = twoVar_priority[i];
                        for (String anArr : arr) {
                                if (anArr.equals(s)) {
                                        return i;
                                }
                        }
                }
                return -1;
        }

        public static void expecting(String token, Node previous, Node got) throws UnexpectedTokenException, UnexpectedEndException {
                if (got == null) {
                        throw new UnexpectedEndException(previous.getLineCol());
                } else if (!(got instanceof Element)) {
                        throw new UnexpectedTokenException(token, got.getClass().getSimpleName(), got.getLineCol());
                } else if (!((Element) got).getContent().endsWith(token)) {
                        throw new UnexpectedTokenException(token, ((Element) got).getContent(), got.getLineCol());
                }
        }

        public static boolean isAssign(String s) {
                return s.equals("=") || s.equals("+=") || s.equals("-=") || s.equals("*=") || s.equals("/=") || s.equals("%=");
        }

        public static boolean isSync(Element elem) {
                String content = elem.getContent();
                if (content.equals("sync")) {
                        Node n = get_next_node(elem);
                        if (n instanceof Element) {
                                String s = ((Element) n).getContent();
                                if (s.equals("(")) {
                                        return true;
                                }
                        }
                }
                return false;
        }

        private static Set<String> primitives = new HashSet<>(Arrays.asList(
                "int", "double", "float", "short", "long", "byte", "char", "bool"
        ));

        public static boolean isPrimitive(String s) {
                return primitives.contains(s);
        }

        static {
                // 2 var op
                twoVarOperators = new HashSet<>();
                for (String[] sArr : twoVar_priority) {
                        Collections.addAll(twoVarOperators, sArr);
                }

                keys.addAll(modifiers);
                keys.addAll(javaKeys);
        }

        public static String validateValidName(String validName) {
                if (validName.startsWith("`")) return validName.substring(1, validName.length() - 1);
                return validName;
        }
}