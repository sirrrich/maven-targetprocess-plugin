/**
 * Copyright (C) 2010 http://code.google.com/p/maven-targetprocess-plugin/
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.googlecode.mojo.targetprocess;

import java.text.MessageFormat;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;

import javax.xml.datatype.XMLGregorianCalendar;

import org.apache.maven.artifact.manager.WagonManager;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.wagon.authentication.AuthenticationInfo;
import org.apache.ws.security.WSConstants;
import org.apache.ws.security.handler.WSHandlerConstants;
import org.codehaus.xfire.XFireRuntimeException;
import org.codehaus.xfire.client.Client;
import org.codehaus.xfire.fault.XFireFault;
import org.codehaus.xfire.security.wss4j.WSS4JOutHandler;
import org.codehaus.xfire.util.dom.DOMOutHandler;

import org.stuartgunter.maven.plugins.tp.build.BuildDTO;
import org.stuartgunter.maven.plugins.tp.build.BuildServiceClient;
import org.stuartgunter.maven.plugins.tp.build.BuildServiceSoap;

import com.sun.org.apache.xerces.internal.jaxp.datatype.XMLGregorianCalendarImpl;

/**
 * Creates a Build entity in TargetProcess
 * @author stuart.gunter
 * @goal create-build
 * @phase deploy
 * @requiresOnline true
 * @inheritedByDefault false
 * @aggregator
 */
public class CreateBuildMojo extends AbstractMojo {
	
	/**
	 * @component role="org.apache.maven.artifact.manager.WagonManager"
	 * @required
	 * @readonly
	 */
	private WagonManager wagonManager;
	
	/**
	 * Base URL for TargetProcess - defaults to the project Issue Management URL in POM
	 * @parameter default-value="${project.issueManagement.url}"
	 * @required
	 */
	private String baseUrl;
	
	/**
	 * Server for TargetProcess (references the corresponding server element in settings.xml)
	 * @parameter
	 * @required
	 */
	private String server;
	
	/**
	 * Project name to use for the Build entity
	 * @parameter
	 * @required
	 */
	private String projectName;
	
	/**
	 * Name to use for the Build entity
	 * @parameter
	 * @required
	 */
	private String name;
	
	/**
	 * Iteration ID to use for the Build entity
	 * @parameter
	 */
	private Integer iterationId;
	
	/**
	 * Release name to use for the Build entity
	 * @parameter
	 */
	private String releaseName;
	
	/**
	 * Description to use for the Build entity
	 * @parameter
	 */
	private String description;
	
	/**
	 * Username of System User Credentials
	 * @parameter
	 */
	private String username;
	
	/**
	 * Password of System User Credentials
	 * @parameter
	 */
	private String password;
	
	private ServiceLocator serviceLocator = ServiceLocator.getInstance();
	
	public void execute() throws MojoExecutionException, MojoFailureException {
		
		int buildId = createBuildEntity();
		getLog().info(MessageFormat.format("Created Build \"{0}\" with ID {1,number,integer} in TargetProcess", name, buildId));
	}
	
	private int createBuildEntity() throws MojoFailureException, MojoExecutionException {
		
		try {
			BuildServiceSoap buildService = getBuildService();
			BuildDTO buildDTO = getBuildDTO();
			return buildService.create(buildDTO);
			
		} catch (XFireRuntimeException ex) {
			if (ex.getCause() instanceof XFireFault) {
				XFireFault fault = (XFireFault)ex.getCause();
				throw new MojoFailureException(fault.getMessage());
			} else {
				throw new MojoFailureException(ex.getMessage());
			}
		}
	}
	
	private BuildDTO getBuildDTO() {
		
		Calendar calendar = Calendar.getInstance();
		XMLGregorianCalendar buildDate = XMLGregorianCalendarImpl.createDateTime(
				calendar.get(Calendar.YEAR),
				calendar.get(Calendar.MONTH) + 1,
				calendar.get(Calendar.DAY_OF_MONTH),
				calendar.get(Calendar.HOUR_OF_DAY),
				calendar.get(Calendar.MINUTE),
				calendar.get(Calendar.SECOND));
		
		BuildDTO buildDTO = new BuildDTO();
		buildDTO.setProjectName(projectName);
		buildDTO.setName(name);
		buildDTO.setBuildDate(buildDate);
		
		if (iterationId != null) {
			buildDTO.setIterationID(iterationId);
		}
		
		if (releaseName != null) {
			buildDTO.setReleaseName(releaseName);
		}
		
		if (description != null) {
			buildDTO.setDescription(description);
		}
		
		return buildDTO;
	}
	
	private BuildServiceSoap getBuildService() throws MojoExecutionException {
		
		if (baseUrl == null) {
			throw new MojoExecutionException("Base URL for TargetProcess is missing");
		}
		
		BuildServiceClient buildServiceClient = new BuildServiceClient();
		BuildServiceSoap buildService = buildServiceClient.getBuildServiceSoap(serviceLocator.getServiceURI(baseUrl, ServiceLocator.BUILD_SERVICE));
		
		Client client = Client.getInstance(buildService);
		
		client.addOutHandler(new DOMOutHandler());
		
//		AuthenticationInfo authInfo = wagonManager.getAuthenticationInfo(server);
//		if (authInfo == null) {
//			throw new MojoExecutionException("TargetProcess server authentication details not configured");
//		}
//		
//		if (authInfo.getUserName() == null) {
//			throw new MojoExecutionException("TargetProcess username not configured");
//		}
//		
//		if (authInfo.getPassword() == null) {
//			throw new MojoExecutionException("TargetProcess password not configured");
//		}
		
		PasswordHandler passwordHandler = new PasswordHandler(password);
		
		Map<String, Object> wss4jProperties = new HashMap<String, Object>();
		wss4jProperties.put(WSHandlerConstants.ACTION, WSHandlerConstants.USERNAME_TOKEN);
		wss4jProperties.put(WSHandlerConstants.USER, username);
		wss4jProperties.put(WSHandlerConstants.PASSWORD_TYPE, WSConstants.PW_TEXT);
		wss4jProperties.put(WSHandlerConstants.PW_CALLBACK_REF, passwordHandler);
		
		client.addOutHandler(new WSS4JOutHandler(wss4jProperties));
		
		return buildService;
	}
}
