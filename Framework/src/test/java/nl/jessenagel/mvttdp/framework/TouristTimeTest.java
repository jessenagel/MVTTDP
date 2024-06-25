package nl.jessenagel.mvttdp.framework;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class TouristTimeTest {

    @Test
    void fromMinutes() {
        TouristTime time = TouristTime.fromMinutes(0);
        assertEquals(0, time.day);
        assertEquals(0, time.hour);
        assertEquals(0, time.minute);

        time = TouristTime.fromMinutes(1);
        assertEquals(0, time.day);
        assertEquals(0, time.hour);
        assertEquals(1, time.minute);

        time = TouristTime.fromMinutes(60);
        assertEquals(0, time.day);
        assertEquals(1, time.hour);
        assertEquals(0, time.minute);

        time = TouristTime.fromMinutes(61);
        assertEquals(0, time.day);
        assertEquals(1, time.hour);
        assertEquals(1, time.minute);

        time = TouristTime.fromMinutes(1440);
        assertEquals(1, time.day);
        assertEquals(0, time.hour);
        assertEquals(0, time.minute);

        time = TouristTime.fromMinutes(1441);
        assertEquals(1, time.day);
        assertEquals(0, time.hour);
        assertEquals(1, time.minute);

        time = TouristTime.fromMinutes(1500);
        assertEquals(1, time.day);
        assertEquals(1, time.hour);
        assertEquals(0, time.minute);

        time = TouristTime.fromMinutes(1501);
        assertEquals(1, time.day);
        assertEquals(1, time.hour);
        assertEquals(1, time.minute);
    }

}