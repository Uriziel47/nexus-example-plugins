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

package org.sonatype.nexus.examples.selectionactors.selectors;

import javax.inject.Inject;

import org.sonatype.nexus.examples.selectionactors.SelectionFactory;
import org.sonatype.nexus.examples.selectionactors.Selector;
import org.sonatype.sisu.goodies.common.ComponentSupport;

import com.google.common.base.Preconditions;

/**
 * ???
 *
 * @since 1.0
 */
public abstract class AbstractSelector
    extends ComponentSupport
    implements Selector
{
  private final SelectionFactory selectionFactory;

  @Inject
  public AbstractSelector(final SelectionFactory selectionFactory) {
      this.selectionFactory = Preconditions.checkNotNull(selectionFactory);
  }

  protected SelectionFactory getSelectionFactory() {
    return selectionFactory;
  }
}
