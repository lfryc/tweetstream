/*
 * JBoss, Home of Professional Open Source
 * Copyright 2011, Red Hat, Inc. and individual contributors
 * by the @authors tag. See the copyright.txt in the distribution for a
 * full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.richfaces.examples.tweetstream.dataserver.source;

import org.richfaces.examples.tweetstream.dataserver.cache.InfinispanCacheBuilder;
import org.richfaces.examples.tweetstream.dataserver.listeners.TweetStreamListener;
import org.richfaces.examples.tweetstream.dataserver.listeners.CacheUpdateListener;
import org.richfaces.examples.tweetstream.domain.*;
import org.richfaces.examples.tweetstream.domain.Tweet;
import twitter4j.*;

import javax.annotation.PostConstruct;
import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Alternative;
import javax.faces.context.FacesContext;
import javax.inject.Inject;
import java.util.ArrayList;
import java.util.List;

/**
 * An implementation of the twitter source interfaces that will
 * pull the initial content from the containers Cache manager.
 *
 * @author <a href="mailto:jbalunas@redhat.com">Jay Balunas</a>
 */
@ApplicationScoped
@Alternative
public class TwitterSourceServer implements TwitterSource {

  @Inject
  org.slf4j.Logger log;

  @Inject
  InfinispanCacheBuilder cacheBuilder;

  @Inject
  TweetStreamListener tweetListener;


  private TwitterAggregate twitterAggregate;

  @PostConstruct
  private void init() {

    //TODO Remove this top part once we integrate with server side
    fetchContent();

    // add the listener that checks hi new data has been added.
    cacheBuilder.getCache().addListener(new CacheUpdateListener());

    //Populate cache with seed data from this class
    cacheBuilder.getCache().put("tweetaggregate", twitterAggregate);
    System.out.println("-------cacheBuilder.getCache().--" + cacheBuilder.getCache().containsKey("tweetaggregate"));

    //Start the twitter streaming
    tweetListener.startTwitterStream();


    //TODO setup connection/injection point etc... to interact with server content
    //Likely will be injected above.

    //TODO Load the filter/search term from server
    //TODO try/catch as needed
    fetchContent();

    //TODO Trigger polling of server, which will push updates.


    log.info("Initialization of twitter source server complete");
  }

  @Override
  public String getSearchTerm() {
    return twitterAggregate.getFilter();
  }

  public List<Tweet> getTweets() {
    return twitterAggregate.getTweets();
  }

  public List<Tweeter> getTopTweeters() {
    return twitterAggregate.getTopTweeters();
  }

  public List<HashTag> getTopHashtags() {
    return twitterAggregate.getTopHashTags();
  }

  @Override
  public TwitterAggregate getTwitterAggregate() {
    return twitterAggregate;
  }

  @Override
  public void fetchContent() {
    //TODO check if updating data is required
    //  this method can be called on every page load

    twitterAggregate = new TwitterAggregate();

    //Load the base search term from context param
    String searchTerm = FacesContext.getCurrentInstance().getExternalContext().getInitParameter("org.richfaces.examples.tweetstream.searchTermBase");

    if (searchTerm == null) {
      searchTerm = "";
      log.warn("Default initial twitter filter term not found in context params");
    }

    twitterAggregate.setFilter(searchTerm);

    //Load the twitter search
    List<Tweet> tweets = new ArrayList<Tweet>();

    Twitter twitter = new TwitterFactory().getInstance();
    List<twitter4j.Tweet> t4jTweets = null;
    try {
      QueryResult result = twitter.search(new Query(searchTerm));
      t4jTweets = result.getTweets();
      for (twitter4j.Tweet t4jTweet : t4jTweets) {
        log.info("@" + t4jTweet.getFromUser() + " - " + t4jTweet.getText());
        //Create a local tweet object from the t4j
        Tweet tweet = new Tweet();
        tweet.setText(t4jTweet.getText());
        tweet.setId(t4jTweet.getFromUserId());
        tweet.setProfileImageUrl(t4jTweet.getProfileImageUrl().toString());
        tweet.setScreenName(t4jTweet.getFromUser());
        //TODO fill in any other required data
        tweets.add(tweet);
      }
    } catch (TwitterException te) {
      te.printStackTrace();
      log.info("Failed to search tweets: " + te.getMessage());
    }

    twitterAggregate.setTweets(tweets);

    //Load TopTweeters
    List<Tweeter> tweeters = new ArrayList<Tweeter>();

    Tweeter tweeter = null;
    for (int i = 0; i < 10; i++) {
      tweeter = new Tweeter();
      tweeter.setProfileImgUrl("http://twitter.com/account/profile_image/tech4j?hreflang=en");
      tweeter.setTweetCount(100 - (2 * i));
      tweeter.setUser("tech4j_" + i);
      tweeter.setUserId(32423444);
      tweeters.add(tweeter);
    }

    twitterAggregate.setTopTweeters(tweeters);

    //Load TopTags
    List<HashTag> hashTags = new ArrayList<HashTag>();

    HashTag hashTag = null;
    for (int i = 0; i < 10; i++) {
      hashTag = new HashTag();
      hashTag.setHashtag("#richfaces_" + i);
      hashTag.setCount(1000 - (5 * i));
      hashTags.add(hashTag);
    }

    twitterAggregate.setTopHashTags(hashTags);

  }


}