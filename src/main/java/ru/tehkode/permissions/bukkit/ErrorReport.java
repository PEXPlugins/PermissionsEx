package ru.tehkode.permissions.bukkit;

import org.bukkit.ChatColor;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.Plugin;
import ru.tehkode.utils.StringUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Error report builder for PEX that provides additional information on top of report and generates a short URL to create a github issue
 */
public class ErrorReport {
	private static final ExecutorService ASYNC_EXEC = Executors.newSingleThreadExecutor();
	private static final String UTF8_ENCODING = "utf-8";
	public static final String LONG_URL_FORMAT = "https://github.com/PEXPlugins/PermissionsEx/issues/new?title=%s&body=%s";

	private String shortURL;
	private final String title;
	private final String message;
	private final Throwable error;

	// Main report
	private ErrorReport(String title, String message, Throwable error) {
		this.title = title;
		this.message = message;
		this.error = error;
	}

	/**
	 * Returns a bit.ly shortened version of the input
	 *
	 * @param url The input url
	 * @return The shortened URl, or the input url if an error occurs
	 */
	public static String shortenURL(String url) {
		URL shortUrlApi;
		try {
			shortUrlApi = new URL("http://is.gd/create.php?format=simple&url=" + URLEncoder.encode(url, UTF8_ENCODING));
		} catch (MalformedURLException e) {
			e.printStackTrace();
			return url;
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
			return url;
		}

		URLConnection conn = null;
		try {
			conn = shortUrlApi.openConnection();
			return StringUtils.readStream(conn.getInputStream());
		} catch (IOException ex) {
			/*ex.printStackTrace(); // Debug code, uncomment if needed
			try {
				if (conn != null) {
					return StringUtils.readStream(((HttpURLConnection) conn).getErrorStream());
				}
			} catch (IOException ignore) {
			}*/
			return url;
		}
	}

	public String getShortURL() {
		if (shortURL == null) {
			shortURL = shortenURL(getLongURL());
		}
		return this.shortURL;
	}

	public String getLongURL() {
		try {
			return String.format(LONG_URL_FORMAT, URLEncoder.encode(title, UTF8_ENCODING), URLEncoder.encode(message, UTF8_ENCODING));
		} catch (UnsupportedEncodingException e) {
			throw new RuntimeException(e);
		}
	}

	public String getErrorBody() {
		return message;
	}

	public String buildUserErrorMessage() {
		StringBuilder build = new StringBuilder("Error occurred with PermissionsEx! Please post it to ")
				.append(getShortURL())
				.append(ChatColor.RESET)
				.append(". Full error:\n");
		StringWriter writer = new StringWriter();
		PrintWriter pWriter = new PrintWriter(writer);
		if (error != null) {
			error.printStackTrace(pWriter);
			build.append(writer);
			pWriter.close();
		} else {
			build.append("Not specified");
		}
		return build.toString();
	}

	public static void handleError(Throwable error) {
		handleError(null, error);
	}

	public static void handleError(final String cause, final Throwable error) {
		if (!ASYNC_EXEC.isShutdown()) {
			ASYNC_EXEC.submit(new Runnable() {
				@Override
				public void run() {
					PermissionsEx.logger.severe(withException(cause, error).buildUserErrorMessage());
				}
			});
		} else {
			PermissionsEx.logger.severe(withException(cause, error).buildUserErrorMessage());
		}
	}

	public static void shutdown() {
		ASYNC_EXEC.shutdown();
	}

	// Factory methods
	public static ErrorReport withException(String cause, Throwable error) {
		Builder builder = builder(error);
		builder.addHeading("Description")
				.addText("[Insert description of issue here]");

		builder.addHeading("What PEX Saw");

		if (cause != null) {
			builder.addText("**Reason:** " + cause);
		}

		if (error != null) {
			// Stacktrace
			StringWriter w = new StringWriter();
			error.printStackTrace(new PrintWriter(w));
			w.flush();
			String exStackTrace = w.getBuffer().toString();

			builder.addText("**Stacktrace:**")
					.addCode(exStackTrace, null);
		}


		// PEX Configuration
		Plugin pexPlugin = PermissionsEx.getPlugin();
		YamlConfiguration pexConfig = new YamlConfiguration();
		boolean successfulLoad = false;
		final File mainConfigFile = new File(pexPlugin.getDataFolder(), "config.yml");
		String configuration;
		String permissionsDb = "Permissions configuration could not be read. Does it exist?";

		if (mainConfigFile.exists()) {
			try {
				pexConfig.load(mainConfigFile);
				successfulLoad = true;
			} catch (IOException ignore) {
			} catch (InvalidConfigurationException ignore) {
			}

			try {
				configuration = StringUtils.readStream(new FileInputStream(mainConfigFile));
			} catch (IOException e1) {
				configuration = "Unable to read configuration file at: " + mainConfigFile.getAbsolutePath();
			}
		} else {
			configuration = "PEX configuration does not exist!";
		}
		configuration = configuration.replaceAll("password: (.*)", "password: XXXXXXXXXX"); // Attempt to remove any passwords from the file (SQL)
		builder.addHeading("PEX configuration")
				.addCode(configuration, "yaml");

		// Permissions database
		if (pexConfig.getString("permissions.backend", "file").equalsIgnoreCase("file")) {
			File file = new File(pexPlugin.getDataFolder(), pexConfig.getString("permissions.backends.file.file", "permissions.yml"));
			if (file.exists()) {
				try {
					permissionsDb = StringUtils.readStream(new FileInputStream(file));
				} catch (IOException ignore) {
				}
			}
		} else {
			permissionsDb = "Backend is not file, see configuration file for details";
		}

		builder.addHeading("Permissions database");
		if (!successfulLoad) {
			builder.addText("PEX configuration could not be successfully loaded, attempting to read default permissions file");
		}
		builder.addCode(permissionsDb, "yaml");

		return builder.build();
	}

	public static Builder builder(Throwable error) {
		return new Builder("", error);
	}

	public static class Builder {
		private final String name;
		private final StringBuilder message = new StringBuilder();
		private final Throwable error;

		private Builder(String name, Throwable error) {
			this.name = name;
			this.error = error;
		}

		public Builder addHeading(String text) {
			message.append("### ").append(text).append(" ###\n");
			return this;
		}

		public Builder addText(String text) {
			message.append('\n').append(text).append('\n');
			return this;
		}

		public Builder addCode(String text, String format) {
			message.append("```");
			if (format != null) {
				message.append(format);
			}
			message.append('\n').append(text).append("```\n");
			return this;
		}

		public ErrorReport build() {
			return new ErrorReport(this.name, this.message.toString(), error);
		}
	}

	public class ExceptionHandler implements Thread.UncaughtExceptionHandler {
		@Override
		public void uncaughtException(Thread t, Throwable e) {
			PermissionsEx.logger.severe(
					withException("Unknown error in thread " + t.getName() + "-" + t.getId(), e)
							.buildUserErrorMessage());
		}
	}
}
