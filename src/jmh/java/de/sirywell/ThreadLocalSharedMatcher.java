package de.sirywell;

import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.State;
import spoon.Launcher;

import java.util.Collection;
import java.util.HashSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@State(Scope.Benchmark)
public class ThreadLocalSharedMatcher implements JLSCorrectnessChecker {

    private static Launcher launcher = new Launcher();

    private static final ThreadLocal<Matcher> IS_ARRAY_OR_INSTANCE = ThreadLocal.withInitial(
            () -> Pattern.compile("\\[\\]|@").matcher(""));
    private static final Pattern IS_INNER_OR_GENERIC = Pattern.compile("\\.|<|>");
    private static final ThreadLocal<Matcher> IS_INNER_OR_GENERIC_OR_EMPTY = ThreadLocal.withInitial(
            () -> Pattern.compile("\\d.*|<.*>|^.{0}$").matcher(""));
    private static final Collection<String> baseKeywords = fillWithBaseKeywords();
    private static final Collection<String> java2Keywords = Stream.of("strictfp").collect(Collectors.toCollection(HashSet::new));
    private static final Collection<String> java4Keywords = Stream.of("assert").collect(Collectors.toCollection(HashSet::new));
    private static final Collection<String> java5Keywords = Stream.of("enum").collect(Collectors.toCollection(HashSet::new));
    private static final Collection<String> java9Keywords = Stream.of("_").collect(Collectors.toCollection(HashSet::new));

    private void checkIdentiferForJLSCorrectness(String simplename) {
        /*
         * At the level of the Java Virtual Machine, every constructor written in the Java programming language (JLS §8.8)
         * appears as an instance initialization method that has the special name <init>.
         * This name is supplied by a compiler. Because the name is not a valid identifier,
         * it cannot be used directly in a program written in the Java programming language.
         */
        //JDTTreeBuilderHelper.computeAnonymousName returns "$numbers$Name" so we have to skip them if they start with numbers
        //allow empty identifier because they are sometimes used.
        if (!IS_INNER_OR_GENERIC_OR_EMPTY.get().reset(simplename).matches()) {
            //split at "<" and ">" because "Iterator<Cache.Entry<K,Store.ValueHolder<V>>>" submits setSimplename ("Cache.Entry<K")
            String[] splittedSimplename = IS_INNER_OR_GENERIC.split(simplename);
            if (checkAllParts(splittedSimplename)) {
                throw new spoon.SpoonException("Not allowed javaletter or keyword in identifier found. See JLS for correct identifier. Identifier: " + simplename);
            }
        }
    }

    /**
     * Keywords list and history selected according to:
     * https://docs.oracle.com/en/java/javase/15/docs/specs/sealed-classes-jls.html#jls-3.9
     * https://en.wikipedia.org/wiki/List_of_Java_keywords (contains history of revisions)
     * and https://docs.oracle.com/javase/tutorial/java/nutsandbolts/_keywords.html (history up to java 8)
     *
     * @param simplename
     * @return true if simplename is a keyword in the current setting (compliance level), false if not
     */
    private boolean isKeyword(String simplename) {
        int complianceLevel = launcher.getFactory().getEnvironment().getComplianceLevel();
        return (baseKeywords.contains(simplename)
                || (complianceLevel >= 2 && java2Keywords.contains(simplename))
                || (complianceLevel >= 4 && java4Keywords.contains(simplename))
                || (complianceLevel >= 5 && java5Keywords.contains(simplename))
                || (complianceLevel >= 9 && java9Keywords.contains(simplename)));
    }

    private boolean checkAllParts(String[] simplenameParts) {
        for (String simpleName:simplenameParts) {
            //because arrays use e.g. int[] and @Number is used for instances of an object e.g. foo@1
            simpleName = IS_ARRAY_OR_INSTANCE.get().reset(simpleName).replaceAll("");
            if (isWildCard(simpleName)) {
                // because in intersection types a typeReference sometimes has '?' as simplename
                return false;
            }
            if (isKeyword(simpleName) || checkIdentifierChars(simpleName)) {
                return true;
            }
        }
        return false;
    }
    private boolean checkIdentifierChars(String simplename) {
        if (simplename.length() == 0) {
            return false;
        }
        return (!Character.isJavaIdentifierStart(simplename.charAt(0)))
                || simplename.chars().anyMatch(letter -> !Character.isJavaIdentifierPart(letter)
        );
    }
    private boolean isWildCard(String name) {
        return name.equals("?");
    }


    private static Collection<String> fillWithBaseKeywords() {
        // removed types because needed as ref: "int","short", "char", "void", "byte","float", "true","false","boolean","double","long","class", "null"
        // in the method isKeyword, more keywords are added to the checks based on the compliance level
        return Stream.of("abstract", "continue", "for", "new", "switch", "default", "if", "package", "synchronized",  "do", "goto", "private",
                        "this", "break",  "implements", "protected", "throw", "else", "import", "public", "throws", "case", "instanceof", "return",
                        "transient", "catch", "extends", "try", "final", "interface", "static", "finally", "volatile",
                        "const",  "native", "super", "while")
                .collect(Collectors.toCollection(HashSet::new));
    }
    @Override
    public void checkForCorrectness(String name) {
        checkIdentiferForJLSCorrectness(name);
    }
}
