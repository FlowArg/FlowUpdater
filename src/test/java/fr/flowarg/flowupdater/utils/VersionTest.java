package fr.flowarg.flowupdater.utils;

import org.junit.jupiter.api.Test;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class VersionTest
{
    @Test
    public void testVersionCompareWithSameSize()
    {
        final Version version = Version.gen("1.0.0");
        final Version version2 = Version.gen("1.0.0");
        final Version version3 = Version.gen("1.0.1");
        final Version version4 = Version.gen("1.1.0");
        final Version version5 = Version.gen("2.0.0");
        final Version version6 = Version.gen("0.1.0");
        final Version version7 = new Version(Arrays.asList(1, 1, 1));

        assertEquals(0, version.compareTo(version2));
        assertEquals(-1, version.compareTo(version3));
        assertEquals(-1, version.compareTo(version4));
        assertEquals(-1, version.compareTo(version5));
        assertEquals(1, version.compareTo(version6));
        assertEquals(-1, version.compareTo(version7));
    }

    @Test
    public void testVersionCompareWithDifferentSize()
    {
        final Version version = Version.gen("1.0.0");
        final Version version2 = Version.gen("1.1");
        final Version version3 = Version.gen("1.0");
        final Version version4 = Version.gen("3");
        final Version version5 = Version.gen("0");
        final Version version6 = Version.gen("0.1");
        final Version version7 = new Version(Arrays.asList(1, 2, 3, 4, 5));

        assertEquals(-1, version.compareTo(version2));
        assertEquals(1, version.compareTo(version3));
        assertEquals(-1, version.compareTo(version4));
        assertEquals(1, version.compareTo(version5));
        assertEquals(1, version.compareTo(version6));
        assertEquals(-1, version.compareTo(version7));
    }
}
