package lodi;

import org.junit.*;
import static org.junit.Assert.assertThat;
import static org.hamcrest.CoreMatchers.*;

public class RawLocationTest {

    @Test
    public void testConcatenateLocation() {
	    String s = RawLocation.concatenateLocation("washington", "dc", "us");
        assertThat(s, is("washington, dc, us"));

        s = RawLocation.concatenateLocation("", "dc", "us");
        assertThat(s, is("dc, us"));

        s = RawLocation.concatenateLocation("washington", null, "us");
        assertThat(s, is("washington, us"));
    }
}
