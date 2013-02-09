package ru.tehkode.permissions.bukkit.superperms;

import org.bukkit.entity.Player;
import org.bukkit.permissions.Permissible;
import org.bukkit.permissions.PermissibleBase;

import java.lang.reflect.Field;
import java.util.logging.Logger;

/**
 * This class handles injection of {@link Permissible}s into {@link Player}s for various server implementations.
 */
public abstract class PermissibleInjector {
	protected final String clazzName, fieldName;
	protected final boolean copyValues;

	public PermissibleInjector(String clazzName, String fieldName, boolean copyValues) {
		this.clazzName = clazzName;
		this.fieldName = fieldName;
		this.copyValues = copyValues;
	}

	/**
	 * Attempts to inject {@code permissible} into {@code player},
	 *
	 * @param player      The player to have {@code permissible} injected into
	 * @param permissible The permissible to inject into {@code player}
	 * @return whether the injection was successful
	 * @throws NoSuchFieldException   when the permissions field could not be found in the Permissible
	 * @throws IllegalAccessException when things go very wrong
	 */
	public boolean inject(Player player, Permissible permissible) throws NoSuchFieldException, IllegalAccessException {
		Class humanEntity;
		try {
			humanEntity = Class.forName(clazzName);
		} catch (ClassNotFoundException e) {
			Logger.getLogger("Minecraft").warning("[PermissionsEx] Unknown server implementation being used!");
			return false;
		}

		if (!humanEntity.isAssignableFrom(player.getClass())) {
			Logger.getLogger("Minecraft").warning("[PermissionsEx] Strange error while injecting permissible!");
			return false;
		}

		Field permField = humanEntity.getDeclaredField(fieldName);
		// Make it public for reflection
		permField.setAccessible(true);

		if (copyValues) {
			PermissibleBase oldBase = (PermissibleBase) permField.get(player);

			// Copy permissions and attachments from old Permissible

			// Attachments
			Field attachmentField = PermissibleBase.class.getDeclaredField("attachments");
			attachmentField.setAccessible(true);
			attachmentField.set(permissible, attachmentField.get(oldBase));

			// Permissions
			Field permissionsField = PermissibleBase.class.getDeclaredField("permissions");
			permissionsField.setAccessible(true);
			permissionsField.set(permissible, permissionsField.get(oldBase));
		}

		// Inject permissible
		permField.set(player, permissible);
		return true;
	}

	public abstract boolean isApplicable(Player player);

	public static class ServerNamePermissibleInjector extends PermissibleInjector {
		protected final String serverName;

		public ServerNamePermissibleInjector(String clazz, String field, boolean copyValues, String serverName) {
			super(clazz, field, copyValues);
			this.serverName = serverName;
		}

		@Override
		public boolean isApplicable(Player player) {
			return player.getServer().getName().equalsIgnoreCase(serverName);
		}
	}

	public static class ClassNameRegexPermissibleInjector extends PermissibleInjector {
		private final String regex;

		public ClassNameRegexPermissibleInjector(String clazz, String field, boolean copyValues, String regex) {
			super(clazz, field, copyValues);
			this.regex = regex;
		}

		@Override
		public boolean isApplicable(Player player) {
			return player.getClass().getName().matches(regex);
		}
	}
}
