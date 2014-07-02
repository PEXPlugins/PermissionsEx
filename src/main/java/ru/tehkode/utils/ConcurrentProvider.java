package ru.tehkode.utils;

import java.util.Iterator;
import java.util.concurrent.atomic.AtomicReference;

/**
 * This is a simple data structure that allows transfer of data between threads until it is explicitly closed. Somewhat like a channel used in other languages.
 * This class is (obviously) thread-safe.
 *
 * This class is a bit limited because it can only be closed in conjunction with the addition of a new element.
 * This is unfortunately required by the way iterators work.
 */
public class ConcurrentProvider<V> implements Iterable<V> {
	private static class Element<V> {
		private final V value;
		volatile Element<V> next;

		Element(V value) {
			this.value = value;
		}
	}

	/**
	 * Special element to indicate that the stream of data has come to an end
	 */
	@SuppressWarnings("unchecked")
	private static final Element CLOSE_ELEMENT = new Element(null);

	/**
	 * Returns a provider that is empty and already closed.
	 * @param <T> The type of value contained in this provider
	 * @return An empty provider
	 */
	public static <T> ConcurrentProvider<T> emptyProvider() {
		ConcurrentProvider<T> provider = new ConcurrentProvider<>();
		provider.insertElement(CLOSE_ELEMENT);
		return provider;
	}

	public static <T> ConcurrentProvider<T> newProvider() {
		return new ConcurrentProvider<>();
	}

	private ConcurrentProvider() {
	}

	private final AtomicReference<Element<V>> head = new AtomicReference<>(), tail = new AtomicReference<>();
	private final Object waitCondition = new Object();

	/**
	 * Provide a new element to be received by any iterators listening.
	 * @param value The value to add
	 */
	public void provide(V value) {
		insertElement(new Element<>(value));
	}

	/**
	 * Provide a new element, closing at the same time. Value cannot be null.
	 *
	 * @param value The value to add.
	 */
	public void provideAndClose(V value) {
		Element<V> newElement = new Element<>(value);
		newElement.next = CLOSE_ELEMENT;
		insertElement(newElement);
	}

	private void insertElement(Element<V> newElement) {
		if (head.compareAndSet(null, newElement)) { // First add
			synchronized (waitCondition) {
				Element<V> setElement = newElement;
				while (setElement.next != null) {
					setElement = setElement.next;
				}
				if (!tail.compareAndSet(null, setElement)) {
					throw new IllegalStateException("Tail is non-null when head is first being set!");
				}
				waitCondition.notifyAll();
			}
			return;
		}
		while (true) {
			Element<V> oldElement = tail.getAndSet(null);
			if (oldElement == null) {
				continue;
			}
			if (oldElement == CLOSE_ELEMENT) {
				throw new IllegalStateException("Tried to add element to closed provider!");
			}
			synchronized (waitCondition) {
				oldElement.next = newElement;
				Element<V> setElement = newElement;
				while (setElement.next != null) {
					setElement = setElement.next;
				}
				if (tail.compareAndSet(null, setElement)) {
					System.out.println("Set element " + setElement);
					break;
				}
				waitCondition.notifyAll();
			}
		}
	}

	private class ProviderIterator implements Iterator<V> {
		private Element<V> next;

		ProviderIterator() {
		}

		@Override
		public boolean hasNext() {
			return head.get() != CLOSE_ELEMENT && (next == null || next.next != CLOSE_ELEMENT);
		}

		@Override
		public V next() {
			fetchNext();
			if (next == CLOSE_ELEMENT) {
				throw new IndexOutOfBoundsException("Tried to iterate past close");
			}
			return next.value;
		}

		private void fetchNext() {
			if (next == null) {
				while (next == null) {
					synchronized (waitCondition) {
						next = head.get();
						if (next == null) {
							try {
								waitCondition.wait();
							} catch (InterruptedException e) {
								// Ignore it and try another loop
							}
						}
					}
				}
			} else {
				while (next.next == null) {
					synchronized (waitCondition) {
						try {
							waitCondition.wait();
						} catch (InterruptedException e) {
							// Try another loop
						}
					}
				}
				next = next.next;
			}
		}



		@Override
		public void remove() {
			throw new UnsupportedOperationException("Not supported by ConcurrentProvider!");
		}
	}

	@Override
	public Iterator<V> iterator() {
		return new ProviderIterator();
	}
}
