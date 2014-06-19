package ru.tehkode.permissions.callback;

/**
 * @author zml2008
 */
public interface Callback<T> {
	public void onSuccess(T result);
	public void onError(Throwable t);
}
