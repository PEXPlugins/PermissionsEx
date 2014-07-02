package ru.tehkode.utils;

import com.google.common.collect.Iterables;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import java.util.Iterator;

import static org.junit.Assert.*;

/**
 * Basic functionality tests for ConcurrentProvider
 */
public class ConcurrentProviderTest {
	@Rule
	public final ExpectedException thrown = ExpectedException.none();

	@Test
	public void testBasicUsage() {
		final ConcurrentProvider<String> subject = ConcurrentProvider.newProvider();
		subject.provide("a");
		subject.provide("b");
		subject.provideAndClose("c");
		assertArrayEquals(new String[]{"a", "b", "c"}, Iterables.toArray(subject, String.class));
	}

	@Test(timeout = 200L)
	public void testInterleavedUsage() {
		final ConcurrentProvider<String> subject = ConcurrentProvider.newProvider();
		final Iterator<String> it = subject.iterator();
		assertTrue(it.hasNext());
		subject.provide("a");
		assertEquals("a", it.next());
		assertTrue(it.hasNext());

		subject.provide("b");
		assertEquals("b", it.next());
		assertTrue(it.hasNext());

		subject.provide("c");
		subject.provide("d");
		assertEquals("c", it.next());
		assertEquals("d", it.next());

		subject.provideAndClose("end");
		assertEquals("end", it.next());
		assertFalse(it.hasNext());
	}

	@Test
	public void testEmptyProvider() {
		final ConcurrentProvider<Object> subject = ConcurrentProvider.emptyProvider();
		Iterator<Object> it = subject.iterator();
		assertFalse(it.hasNext());
	}

	@Test
	public void testAddAfterCloseDisallowed() {
		final ConcurrentProvider<String> subject = ConcurrentProvider.newProvider();
		subject.provideAndClose("closing");
		thrown.expect(IllegalStateException.class);
		thrown.expectMessage("Tried to add element to closed provider!");
		subject.provide("after close");
	}

}
