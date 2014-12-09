package com.permissionsex.permissions.sponge;

import com.google.inject.Inject;
import com.permissionsex.permissions.exception.PEBKACException;
import org.slf4j.Logger;
import org.spongepowered.api.event.state.InitializationEvent;
import org.spongepowered.api.event.state.PreInitializationEvent;
import org.spongepowered.api.event.state.ServerStoppedEvent;
import org.spongepowered.api.plugin.Plugin;
import org.spongepowered.api.service.ProviderExistsException;
import org.spongepowered.api.service.config.DefaultConfig;
import org.spongepowered.api.service.permission.PermissionService;
import org.spongepowered.api.service.sql.SQLService;
import org.spongepowered.api.util.config.ConfigFile;
import org.spongepowered.api.util.event.Subscribe;

import java.net.URL;

/**
 * PermissionsEx plugin
 */
@Plugin(id = "permissionsex", name = "PermissionsEx")
public class PermissionsExPlugin {
	@Inject private SQLService sql;
	@Inject @DefaultConfig(sharedRoot = false) private ConfigFile config;
	@Inject private Logger logger;
	private PEXService service;

	@Subscribe
	public void onPreInit(PreInitializationEvent event) throws PEBKACException {
		logger.info("Pre-init of PermissionsEx vSpongeNeedsWork");
		final URL defaultConfig = getClass().getResource("default.conf");
		if (defaultConfig == null) {
			throw new Error("Default config file is not present in jar!");
		}
		config = config.withFallback(defaultConfig);


		service = new PEXService();
		try {
			event.getGame().getServiceManager().setProvider(this, PermissionService.class, service);
		} catch (ProviderExistsException e) {
			service.close();
			throw new PEBKACException("Your appear to already be using a different permissions plugin: " + e.getMessage());
		}
		// Clean up stuff
		config.save(true);
	}

	@Subscribe
	public void disable(ServerStoppedEvent event) {
		logger.debug("Disabling PermissionsEx");
		if (service != null) {
			service.close();
			service = null;
		}
	}

	public boolean reload() {
		//config.
		return true;
	}


}
