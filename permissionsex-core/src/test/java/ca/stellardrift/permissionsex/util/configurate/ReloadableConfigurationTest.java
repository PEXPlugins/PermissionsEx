/**
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

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.IOException;
import java.nio.file.*;
import java.util.Collections;
import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.Assert.assertEquals;

public class ReloadableConfigurationTest {
    @Rule
    public TemporaryFolder tempFolder = new TemporaryFolder();

    private static WatchServiceListener listener;

    @BeforeClass
    public static void setUpClass() {
        listener = new WatchServiceListener();
    }

    @AfterClass
    public static void tearDownClass() {
        listener.close();
    }

    @Test
    public void testListenToPath() throws IOException, InterruptedException, BrokenBarrierException {
        Path testFile = tempFolder.newFile().toPath();
        Files.write(testFile, Collections.singleton("version one"), StandardOpenOption.SYNC);
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
    }
    
    @Test
    public void testListenToDirectory() throws IOException, BrokenBarrierException, InterruptedException {
        Path baseDir = tempFolder.newFolder().toPath();
        Files.createDirectories(baseDir);
        final AtomicReference<Path> lastPath = new AtomicReference<>();
        final CyclicBarrier barrier = new CyclicBarrier(2);
        final WatchKey key = listener.listenToDirectory(baseDir, event -> {
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

        final Path test1 = baseDir.resolve("test1");
        Files.write(test1, Collections.singleton("version one"));

        barrier.await();
        assertEquals(test1.getFileName(), lastPath.get());
        barrier.reset();

        final Path test2 = baseDir.resolve("test2");
        Files.write(test2, Collections.singleton("version two"));

        barrier.await();
        assertEquals(test2.getFileName(), lastPath.get());
    }

    @Test
    public void testReloadableConfig() {

    }
}
