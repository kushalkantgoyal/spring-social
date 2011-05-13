/*
 * Copyright 2010 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.social.security.provider;

import java.util.HashSet;
import java.util.Set;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.social.connect.ConnectionData;
import org.springframework.social.connect.support.OAuth1ConnectionFactory;
import org.springframework.social.oauth1.AuthorizedRequestToken;
import org.springframework.social.oauth1.OAuth1Operations;
import org.springframework.social.oauth1.OAuth1Parameters;
import org.springframework.social.oauth1.OAuth1Version;
import org.springframework.social.oauth1.OAuthToken;
import org.springframework.social.security.SocialAuthenticationRedirectException;
import org.springframework.social.security.SocialAuthenticationToken;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

public class OAuth1AuthenticationService<S> extends AbstractSocialAuthenticationService<S> implements InitializingBean {

	private static final String OAUTH_TOKEN_ATTRIBUTE = "oauthToken";

	private Set<String> returnToUrlParameters;
	private OAuth1ConnectionFactory<S> connectionFactory;

	public OAuth1AuthenticationService() {
		super(AuthenticationMode.EXPLICIT);
	}

	public OAuth1AuthenticationService(OAuth1ConnectionFactory<S> connectionFactory) {
		this();
		setConnectionFactory(connectionFactory);
	}

	public void afterPropertiesSet() throws Exception {
		super.afterPropertiesSet();
		Assert.notNull(getConnectionFactory(), "connectionFactory");
	}

	public SocialAuthenticationToken getAuthToken(AuthenticationMode authMode, HttpServletRequest request,
			HttpServletResponse response) throws SocialAuthenticationRedirectException {

		if (authMode != AuthenticationMode.EXPLICIT) {
			return null;
		}

		/**
		 * OAuth Authentication flow: See http://dev.twitter.com/pages/auth
		 */
		String verifier = request.getParameter("oauth_verifier");
		if (!StringUtils.hasText(verifier)) {
			// First phase: get a request token
			OAuth1Operations ops = getConnectionFactory().getOAuthOperations();

			String returnToUrl = buildReturnToUrl(request);
			OAuthToken requestToken = ops.fetchRequestToken(returnToUrl, null);
			request.getSession().setAttribute(OAUTH_TOKEN_ATTRIBUTE, requestToken);

			// Redirect to the service provider for authorization
			String oAuthUrl = ops.buildAuthenticateUrl(requestToken.getValue(),
					ops.getVersion() == OAuth1Version.CORE_10 ? new OAuth1Parameters(returnToUrl)
							: OAuth1Parameters.NONE);
			throw new SocialAuthenticationRedirectException(oAuthUrl);
		} else {
			// Second phase: request an access token
			OAuthToken accessToken = getConnectionFactory().getOAuthOperations().exchangeForAccessToken(
					new AuthorizedRequestToken(extractCachedRequestToken(request), verifier), null);

			// TODO avoid API call if possible (auth using token would be fine)
			ConnectionData data = getConnectionFactory().createConnection(accessToken).createData();
			return new SocialAuthenticationToken(data, null);
		}
	}

	protected String buildReturnToUrl(HttpServletRequest request) {
		StringBuffer sb = request.getRequestURL();
		sb.append("?");

		for (String name : getReturnToUrlParameters()) {
			// Assume for simplicity that there is only one value
			String value = request.getParameter(name);

			if (value == null) {
				continue;
			}
			sb.append(name).append("=").append(value).append("&");

		}

		sb.setLength(sb.length() - 1); // strip trailing ? or &

		return sb.toString();
	}

	private OAuthToken extractCachedRequestToken(HttpServletRequest request) {
		OAuthToken requestToken = (OAuthToken) request.getSession().getAttribute(OAUTH_TOKEN_ATTRIBUTE);
		request.getSession().removeAttribute(OAUTH_TOKEN_ATTRIBUTE);
		return requestToken;
	}

	public OAuth1ConnectionFactory<S> getConnectionFactory() {
		return connectionFactory;
	}

	public void setConnectionFactory(OAuth1ConnectionFactory<S> connectionFactory) {
		this.connectionFactory = connectionFactory;
	}

	public void setReturnToUrlParameters(Set<String> returnToUrlParameters) {
		Assert.notNull(returnToUrlParameters, "returnToUrlParameters cannot be null");
		this.returnToUrlParameters = returnToUrlParameters;
	}

	public Set<String> getReturnToUrlParameters() {
		if (returnToUrlParameters == null) {
			returnToUrlParameters = new HashSet<String>();
		}
		return returnToUrlParameters;
	}

}
