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

package org.sonatype.nexus.examples.virusscan;

import javax.inject.Inject;
import javax.inject.Named;

import org.sonatype.configuration.ConfigurationException;
import org.sonatype.nexus.plugins.RepositoryCustomizer;
import org.sonatype.nexus.proxy.repository.ProxyRepository;
import org.sonatype.nexus.proxy.repository.Repository;
import org.sonatype.nexus.proxy.repository.RequestStrategy;

import com.google.common.base.Preconditions;

/**
 * Configures the {@link VirusScannerRepositoryCustomizer} on all proxy repositories.
 *
 * @since 1.0
 */
@Named(VirusScannerRequestProcessor.ID)
public class VirusScannerRepositoryCustomizer
    implements RepositoryCustomizer
{
  private final RequestStrategy processor;

  @Inject
  public VirusScannerRepositoryCustomizer(final @Named(VirusScannerRequestProcessor.ID) RequestStrategy processor) {
    this.processor = Preconditions.checkNotNull(processor);
  }

  @Override
  public boolean isHandledRepository(final Repository repository) {
    // handle proxy reposes only
    return repository.getRepositoryKind().isFacetAvailable(ProxyRepository.class);
  }

  @Override
  public void configureRepository(final Repository repository) throws ConfigurationException {
    repository.registerRequestStrategy(VirusScannerRequestProcessor.ID, processor);
  }
}
