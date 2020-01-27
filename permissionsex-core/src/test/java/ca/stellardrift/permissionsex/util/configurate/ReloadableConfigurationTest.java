/*
 * PermissionsEx
 * Copyright (C) zml and PermissionsEx contributors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package ca.stellardrift.permissionsex.util.configurate;


import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.nio.file.WatchKey;
import java.util.Collections;
import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;


@Disabled("Intermittently locks up and prevents the build from completing")
public class ReloadableConfigurationTest {

    private static WatchServiceListener listener;

    @BeforeAll
    public static void setUpClass() {
        listener = new WatchServiceListener();
    }

    @AfterAll
    public static void tearDownClass() {
        listener.close();
    }

    @Test
    public void testListenToPath(@TempDir Path tempFolder) throws IOException, InterruptedException, BrokenBarrierException {
        Files.createDirectories(tempFolder);
        Path testFile = tempFolder.resolve("listenPath.txt");
        Files.write(testFile, Collections.singleton("version one"), StandardOpenOption.SYNC, StandardOpenOption.CREATE);
        final AtomicInteger callCount = new AtomicInteger(0);
        final CyclicBarrier barrier = new CyclicBarrier(2);
        final WatchKey key = listener.listenToFile(testFile, event -> {
            int oldVal = callCount.getAndIncrement();
            if (oldVal > 1) {
                return false;
            }

            try {
                barrier.await();
            } catch (InterruptedException | BrokenBarrierException e) {
                throw new RuntimeException(e);
            }
            return oldVal < 1;
        });

        assertEquals(0, callCount.get());

        Files.write(testFile, Collections.singleton("version two"), StandardOpenOption.SYNC);

        barrier.await();
        assertEquals(1, callCount.get());
        barrier.reset();

        Files.write(testFile, Collections.singleton("version three"), StandardOpenOption.SYNC);

        barrier.await();
        assertEquals(2, callCount.get());

        Files.write(testFile, Collections.singleton("version four"), StandardOpenOption.SYNC);

        assertEquals(2, callCount.get());

        key.cancel();
    }
    
    @Test
    public void testListenToDirectory(@TempDir Path tempFolder) throws IOException, BrokenBarrierException, InterruptedException {
        Files.createDirectories(tempFolder);
        final AtomicReference<Path> lastPath = new AtomicReference<>();
        final CyclicBarrier barrier = new CyclicBarrier(2);
        final WatchKey key = listener.listenToDirectory(tempFolder, event -> {
            if (event.context() instanceof Path){
                lastPath.set(((Path) event.context()));
            } else if (event instanceof CloseWatchEvent) {
                return false;
            } else {
                throw new RuntimeException("Event " + event + " received, was not expected");
            }
            try {
                barrier.await();
            } catch (InterruptedException | BrokenBarrierException e) {
                throw new RuntimeException(e);
            }
            return true;
        });

        final Path test1 = tempFolder.resolve("test1");
        Files.write(test1, Collections.singleton("version one"), StandardOpenOption.SYNC, StandardOpenOption.CREATE);

        barrier.await();
        assertEquals(test1.getFileName(), lastPath.get());
        barrier.reset();

        final Path test2 = tempFolder.resolve("test2");
        Files.write(test2, Collections.singleton("version two"), StandardOpenOption.SYNC, StandardOpenOption.CREATE);

        barrier.await();
        assertEquals(test2.getFileName(), lastPath.get());
    }

    @Test
    @Disabled
    public void testReloadableConfig() {

    }
}
