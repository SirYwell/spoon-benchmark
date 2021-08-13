package de.sirywell;

import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Param;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.Threads;
import org.openjdk.jmh.annotations.Warmup;

import java.util.concurrent.TimeUnit;

@Warmup(iterations = 3)
@Threads(value = 4) // 4 threads in parallel per benchmark, especially to test the ThreadLocal performance
@State(Scope.Thread)
@Fork(value = 1) // it takes a lot of time already...
@Measurement(iterations = 4)
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
public class SpoonCheckIdentifierBenchmark {

    // some (valid) parameters we want to test performance with
    @Param({"Hi<T.R>[]@", "HelloWorld", "VeryLongValidKeyword123<A.B.C.D.E.F>[][][]@"})
    private String stringToCheck;

    @Benchmark
    public void testOldImpl(OldImplementation implementation) {
        implementation.checkForCorrectness(stringToCheck);
    }

    @Benchmark
    public void testPrecompiledPatternImpl(PrecompiledPatterns implementation) {
        implementation.checkForCorrectness(stringToCheck);
    }

    @Benchmark
    public void testThreadLocalMatcherImpl(ThreadLocalSharedMatcher implementation) {
        implementation.checkForCorrectness(stringToCheck);
    }

    @Benchmark
    public void testHandMadeCheckerImpl(HandMadeChecker implementation) {
        implementation.checkForCorrectness(stringToCheck);
    }

    // just a lazy way to see what's allowed and what isn't
    public static void main(String[] args) {
        String s = "<<>>...@@@";
        try {
            new OldImplementation().checkForCorrectness(s);
            System.out.println("old success");
        } catch (Exception e) {
            e.printStackTrace();
        }
        try {
            new HandMadeChecker().checkForCorrectness(s);
            System.out.println("new success");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
