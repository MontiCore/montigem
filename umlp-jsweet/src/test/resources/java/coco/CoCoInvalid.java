package coco;

import java.io.File;
import java.util.Collections;
import java.util.ArrayList;
import java.util.Objects;
import java.util.TreeMap;
import java.time.Duration;
import java.util.Date;
import java.util.concurrent.Executors;

public class CoCoInvalid {

    public void testIO() {
        File f = new File("dummy");
    }

    public void testCollections() {
       Collections.sort(new ArrayList<String>());
       Collections.emptySet();
       Collections.emptyMap();
    }

    public void testObjects() {
        Objects.hash("a", "b");
        Objects.hashCode("a");
    }

    public void testTime() {
        Duration t = Duration.ofSeconds(1);
    }

    public void testDate() {
        Date d = new Date();
    }

    public void testConcurrent() {
       Executors.newCachedThreadPool();
    }

    public void testSortedMap() {
       TreeMap<String, Integer> sorted = new TreeMap<>();
    }
}
