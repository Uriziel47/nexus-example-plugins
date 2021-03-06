/*
 * Copyright (c) 2007-2012 Sonatype, Inc. All rights reserved.
 *
 * This program is licensed to you under the Apache License Version 2.0,
 * and you may not use this file except in compliance with the Apache License Version 2.0.
 * You may obtain a copy of the Apache License Version 2.0 at http://www.apache.org/licenses/LICENSE-2.0.
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the Apache License Version 2.0 is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the Apache License Version 2.0 for the specific language governing permissions and limitations there under.
 */

package org.sonatype.nexus.examples.url;

import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.sonatype.nexus.apachehttpclient.Hc4Provider;
import org.sonatype.nexus.configuration.application.ApplicationConfiguration;
import org.sonatype.nexus.proxy.storage.remote.DefaultRemoteStorageContext;
import org.sonatype.nexus.proxy.storage.remote.RemoteStorageContext;
import org.sonatype.sisu.litmus.testsupport.TestSupport;
import org.sonatype.tests.http.server.api.Behaviour;
import org.sonatype.tests.http.server.fluent.Behaviours;
import org.sonatype.tests.http.server.fluent.Server;

import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.shiro.authc.AuthenticationInfo;
import org.apache.shiro.authc.UnknownAccountException;
import org.apache.shiro.authc.UsernamePasswordToken;
import org.apache.shiro.authz.UnauthorizedException;
import org.apache.shiro.subject.SimplePrincipalCollection;
import org.eclipse.jetty.util.B64Code;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.Mockito;

import static org.hamcrest.MatcherAssert.*;
import static org.hamcrest.Matchers.*;
import static org.mockito.Mockito.when;

public class UrlRealmTest
    extends TestSupport
{
  protected static final String username = "popeye";

  protected static final String password = "spinach";

  protected static final String DEFAULT_ROLE = "default-url-role";

  @Mock
  protected ApplicationConfiguration applicationConfiguration;

  @Mock
  protected Hc4Provider hc4Provider;

  protected Server server;

  protected CloseableHttpClient httpClient;

  protected UrlRealm urlRealm;

  /**
   * HTTP Basic auth, copied and fixed from org.sonatype.tests.http.server.jetty.behaviour.BasicAuth
   * Fixed to challenge if 401 is the response, hence, properly implements HTTP Basic, unlike the one from Testsuite.
   */
  public static class BasicAuth
      implements Behaviour
  {
    private final String password;

    private final String user;

    public BasicAuth(String user, String password) {
      this.user = user;
      this.password = password;
    }

    public boolean execute(HttpServletRequest request, HttpServletResponse response, Map<Object, Object> ctx)
        throws Exception
    {
      String userPass = new String(B64Code.encode((user + ":" + password).getBytes("UTF-8")));
      if (("Basic " + userPass).equals(request.getHeader("Authorization"))) {
        return true;
      }
      response.setHeader("WWW-Authenticate", "Basic realm=\"Secure\"");
      response.sendError(401, "not authorized");
      return false;
    }
  }

  @Before
  public void prepare() throws Exception {
    server = Server.server().port(0).serve("/*").withBehaviours(new BasicAuth(username, password),
        Behaviours.content("not important"));
    server.start();

    // prepare URLRealm
    when(applicationConfiguration.getGlobalRemoteStorageContext()).thenReturn(new DefaultRemoteStorageContext(null));

    urlRealm = new UrlRealm(applicationConfiguration, hc4Provider, server.getUrl().toString() + "/foo", DEFAULT_ROLE);
  }

  @After
  public void cleanUp() throws Exception {
    try {
      if (httpClient != null) {
        httpClient.close();
      }
    }
    finally {
      server.stop();
    }
  }

  // ==

  @Test
  public void testAuthc()
      throws Exception
  {
    // build client
    BasicCredentialsProvider credsProvider = new BasicCredentialsProvider();
    credsProvider.setCredentials(
        AuthScope.ANY,
        new UsernamePasswordCredentials(username, password));
    httpClient = HttpClients.custom()
        .setDefaultCredentialsProvider(credsProvider)
        .build();
    when(hc4Provider.createHttpClient(Mockito.any(RemoteStorageContext.class))).thenReturn(httpClient);

    final AuthenticationInfo info = urlRealm.getAuthenticationInfo(new UsernamePasswordToken(username, password));
    assertThat(info, notNullValue());
  }

  @Test(expected = UnknownAccountException.class)
  public void testAuthcWrongCreds()
      throws Exception
  {
    // build client
    BasicCredentialsProvider credsProvider = new BasicCredentialsProvider();
    credsProvider.setCredentials(
        AuthScope.ANY,
        new UsernamePasswordCredentials(username, "cake"));
    httpClient = HttpClients.custom()
        .setDefaultCredentialsProvider(credsProvider)
        .build();
    when(hc4Provider.createHttpClient(Mockito.any(RemoteStorageContext.class))).thenReturn(httpClient);

    urlRealm.getAuthenticationInfo(new UsernamePasswordToken(username, "cake"));
  }

  @Test(expected = UnknownAccountException.class)
  public void testAuthcJunkCreds()
      throws Exception
  {
    // build client
    BasicCredentialsProvider credsProvider = new BasicCredentialsProvider();
    credsProvider.setCredentials(
        AuthScope.ANY,
        new UsernamePasswordCredentials("fakeuser", "hack-me-in"));
    httpClient = HttpClients.custom()
        .setDefaultCredentialsProvider(credsProvider)
        .build();
    when(hc4Provider.createHttpClient(Mockito.any(RemoteStorageContext.class))).thenReturn(httpClient);

    urlRealm.getAuthenticationInfo(new UsernamePasswordToken("fakeuser", "hack-me-in"));
  }

  @Test
  public void testAuthz()
      throws Exception
  {
    urlRealm.checkRole(new SimplePrincipalCollection(username, urlRealm.getName()), DEFAULT_ROLE);
  }

  @Test(expected = UnauthorizedException.class)
  public void testAuthzWrongRole()
      throws Exception
  {
    urlRealm.checkRole(new SimplePrincipalCollection(username, urlRealm.getName()), "Not-Existing-Role");
  }
}
