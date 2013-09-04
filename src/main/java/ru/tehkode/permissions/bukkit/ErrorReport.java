package ru.tehkode.permissions.bukkit;

import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.Plugin;
import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.Yaml;
import ru.tehkode.utils.StringUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Error report builder for PEX that provides additional information on top of report and generates a short URL to create a github issue
 */
public class ErrorReport {
	private static final ExecutorService ASYNC_EXEC = Executors.newSingleThreadExecutor();
	private static final String UTF8_ENCODING = "utf-8";
	private static final ThreadLocal<Yaml> YAML_INSTANCE = new ThreadLocal<Yaml>() {
		@Override
		protected Yaml initialValue() {
			DumperOptions opts = new DumperOptions();
			opts.setDefaultFlowStyle(DumperOptions.FlowStyle.FLOW);
			opts.setDefaultScalarStyle(DumperOptions.ScalarStyle.DOUBLE_QUOTED);
			opts.setPrettyFlow(true);
			opts.setWidth(Integer.MAX_VALUE); // Don't wrap scalars -- json no like
			return new Yaml(opts);
		}
	};
	private static final URL GIST_POST_URL;

	static {
		try {
			GIST_POST_URL = new URL("https://api.github.com/gists");
		} catch (MalformedURLException e) {
			throw new ExceptionInInitializerError(e);
		}
	}

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

		URLConnection conn;
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

	public static String gistText(String text) {
		Yaml yaml = YAML_INSTANCE.get();
		OutputStreamWriter requestWriter = null;
		InputStream responseReader = null;
		try {
			URLConnection conn = GIST_POST_URL.openConnection();
			conn.setDoOutput(true);
			conn.setDoInput(true);

			Map<String, Object> request = new HashMap<String, Object>();       // {
			request.put("description", "PEX Error Report");       //     "description": "PEX Error Report",
			request.put("public", "false");                       //     "public": false,
			Map<String, Object> filesMap = new HashMap<String, Object>();      //     "files": {
			Map<String, Object> singleFileMap = new HashMap<String, Object>(); //         "report.md": {
			singleFileMap.put("content", text);                   //             "content": <text>
			filesMap.put("report.md", singleFileMap);             //         }
			request.put("files", filesMap);                       //     }
			                                                      // }
			yaml.dump(request, (requestWriter = new OutputStreamWriter(conn.getOutputStream())));

			Map<?, ?> data = (Map<?, ?>) yaml.load((responseReader = conn.getInputStream()));
			if (data.containsKey("html_url")) {
				return data.get("html_url").toString();
			}
		} catch (IOException e) {
			e.printStackTrace();
			return null;
		} finally {
			if (requestWriter != null) {
				try {
					requestWriter.close();
				} catch (IOException ignore) {
				}
			}

			if (responseReader != null) {
				try {
					responseReader.close();
				} catch (IOException ignore) {
				}
			}
		}
		return null;
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
		handleError(cause, error, null);
	}

	public static void handleError(final String cause, final Throwable error, final CommandSender target) {
		if (!ASYNC_EXEC.isShutdown()) {
			ASYNC_EXEC.submit(new Runnable() {
				@Override
				public void run() {
					String msg = withException(cause, error).buildUserErrorMessage();
					if (target != null) {
						target.sendMessage(msg);
					} else {
						PermissionsEx.getPlugin().getLogger().severe(msg);
					}
				}
			});
		} else {
			String msg = withException(cause, error).buildUserErrorMessage();
			if (target != null) {
				target.sendMessage(msg);
			} else {
				PermissionsEx.getPlugin().getLogger().severe(msg);
			}
		}
	}

	public static void shutdown() {
		ASYNC_EXEC.shutdown();
	}

	// Factory methods
	public static ErrorReport withException(String cause, Throwable error) {
		Builder builder = builder(error);

		Plugin pexPlugin = PermissionsEx.getPlugin();
		builder.addHeading("Basic info").
				addText("**Bukkit version:** " + Bukkit.getBukkitVersion() + " running on " + Bukkit.getVersion()).
				addText("**PermissionsEx version:** " + (pexPlugin == null || pexPlugin.getDescription() == null ? "unknown" : pexPlugin.getDescription().getVersion()));

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
			message.append('\n').append(text).append("\n```\n");
			return this;
		}

		public ErrorReport build() {
			Builder builder = new Builder(name, error);
			builder.addHeading("Description")
					.addText("[Insert description of issue here]");
			builder.addHeading("Detailed Information");
			if (new File("plugins" + File.separator + "PermissionsEx", "report-disable").exists()) {
				builder.addText("I am stupid and chose to disable error reporting, therefore removing any chance of getting help with my error");
			} else {
				builder.addText("[Is available here](" + gistText(this.message.toString()) + ")");
			}
			return new ErrorReport(this.name, builder.message.toString(), error);
		}
	}

	public class ExceptionHandler implements Thread.UncaughtExceptionHandler {
		@Override
		public void uncaughtException(Thread t, Throwable e) {
			handleError("Unknown error in thread " + t.getName() + "-" + t.getId(), e);
		}
	}
}
