package ninja.leaping.permissionsex.sponge.config;

import ninja.leaping.configurate.ConfigurationNode;
import ninja.leaping.configurate.transformation.ConfigurationTransformation;
import ninja.leaping.configurate.transformation.MoveStrategy;
import ninja.leaping.configurate.transformation.TransformAction;

import static ninja.leaping.configurate.transformation.ConfigurationTransformation.builder;

public class ConfigTransformations {
    private static final TransformAction DELETE_ITEM = new TransformAction() {
        @Override
        public Object[] visitPath(ConfigurationTransformation.NodePath inputPath, ConfigurationNode valueAtPath) {
            System.out.println("Deleting value " + valueAtPath.getValue());
            valueAtPath.setValue(null);
            return null;
        }
    };

    /**
     * Creat a configuration transformation that converts the Bukkit global configuration structure to the new Sponge configuration structure.
     * @return A transformation that converts a Bukkit-style configuration to a Sponge-style configuration
     */
    public static ConfigurationTransformation fromBukkit() {
        return builder()
                        .setMoveStrategy(MoveStrategy.MERGE)
                        .addAction(p("permissions"), new TransformAction() {
                            @Override
                            public Object[] visitPath(ConfigurationTransformation.NodePath inputPath, ConfigurationNode valueAtPath) {
                                return new Object[0];
                            }
                        })
                        .addAction(p("permissions", "backend"), new TransformAction() {
                            @Override
                            public Object[] visitPath(ConfigurationTransformation.NodePath inputPath, ConfigurationNode valueAtPath) {
                                return p("default-backend");
                            }
                        })
                        .addAction(p("permissions", "allowOps"), DELETE_ITEM)
                        .addAction(p("permissions", "basedir"), DELETE_ITEM)
                        .addAction(p("updater"), new TransformAction() {
                            @Override
                            public Object[] visitPath(ConfigurationTransformation.NodePath inputPath, ConfigurationNode valueAtPath) {
                                valueAtPath.getNode("enabled").setValue(valueAtPath.getValue());
                                valueAtPath.getNode("always-update").setValue(valueAtPath.getParent().getNode("alwaysUpdate"));
                                valueAtPath.getParent().getNode("alwaysUpdate").setValue(null);
                                return null;
                            }
                        })
                        .build();
    }

    private static Object[] p(Object... path) {
        return path;
    }
}
