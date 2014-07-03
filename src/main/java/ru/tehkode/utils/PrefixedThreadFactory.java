package ru.tehkode.utils;

import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Factory that creates threads named
 * {prefix}-{number}
 */
public class PrefixedThreadFactory implements ThreadFactory {
	private final String name;
	private final AtomicInteger counter = new AtomicInteger();

	public PrefixedThreadFactory(String name) {
		this.name = name;
	}

	@Override
	public Thread newThread(Runnable r) {
		return new Thread(r, name + "-" + counter.getAndIncrement());
	}
}
