package com.permissionsex.permissions.data;

import org.junit.Test;
import org.spongepowered.api.util.Tristate;

import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.*;

public class NodeTreeTest {
	@Test
	public void testGetValue() throws Exception {

		final Map<String, Tristate> values = new HashMap<>();
		values.put("test.perm.level", Tristate.TRUE);
		values.put("test.perm", Tristate.FALSE);
		NodeTree tree = NodeTree.fromMap(values, Tristate.UNDEFINED);

		assertEquals(Tristate.TRUE, tree.getValue("test.perm.level"));
		assertEquals(Tristate.FALSE, tree.getValue("test.perm"));
		assertEquals(Tristate.UNDEFINED, tree.getValue("random.perm"));
		assertEquals(Tristate.TRUE, tree.getValue("test.perm.level.deeper"));
		assertEquals(Tristate.FALSE, tree.getValue("test.perm.another"));
	}
}
