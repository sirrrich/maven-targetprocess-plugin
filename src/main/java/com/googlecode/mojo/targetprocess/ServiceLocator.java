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


public class ServiceLocator {

	public static final String BUILD_SERVICE = "BuildService";
	
	private static ServiceLocator instance;
	
	private ServiceLocator() { }
	
	static {
		instance = new ServiceLocator();
	}
	
	public static ServiceLocator getInstance() {
		return instance;
	}
	
	public String getServiceURI(String baseUrl, String serviceName) {
		StringBuilder url = new StringBuilder(baseUrl);
		
		if (url.charAt(url.length() - 1) == '/') {
			url.append("Services/");
		} else {
			url.append("/Services/");
		}
		
		url.append(serviceName);
		url.append(".asmx");
		
		return url.toString();
	}
}
