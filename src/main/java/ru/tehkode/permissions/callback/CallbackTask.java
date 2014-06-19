package ru.tehkode.permissions.callback;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.FutureTask;

/**
 * An implementation of FutureTask that handles callbacks
 */
public class CallbackTask<V> extends FutureTask<V> {
	private final Callback<V> callback;
	public CallbackTask(Callable<V> callable, Callback<V> callback) {
		super(callable);
		this.callback = callback;
	}

	public CallbackTask(Runnable runnable, V result, Callback<V> callback) {
		super(runnable, result);
		this.callback = callback;
	}

	@Override
	public void done() {
		try {
			V result = get();
			if (callback != null) {
				callback.onSuccess(result);
			}
		} catch (InterruptedException e) {
			callError(e);
		} catch (ExecutionException e) {
			if (e.getCause() != null) {
				callError(e.getCause());
			} else {
				callError(e);
			}
		}
	}

	private void callError(Throwable t) {
		if (callback != null) {
			callback.onError(t);
		}
	}
}
