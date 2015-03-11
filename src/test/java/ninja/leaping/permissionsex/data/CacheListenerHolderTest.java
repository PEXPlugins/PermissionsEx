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
package ninja.leaping.permissionsex.data;

import org.junit.Test;
import org.mockito.Mockito;

import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.Assert.*;

public class CacheListenerHolderTest {
    private final ImmutableOptionSubjectData testData = Mockito.mock(ImmutableOptionSubjectData.class);

    @Test
    public void testRegisterEvent() {
        final CacheListenerHolder<String> holder = new CacheListenerHolder<>();
        final CountingListener listener = new CountingListener();
        holder.addListener("test", listener);
        holder.call("test", testData);

        assertEquals(1, listener.getCount());
    }

    @Test
    public void testUnregisterEvent() {
        final CacheListenerHolder<String> holder = new CacheListenerHolder<>();
        final CountingListener listener = new CountingListener();
        holder.addListener("test", listener);
        holder.removeListener("test", listener);
        holder.call("test", testData);

        assertEquals(0, listener.getCount());
    }

    private static class CountingListener implements Caching {
        private final AtomicInteger count = new AtomicInteger();

        @Override
        public void clearCache(ImmutableOptionSubjectData newData) {
            count.getAndIncrement();
        }

        public int getCount() {
            return count.get();
        }
    }
}
