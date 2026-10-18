package com.nsysmon.servlet.environment;

import com.ajjpj.afoundation.collection.immutable.AList;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Documents the behaviour of {@link PathMatcher} as it is implemented. Note that a pattern without a trailing
 *  <code>*</code> never matches, not even for a path of exactly the same length - see
 *  {@link #neverMatchesWhenThePatternHasNoSuffixWildcard()}.
 */
class PathMatcherTest {

    private static boolean matches(String pattern, String... path) {
        return PathMatcher.create(pattern).get(0).matches(AList.create(path));
    }

    @Test void suffixWildcardMatchesTheRest() {
        assertTrue(matches("java.*", "java", "a", "b", "c"));
    }

    @Test void suffixWildcardMatchesASingleSegmentToo() {
        assertTrue(matches("java.*", "java"));
    }

    @Test void leadingSuffixWildcardMatchesEverything() {
        assertTrue(matches("*", "any", "thing"));
    }

    @Test void rejectsPathShorterThanTheLiteralPartOfThePattern() {
        assertFalse(matches("a.b.*", "a"));
    }

    @Test void neverMatchesWhenThePatternHasNoSuffixWildcard() {
        assertFalse(matches("java.version", "java", "version"));
    }

    @Test void rejectsDifferentSegmentBeforeTheSuffixWildcard() {
        assertFalse(matches("java.*", "os", "name"));
    }

    @Test void singleSegmentWildcardAloneDoesNotMakeThePatternMatch() {
        assertFalse(matches("java.?.version", "java", "anything", "version"));
    }

    @Test void rejectsPathShorterThanPattern() {
        assertFalse(matches("java.specification.version", "java", "version"));
    }

    @Test void parsesSeveralPatternsSeparatedByDoublePipe() {
        final List<PathMatcher> matchers = PathMatcher.create(" java.* || os.* ");

        assertTrue(matchers.get(0).matches(AList.create("java", "home")));
        assertFalse(matchers.get(0).matches(AList.create("os", "name")));
        assertTrue(matchers.get(1).matches(AList.create("os", "name")));
    }
}
