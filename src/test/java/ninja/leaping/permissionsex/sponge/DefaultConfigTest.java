package ninja.leaping.permissionsex.sponge;

import org.junit.Test;

import java.io.IOException;

public class DefaultConfigTest {
    /**
     * This is a sanity check that loads the config included in the jar at build time and makes sure it is syntactically valid
     */
    @Test
    public void testDefaultConfigLoading() throws IOException {
        PermissionsExPlugin.loadDefaultConfiguration();
    }
}
