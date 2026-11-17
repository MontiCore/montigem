package coco;

import java.io.File;
import java.util.Collections;
import java.util.ArrayList;
import java.util.Objects;
import java.util.TreeMap;
import java.time.LocalTime;
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
        LocalTime t = LocalTime.now();
    }

    public void testConcurrent() {
       Executors.newCachedThreadPool();
    }

    public void testSortedMap() {
       TreeMap<String, Integer> sorted = new TreeMap<>();
    }
}
