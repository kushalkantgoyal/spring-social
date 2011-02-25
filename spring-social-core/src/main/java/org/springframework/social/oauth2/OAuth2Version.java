/*
 * Copyright 2011 the original author or authors.
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
package org.springframework.social.oauth2;

/**
 * Enum encapsulating the differences between the various versions of the OAuth2 specification.
 * @author Keith Donald
 * @author Craig Walls
 */
enum OAuth2Version {

	STANDARD {
		public String getAuthorizationHeaderValue(String accessToken) {
			return "BEARER " + accessToken;
		}		
	},
	
	DRAFT_10 {
		public String getAuthorizationHeaderValue(String accessToken) {
			return "OAuth " + accessToken + "";
		}
	},

	DRAFT_8 {
		public String getAuthorizationHeaderValue(String accessToken) {
			return "Token token=\"" + accessToken + "\"";
		}			
	};
	
	public abstract String getAuthorizationHeaderValue(String accessToken);		
}