/* $Id$
 * 
 * Copyright 2007 BEKK Consulting
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); 
 * you may not use this file except in compliance with the License. 
 * You may obtain a copy of the License at
 * 
 *     http://www.apache.org/licenses/LICENSE-2.0
 *     
 *  Unless required by applicable law or agreed to in writing, software 
 *  distributed under the License is distributed on an "AS IS" BASIS, 
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. 
 *  See the License for the specific language governing permissions and 
 *  limitations under the License.
 */
package com.bekk.boss.pluto.embedded.util;

import org.mortbay.jetty.handler.ContextHandler;
import org.mortbay.jetty.servlet.Context;
import org.springframework.web.context.ContextLoader;
import org.springframework.web.context.ContextLoaderListener;

import javax.servlet.ServletContext;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import java.util.ArrayList;
import java.util.EventListener;
import java.util.List;
import java.util.Map;

public class PortalStartupListener implements ServletContextListener {

	private org.apache.pluto.driver.PortalStartupListener realStartupListener;

    private static final String PLUTO_SERVICE_CONFIG_LOCATION = "classpath:/pluto-portal-driver-services-config.xml";
	
	public PortalStartupListener() {
		this.realStartupListener = new org.apache.pluto.driver.PortalStartupListener();
	}
	
	public void contextDestroyed(ServletContextEvent sce) {
		realStartupListener.contextDestroyed(sce);
	}

	public void contextInitialized(ServletContextEvent sce) {
        Context.SContext servletContext = (Context.SContext) sce.getServletContext();
		ContextHandler contextHandler = servletContext.getContextHandler();
		Map initParams = contextHandler.getInitParams();
		String configLocations = (String) initParams.get(ContextLoader.CONFIG_LOCATION_PARAM);
		if (configLocations != null) {
			configLocations = configLocations + " " + PLUTO_SERVICE_CONFIG_LOCATION;
			initParams.put(ContextLoader.CONFIG_LOCATION_PARAM, configLocations);
		}
		else {
			initParams.put(ContextLoader.CONFIG_LOCATION_PARAM, PLUTO_SERVICE_CONFIG_LOCATION);
		}
		// Traverse listeners to see if there's a configured
		// ContextLoaderListener
		EventListener[] listeners = contextHandler.getEventListeners();
		if (listeners != null) {
			List newListenerList = new ArrayList();
			for (int i = 0; i < listeners.length; i++) {
				EventListener listener = listeners[i];
				if (!(listener instanceof ContextLoaderListener)) {
					newListenerList.add(listener);
				}
			}
			contextHandler.setEventListeners((EventListener[])newListenerList.toArray(new EventListener[newListenerList.size()]));
		}
		ServletContext wrapped = new WrappedServletContext(sce.getServletContext());
		realStartupListener.contextInitialized(new ServletContextEvent(wrapped));
	}

}
