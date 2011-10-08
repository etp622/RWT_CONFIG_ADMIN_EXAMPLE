/*******************************************************************************
 * Copyright (c) 2011 Frank Appel and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Frank Appel - initial API and implementation
 ******************************************************************************/
package com.codeaffine.example.rwt.osgi.ui.platform;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;


class PageServiceImpl implements PageService {
  private final LayoutProvider layoutProvider;
  private final LayoutContextImpl layoutContext;
  private final Map<String, UIContributor> pageContributors;
  private final Set<PageTracker> pageTrackers;
  private final Object lock;
  
  private Composite contentParent;
  private UIContributor homePageContributor;

  PageServiceImpl( LayoutProvider layoutProvider, LayoutContextImpl layoutContext ) {
    this.layoutProvider = layoutProvider;
    this.layoutContext = layoutContext;
    this.pageContributors = new HashMap<String,UIContributor>();
    this.pageTrackers = new HashSet<PageTracker>();
    this.lock = new Object();
  }

  @Override
  public void registerContentParent( Composite contentParent ) {
    this.contentParent = contentParent;
  }
  
  public void addPageContributor( UIContributor pageContributor ) {
    Object[] trackers;
    synchronized( lock ) {
      pageContributors.put( pageContributor.getId(), pageContributor );
      trackers = pageTrackers.toArray();
    }
    for( int i = 0; i < trackers.length; i++ ) {
      ( ( PageTracker )trackers[ i ] ).pageAdded( pageContributor );
    }
  }
  
  public void removePageContibutor( UIContributor pageContributor ) {
    Object[] trackers;
    synchronized( lock ) {
      pageContributors.remove( pageContributor );
      trackers = pageTrackers.toArray();
    }
    for( int i = 0; i < trackers.length; i++ ) {
      ( ( PageTracker )trackers[ i ] ).pageRemoved( pageContributor );
    }
  }
  
  void setHomePageContributor( UIContributor homePageContributor ) {
    addPageContributor( homePageContributor );
    this.homePageContributor = homePageContributor;
  }
  
  @Override
  public void selectPage( String pageId ) {
    disposeOfCurrentPageContent();
    createNewContent( pageId );
    layoutShell();
  }
  
  @Override
  public void selectHomePage() {
    if( homePageContributor != null ) {
      selectPage( homePageContributor.getId() );
    } else {
      synchronized( lock ) {
        if( !pageContributors.isEmpty() ) {
          selectPage( ( String )pageContributors.keySet().toArray()[ 0 ] );
        }
      }
    }
  }

  @Override
  public String[] getPageIds() {
    synchronized( lock ) {
      String[] result = new String[ pageContributors.size() ];
      pageContributors.keySet().toArray( result );
      return result;
    }
  }

  private void layoutShell() {
    layoutProvider.layout( layoutContext );
    layoutContext.layoutShell();
  }

  private void createNewContent( String pageId ) {
    UIContributor uiProvider;
    synchronized( lock ) {
      uiProvider = pageContributors.get( pageId );
    }
    uiProvider.contribute( contentParent );
  }

  private void disposeOfCurrentPageContent() {
    Control[] children = contentParent.getChildren();
    for( Control control : children ) {
      control.dispose();
    }
  }

  @Override
  public void addPageTracker( PageTracker pageTracker ) {
    Object[] contributors;
    synchronized( lock ) {
      pageTrackers.add( pageTracker );
      contributors = pageContributors.values().toArray();
    }
    for( int i = 0; i < contributors.length; i++ ) {
      pageTracker.pageAdded( ( UIContributor )contributors[ i ] );
    }
  }

  @Override
  public void removePageTracker( PageTracker pageTracker ) {
    Object[] contributors;
    synchronized( lock ) {
      pageTrackers.remove( pageTracker );
      contributors = pageContributors.values().toArray();
    }
    for( int i = 0; i < contributors.length; i++ ) {
      pageTracker.pageRemoved( ( UIContributor )contributors[ i ] );
    }
  }
}