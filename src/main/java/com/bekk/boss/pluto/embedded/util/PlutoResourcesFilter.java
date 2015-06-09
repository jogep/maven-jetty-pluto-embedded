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

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.util.Calendar;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class PlutoResourcesFilter implements Filter {

	private ServletContext servletContext;

	private final static Calendar LAST_MODIFIED = Calendar.getInstance();

	public void destroy() {
	}

	public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException,
			ServletException {
		if (request instanceof HttpServletRequest && response instanceof HttpServletResponse) {
			HttpServletRequest httpServletRequest = (HttpServletRequest) request;
			HttpServletResponse httpServletResponse = (HttpServletResponse) response;
			String resource = httpServletRequest.getServletPath();
			if (resource == null) {
				resource = httpServletRequest.getPathInfo();
			}
			URL resourceUrl = servletContext.getResource(resource);
			if (resourceUrl == null) {
				// Serve resource from classpath
				resourceUrl = Thread.currentThread().getContextClassLoader().getResource(resource);
				if (resourceUrl == null && resource.startsWith("/")) {
					resourceUrl = Thread.currentThread().getContextClassLoader().getResource(resource.substring(1));
				}
				if(resourceUrl == null) {
					chain.doFilter(request, response);
				} else {
					Calendar cal = Calendar.getInstance();

					// check for if-modified-since, prior to any other headers
					long ifModifiedSince = 0;
					try {
						ifModifiedSince = httpServletRequest.getDateHeader("If-Modified-Since");
					} catch (Exception e) {

					}
					long lastModifiedMillis = LAST_MODIFIED.getTimeInMillis();
					long now = cal.getTimeInMillis();
					cal.add(Calendar.DAY_OF_MONTH, 1);
					long expires = cal.getTimeInMillis();

					if (ifModifiedSince > 0 && ifModifiedSince <= lastModifiedMillis) {
						// not modified, content is not sent - only basic
						// headers and status SC_NOT_MODIFIED
						httpServletResponse.setDateHeader("Expires", expires);
						httpServletResponse.setStatus(HttpServletResponse.SC_NOT_MODIFIED);
					} else {
						httpServletResponse.setDateHeader("Date", now);
						httpServletResponse.setDateHeader("Expires", expires);
						httpServletResponse.setDateHeader("Retry-After", expires);
						httpServletResponse.setHeader("Cache-Control", "public");
						httpServletResponse.setDateHeader("Last-Modified", lastModifiedMillis);
						InputStream stream = resourceUrl.openStream();
						OutputStream out = response.getOutputStream();
						try {
							int read = stream.read();
							while (read != -1) {
								out.write(read);
								read = stream.read();
							}
						} finally {
								stream.close();
						}
					}
				}
			} else {
				chain.doFilter(request, response);
			}
		} else {
			chain.doFilter(request, response);
		}
	}

	public void init(FilterConfig filterconfig) throws ServletException {
		this.servletContext = filterconfig.getServletContext();
	}

}
