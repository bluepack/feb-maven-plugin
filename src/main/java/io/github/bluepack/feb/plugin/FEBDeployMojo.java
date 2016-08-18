package io.github.bluepack.feb.plugin;

import java.io.File;
import java.net.URL;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.settings.Server;
import org.apache.maven.settings.Settings;
import org.sonatype.plexus.components.sec.dispatcher.SecDispatcher;
import org.sonatype.plexus.components.sec.dispatcher.SecDispatcherException;

import io.github.bluepack.feb.client.api.ApplicationManagementRESTAPIApi;
import io.github.bluepack.feb.client.invoker.ApiClient;
import io.github.bluepack.feb.client.invoker.ApiException;

/**
 * <h1>FEB Deploy Mojo</h1> Maven Plugin for deploying IBM Forms Experience
 * Builder applications using the Application Management REST API.
 *
 * @author Denny Pichardo
 * @version 1.0
 * @since 2016-08-16
 */

@Mojo(name = "feb-deploy", requiresProject = true)
public class FEBDeployMojo extends AbstractMojo {

	public enum AccessType {
		secure, anon
	}

	public enum FEBSwitch {
		on, off
	}

	private static final String ACCEPT_ENC = "gzip, deflate, sdch";
	private static final String DEFAULT_MODE = "source";

	/**
	 * The hostname of the FEB instance where the form will be deployed
	 * 
	 * @since 1.0
	 */
	@Parameter(defaultValue = "http://localhost/")
	private URL basePath;

	/**
	 * FEB security mode
	 * 
	 * @since 1.0
	 */
	@Parameter(defaultValue = "secure")
	private AccessType accessType;

	/**
	 * FEB Administrator username. If not given, it will be looked up through
	 * <code>settings.xml</code>'s server with <code>${settingsKey}</code> as
	 * key.
	 *
	 * @since 1.0
	 */
	@Parameter(property = "username")
	private String username;

	/**
	 * FEB Administrator password. If not given, it will be looked up through
	 * <code>settings.xml</code>'s server with <code>${settingsKey}</code> as
	 * key.
	 *
	 * @since 1.0
	 */
	@Parameter(property = "password")
	private String password;

	/**
	 * The universal ID of the FEB application
	 * 
	 * @since 1.0
	 */
	@Parameter(required = true)
	private String appUid;

	/**
	 * Name of the nitro_s file
	 * 
	 * @since 1.0
	 */
	@Parameter(required = true)
	private File filename;

	/**
	 * Whether to replace the existing submission data with the data in the
	 * uploaded application
	 * 
	 * @since 1.0
	 */
	@Parameter(defaultValue = "off")
	private FEBSwitch replaceSubmittedData;

	/**
	 * If true automatically deploys the application as part of the import.
	 */
	@Parameter(defaultValue = "true")
	private boolean deploy = true;

	/**
	 * If true imports the submission data, or submitted records, if they were
	 * included when the application was exported.
	 */
	@Parameter(defaultValue = "false")
	private boolean importData = false;

	/**
	 * If true removes all groups and users from roles within the imported
	 * application ensuring that only the current authenticated user has access
	 * to the application.
	 */
	@Parameter(defaultValue = "false")
	private boolean cleanIds = false;

	/**
	 * @since 1.0
	 */
	@Parameter(defaultValue = "${settings}", readonly = true, required = true)
	private Settings settings;

	/**
	 * Server's <code>id</code> in <code>settings.xml</code> to look up username
	 * and password. Defaults to <code>${basePath}</code> if not given.
	 *
	 * @since 1.0
	 */
	@Parameter(property = "settingsKey")
	private String settingsKey;

	/**
	 * MNG-4384
	 * 
	 * @since 1.0
	 */
	@Component(role = org.sonatype.plexus.components.sec.dispatcher.SecDispatcher.class, hint = "default")
	private SecDispatcher securityDispatcher;

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.apache.maven.plugin.Mojo#execute()
	 */
	public void execute() throws MojoExecutionException, MojoFailureException {
		getLog().info("Deploying FEB Application " + appUid + " to " + basePath);

		ApiClient apiClient = new ApiClient();
		apiClient.setBasePath(basePath.toString());

		if (accessType == AccessType.secure) {
			loadUserInfoFromSettings();
			apiClient.setUsername(username);
			apiClient.setPassword(password);
		}

		ApplicationManagementRESTAPIApi febMgmtRESTApi = new ApplicationManagementRESTAPIApi(apiClient);
		Boolean formExists = null;
		try {
			// First check if the form exists
			File currentForm = febMgmtRESTApi.exportApplication(accessType.toString(), appUid, DEFAULT_MODE, ACCEPT_ENC, false);
			formExists = (currentForm != null);
		} catch (ApiException e) {
			formExists = false;
		}

		try {
			if (formExists != null && formExists.booleanValue()) {
				getLog().info("Form exists on the target server, it will be updated");
				febMgmtRESTApi.upgradeApplication(accessType.toString(), appUid, FEBSwitch.on.toString(),
						FEBSwitch.on.toString(), filename, replaceSubmittedData.toString());
			} else {
				getLog().info("Form does not exist on the target server, it will be created");
				febMgmtRESTApi.importApplication(accessType.toString(), filename, deploy, importData, cleanIds);
			}
		} catch (ApiException e) {
			throw new MojoFailureException("An error ocurred while deploying FEB application", e);
		}
	}

	/**
	 * Load username password from settings if user has not set them in JVM
	 * properties
	 * 
	 * @throws MojoExecutionException
	 */
	private void loadUserInfoFromSettings() throws MojoExecutionException {
		if (this.settingsKey == null) {
			this.settingsKey = this.basePath.toString();
		}

		if ((this.username == null || this.password == null) && (this.settings != null)) {
			Server server = this.settings.getServer(this.settingsKey);

			if (server != null) {
				if (this.username == null) {
					this.username = server.getUsername();
				}

				if (this.password == null && server.getPassword() != null) {
					try {
						this.password = securityDispatcher.decrypt(server.getPassword());
					} catch (SecDispatcherException e) {
						throw new MojoExecutionException(e.getMessage());
					}
				}
			}
		}
	}

}