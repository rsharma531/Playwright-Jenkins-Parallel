package com.rahul.framework.core;

import com.rahul.framework.config.FrameworkConfig;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * The journey manifest is the contract between the two parallelism tiers.
 *
 * src/main/resources/journeys.manifest lists every journey class, one per
 * line. Jenkins doesn't split anything itself — every shard checks out the
 * same repo and runs the same command; only SHARD_INDEX differs. Each JVM
 * then deterministically takes its slice here:
 *
 *     sorted manifest line i  belongs to  shard (i % SHARD_TOTAL)
 *
 * Because the list is sorted before slicing, every shard computes the same
 * global order and the slices are disjoint and complete: nothing runs twice,
 * nothing is skipped. On a laptop SHARD_TOTAL defaults to 1, so the slice is
 * "everything" and CI vs local differs by environment variables only.
 */
public final class JourneyManifest {

    private static final String MANIFEST_RESOURCE = "/journeys.manifest";

    private JourneyManifest() {
    }

    public static List<Journey> loadForThisShard() {
        List<String> allClasses = readManifest();
        allClasses.sort(String::compareTo);

        int shardIndex = FrameworkConfig.shardIndex();
        int shardTotal = FrameworkConfig.shardTotal();

        List<Journey> mine = new ArrayList<>();
        for (int i = 0; i < allClasses.size(); i++) {
            if (i % shardTotal == shardIndex) {
                mine.add(instantiate(allClasses.get(i)));
            }
        }
        System.out.printf("Shard %d/%d picked %d of %d journeys: %s%n",
                shardIndex, shardTotal, mine.size(), allClasses.size(),
                mine.stream().map(Journey::name).toList());
        return mine;
    }

    private static List<String> readManifest() {
        InputStream in = JourneyManifest.class.getResourceAsStream(MANIFEST_RESOURCE);
        if (in == null) {
            throw new IllegalStateException("journeys.manifest not found on classpath");
        }
        List<String> classes = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (!line.isEmpty() && !line.startsWith("#")) {
                    classes.add(line);
                }
            }
        } catch (Exception e) {
            throw new IllegalStateException("Failed to read journeys.manifest", e);
        }
        if (classes.isEmpty()) {
            throw new IllegalStateException("journeys.manifest is empty");
        }
        return classes;
    }

    private static Journey instantiate(String className) {
        try {
            return (Journey) Class.forName(className).getDeclaredConstructor().newInstance();
        } catch (ReflectiveOperationException | ClassCastException e) {
            throw new IllegalStateException("Cannot instantiate journey: " + className, e);
        }
    }
}
