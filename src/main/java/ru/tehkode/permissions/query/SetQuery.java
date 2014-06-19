package ru.tehkode.permissions.query;

import com.google.common.collect.ImmutableList;
import ru.tehkode.permissions.PermissionManager;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 * Query used to set permissions data
 */
public class SetQuery extends PermissionQuery<SetQuery> {
	private List<String> addPermissions, removePermissions, setPermissions;
	private Map<String, String> setOption,removeOption;
	private List<String> addParent, removeParent, setParents;
	public SetQuery(PermissionManager manager) {
		super(manager);
		followInheritance(false); // When setting, we usually don't want to go up in the inheritance tree to look for sections to modify
	}

	public SetQuery addPermission(String permission) {
		return this;
	}

	public SetQuery removePermission(String permission) {
		return this;
	}

	public SetQuery setPermissions(List<String> permissions) {
		return this;
	}

	public SetQuery setOption(String key, String value) {
		return this;
	}

	public SetQuery removeOption(String key) {
		return this;
	}

	public SetQuery addParent(String parent) {
		if (addParent == null) {
			addParent = new LinkedList<>();
		}
		addParent.add(parent);
		return this;
	}

	public SetQuery removeParent(String parent) {
		if (removeParent == null) {
			removeParent = new LinkedList<>();
		}
		removeParent.add(parent);
		return this;
	}

	public SetQuery setParents(List<String> parents) {
		setParents = ImmutableList.copyOf(parents);
		return this;
	}

	/**
	 * Actually perform the operation specified
	 */
	public void perform() {

	}

}
