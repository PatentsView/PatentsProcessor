package lodi;

import org.junit.*;
import static org.junit.Assert.assertThat;
import static org.hamcrest.CoreMatchers.*;

public class ManualReplacementsTest {

    @Test
    public void testApply() {
        ManualReplacements rep = 
            new ManualReplacements.Builder()
            .add("foo", "bar")
            .add(" x ", " y ")
            .add("dog", "cat")
            .build();

        assertThat(rep.apply("hello, world"), is("hello, world"));
        assertThat(rep.apply("fooxfoo"), is("barxbar"));
        assertThat(rep.apply("foo x foo"), is("bar y bar"));
        assertThat(rep.apply("foodog"), is("barcat"));
    }
}
