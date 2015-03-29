package ru.tehkode.permissions.bukkit;

import java.io.*;
import java.net.*;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.Plugin;
import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.Yaml;
import ru.tehkode.permissions.PermissionManager;
import ru.tehkode.permissions.backends.PermissionBackend;
import ru.tehkode.utils.StringUtils;

/**
 * Error report builder for PEX that provides additional information on top of report
 * and generates a short URL to create a GitHub issue.
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
	 * Returns a git.io shortened version of the input
	 *
	 * @param longUrl The input url
	 * @return The shortened URL, or the input url if an error occurs
	 */
	public static String shortenURL(String longUrl) {
		if (longUrl == null) {
			return longUrl;
		}

		try {
			URL url = new URL("http://git.io/create");
			HttpURLConnection connection = (HttpURLConnection) url.openConnection();
			connection.setDoOutput(true);
			connection.setRequestMethod("POST");
			connection.setRequestProperty("User-Agent", "Mozilla/5.0");

			String urlParameters = "url=" + URLEncoder.encode(longUrl, UTF8_ENCODING);

			DataOutputStream wr = new DataOutputStream(connection.getOutputStream());
			wr.writeBytes(urlParameters);
			wr.flush();
			wr.close();

			BufferedReader rd = new BufferedReader(new InputStreamReader(connection.getInputStream()));
			String line;
			StringBuilder sb = new StringBuilder();
			while ((line = rd.readLine()) != null) {
				sb.append(line);
			}

			return "http://git.io/" + sb.toString();
		} catch (Exception e) {
			return longUrl;
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

			Map<String, Object> request = new HashMap<>();		// {
			request.put("description", "PEX Error Report");	   //	 "description": "PEX Error Report",
			request.put("public", "false");					   //	 "public": false,
			Map<String, Object> filesMap = new HashMap<>();	   //	 "files": {
			Map<String, Object> singleFileMap = new HashMap<>();  //		 "report.md": {
			singleFileMap.put("content", text);				   //			 "content": <text>
			filesMap.put("report.md", singleFileMap);			 //		 }
			request.put("files", filesMap);					   //	 }
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

		PermissionsEx pexPlugin = (PermissionsEx) PermissionsEx.getPlugin();
		builder.addHeading("Basic info")
				.addText("**Server version:** " + Bukkit.getBukkitVersion() + " *running on* " + Bukkit.getVersion())
				.addText("**Online mode:** " + Bukkit.getOnlineMode())
				.addText("**Java version:** " + Runtime.class.getPackage().getImplementationVendor() + " - " + Runtime.class.getPackage().getImplementationTitle() + " - " + Runtime.class.getPackage().getImplementationVersion());

		if (pexPlugin != null) {
			Plugin[] plugins = pexPlugin.getServer().getPluginManager().getPlugins();
			StringBuilder pluginList = new StringBuilder("**Plugins (" + plugins.length + "):** (~~Strikeout~~ means disabled)\n");
			for (Plugin plugin : plugins) {
				pluginList.append("- ");
				if (plugin.getDescription() != null) {
					if (plugin.isEnabled()) {
						pluginList.append(plugin.getDescription().getName() + " *v" + plugin.getDescription().getVersion() + "*");
					} else {
						pluginList.append("~~").append(plugin.getDescription().getName() + " *v" + plugin.getDescription().getVersion() + "*").append("~~");
					}
				} else {
					pluginList.append("Unknown Plugin!");
				}

				pluginList.append(" (```").append(plugin.getClass().getName()).append("```)").append('\n');
			}
			builder.addText(pluginList.toString());
		}

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
		final File mainConfigFile = pexPlugin != null ? new File(pexPlugin.getDataFolder(), "config.yml") : null;
		String configuration;
		String permissionsDb = "Permissions configuration could not be read. Does it exist?";
		String activeBackend = "unknown";

		if (mainConfigFile == null) {
			configuration = "PEX plugin was inaccessible!";
		} else if (mainConfigFile.exists()) {
			try {
				pexConfig.load(mainConfigFile);
				successfulLoad = true;
			} catch (IOException | InvalidConfigurationException ignore) {
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
		if (pexPlugin != null) {
			PermissionManager manager = pexPlugin.getPermissionsManager();
			if (manager != null) {
				PermissionBackend backend = manager.getBackend();
				try {
					if (backend != null) {
						final StringWriter writer = new StringWriter();
						backend.writeContents(writer);
						permissionsDb = writer.toString();
						activeBackend = backend.toString();
					}
				} catch (Throwable t) {
					// Continue
				}
			}
			if (permissionsDb == null && pexConfig.getString("permissions.backends." + pexConfig.getString("permissions.backend", "file") + ".type", "file").equalsIgnoreCase("file")) {
				File file = new File(pexPlugin.getDataFolder(), pexConfig.getString("permissions.backends.file.file", "permissions.yml"));
				if (file.exists()) {
					try {
						permissionsDb = StringUtils.readStream(new FileInputStream(file));
						activeBackend = "file";
					} catch (IOException ignore) {
					}
				}
			}
		}
		if (permissionsDb == null) {
			permissionsDb = "Backend is not file or plugin was not accessible, see configuration file for details";
		}

		builder.addHeading("Permissions database");
		builder.addText("**Active backend:** " + activeBackend);
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
