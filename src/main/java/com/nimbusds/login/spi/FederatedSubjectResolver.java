/*
 * Copyright 2013-2026 Erudika. https://erudika.com
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
 *
 * For issues and patches go to: https://github.com/erudika
 */
package com.nimbusds.login.spi;

import com.nimbusds.login.auth.AuthenticationException;
import com.nimbusds.login.domain.User;
import org.springframework.security.core.Authentication;

/**
 * Resolves a subject (sub) coming from a third-party IdP (OIDC federation).
 * For example, a user coming from Google will have a {@code sub} like {@code google_1234}.
 * That subject ID must be looked up by the RP and mapped to a local account in the Connect2id server.
 * @author Alex Bogdanovski [alex@erudika.com]
 */
public interface FederatedSubjectResolver {

	public User resolve(Authentication authentication) throws AuthenticationException;

}
