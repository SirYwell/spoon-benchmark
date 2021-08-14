package de.sirywell;

import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.State;
import spoon.Launcher;
import spoon.SpoonException;

import java.util.Collection;
import java.util.HashSet;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@State(Scope.Benchmark)
public class HandMadeChecker implements JLSCorrectnessChecker {
    private static Launcher launcher = new Launcher();

    private static final Collection<String> baseKeywords = fillWithBaseKeywords();
    private static final Collection<String> java2Keywords = Stream.of("strictfp").collect(Collectors.toCollection(HashSet::new));
    private static final Collection<String> java4Keywords = Stream.of("assert").collect(Collectors.toCollection(HashSet::new));
    private static final Collection<String> java5Keywords = Stream.of("enum").collect(Collectors.toCollection(HashSet::new));
    private static final Collection<String> java9Keywords = Stream.of("_").collect(Collectors.toCollection(HashSet::new));

    private void checkIdentifierForJLSCorrectness(String simplename) {
        if (isSpecialType(simplename)) return;
        if (!checkAll(simplename)) {
            throw new SpoonException("Not allowed javaletter or keyword in identifier found. See JLS for correct identifier. Identifier: " + simplename);
        }
    }

    private boolean checkAll(String name) {
        int i = 0;
        // leading digits come from anonymous/local classes. Skip them
        while (i < name.length() && Character.isDigit(name.charAt(i))) {
            i++;
        }
        int start = i;
        char expectNext = 0; // 0 = do not expect anything
        for (; i < name.length(); i++) {
            if (expectNext != 0) {
                if (name.charAt(i) != expectNext) {
                    return false;
                } else if (name.charAt(i) == expectNext) {
                    expectNext = 0; // reset
                    continue; // skip it, no further checks required
                }
            }
            switch (name.charAt(i)) {
                case '.':
                case '<':
                case '>':
                    if (isKeyword(name.substring(start, i))) return false; // keyword -> not allowed
                    start = i + 1; // skip this special char
                    break;
                case '[':
                    expectNext = ']'; // next char *must* close
                    break;
                default: // if we come across an illegal java identifier char here, it's not valid at all
                    if (start == i) { // first char of a part
                        if (!Character.isJavaIdentifierStart(name.charAt(i))) {
                            return false;
                        }
                    } else {
                        if (!Character.isJavaIdentifierPart(name.charAt(i))) {
                            return false;
                        }
                    }
                    break;
            }
        }
        if (expectNext != 0) {
            return false; // expected something that didn't appear anymore
        }
        if (start < name.length()) {
            return !isKeyword(name.substring(start));
        }
        return true;
    }

    private static boolean isSpecialType(String identifier) {
        return identifier.isEmpty()
                || "?".equals(identifier) // is wildcard
                || (identifier.startsWith("<") && identifier.endsWith(">"));
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
        checkIdentifierForJLSCorrectness(name);
    }
}
