package ru.tehkode.utils;

import org.junit.Assert;
import org.junit.Test;

public class DateUtilsTest {

    @Test
    public void testParseIntervalBySeconds() throws Exception {
        int result = DateUtils.parseInterval("1000");

        Assert.assertEquals(1000, result);
    }

    @Test
    public void testParseIntervalByPhrase() throws Exception {
        int result = DateUtils.parseInterval("1 year 2 months 3 weeks 4 days 5 hours 6 minutes 7 seconds");

        // 7 + 6*60 + 5*3600 + 4*3600*24 + 3*7*3600*24 + 2*30*3600*24 + 1*12*30*3600*24
        Assert.assertEquals(38466367,  result);
    }

    @Test
    public void testParseIntervalByShortcutPhrase() throws Exception {
        int result = DateUtils.parseInterval("3 w 4 d 5 h 6 m 7 s");

        // 7 + 6*60 + 5*3600 + 4*3600*24 + 3*7*3600*24
        Assert.assertEquals(2178367,  result);
    }

    @Test
    public void testParseIntervalByFloatPhrase() throws Exception {
        int result = DateUtils.parseInterval("7 s 1.5 m");

        // 7 + 1.5 * 60 = 7 + 90
        Assert.assertEquals(97,  result);
    }

    @Test
    public void testParseIntervalWithTightSyntax() throws Exception {
        int result = DateUtils.parseInterval("7s 6m 1day");

        // 7 + 6 * 60 + 3600*24
        Assert.assertEquals(86767,  result);
    }
}
