/*
 *   Copyright (c) 2014 Program Analysis Group, Georgia Tech
 *
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 */
package edu.gatech.sqltutor.beans;

import javax.faces.application.FacesMessage;
import javax.faces.context.FacesContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

public class BeanUtils extends AbstractDatabaseBean {

	public static String getIPAddress() {
		HttpServletRequest request = (HttpServletRequest) FacesContext.getCurrentInstance().getExternalContext().getRequest();
		String ip = request.getHeader("X-FORWARDED-FOR");
		if (ip == null) {
		    ip = request.getRemoteAddr();
		}
		return ip;
	}
	
	public static String getSessionId() {
		FacesContext fCtx = FacesContext.getCurrentInstance();
		HttpSession session = (HttpSession) fCtx.getExternalContext().getSession(false);
		return session.getId();
	}	
	
	public static void addErrorMessage(String componentName, String message) {
		String componentId = getComponentId(componentName);
		FacesContext.getCurrentInstance().addMessage(componentId, new FacesMessage(FacesMessage.SEVERITY_ERROR, message, null));
	}
	
	public static void addInfoMessage(String componentName, String message) {
		String componentId = getComponentId(componentName);
		FacesContext.getCurrentInstance().addMessage(componentId, new FacesMessage(FacesMessage.SEVERITY_INFO, message, null));
	}
	
	public static String getComponentId(String componentName) {
		String componentId = null;
		if(componentName != null)
			componentId = FacesContext.getCurrentInstance().getViewRoot().findComponent(componentName).getClientId();
		return componentId;
	}
}
