package com.orange.signsatwork.biz.webservice.controller;

/*
 * #%L
 * Signs at work
 * %%
 * Copyright (C) 2016 Orange
 * %%
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as
 * published by the Free Software Foundation, either version 2 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public
 * License along with this program.  If not, see
 * <http://www.gnu.org/licenses/gpl-2.0.html>.
 * #L%
 */

import com.orange.signsatwork.DalymotionToken;
import com.orange.signsatwork.SpringRestClient;
import com.orange.signsatwork.biz.domain.*;
import com.orange.signsatwork.biz.persistence.model.VideoViewData;
import com.orange.signsatwork.biz.persistence.service.MessageByLocaleService;
import com.orange.signsatwork.biz.persistence.service.Services;
import com.orange.signsatwork.biz.view.model.*;
import com.orange.signsatwork.biz.webservice.model.*;
import com.orange.signsatwork.biz.webservice.model.SignView;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.annotation.Secured;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.security.Principal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Types that carry this annotation are treated as controllers where @RequestMapping
 * methods assume @ResponseBody semantics by default, ie return json body.
 */
@Slf4j
@RestController
/** Rest controller: returns a json body */
public class FavoriteRestController {

  @Autowired
  Services services;

  @Autowired
  MessageByLocaleService messageByLocaleService;

  @Secured("ROLE_USER")
  @RequestMapping(value = RestApi.WS_SEC_FAVORITE_VIDEO_ASSOCIATE, method = RequestMethod.POST)
  public String favoriteAssociateVideo(@RequestBody List<Long> favoriteVideosIds, @PathVariable long favoriteId, HttpServletResponse response) {

    services.favorite().changeFavoriteVideos(favoriteId, favoriteVideosIds);

    response.setStatus(HttpServletResponse.SC_OK);
    return "/sec/favorite/" + favoriteId;
  }

  @Secured("ROLE_USER")
  @RequestMapping(value = RestApi.WS_SEC_FAVORITE_COMMUNITY_ASSOCIATE, method = RequestMethod.POST)
  public String favoriteAssociateCommunity(@RequestBody List<Long> favoriteCommunitiesIds, @PathVariable long favoriteId, Principal principal, HttpServletResponse response, HttpServletRequest request) {

    User user = services.user().withUserName(principal.getName());
    Favorite favorite = services.favorite().changeFavoriteCommunities(favoriteId, favoriteCommunitiesIds, user.name(), getAppUrl(request));
    favorite = favorite.loadCommunities();
    response.setStatus(HttpServletResponse.SC_OK);
    List<String> communitiesName = favorite.communities.stream().map(c -> c.name).collect(Collectors.toList());

    return messageByLocaleService.getMessage("favorite.confirm_share_to_community",  new Object[]{favorite.favoriteName(), communitiesName.toString()});
  }

  private String getAppUrl(HttpServletRequest request) {
    return request.getScheme() + "://" + request.getServerName() + ":" + request.getServerPort() + request.getContextPath();
  }

  /** API REST For Android and IOS **/
  @Secured("ROLE_USER")
  @RequestMapping(value = RestApi.WS_SEC_MY_FAVORITES)
  public ResponseEntity<?> myFavorites(Principal principal) {

    User user = services.user().withUserName(principal.getName());

    List<FavoriteViewApi> myFavorites = FavoriteViewApi.from(services.favorite().favoritesforUser(user.id));

    return  new ResponseEntity<>(myFavorites, HttpStatus.OK);
  }

  @Secured("ROLE_USER")
  @RequestMapping(value = RestApi.WS_SEC_FAVORITES)
  public ResponseEntity<?> favorites(Principal principal) {

    User user = services.user().withUserName(principal.getName());

    List<FavoriteViewApi> myFavorites = fillModelWithFavorites(user);

    return  new ResponseEntity<>(myFavorites, HttpStatus.OK);
  }


  private List<FavoriteViewApi> fillModelWithFavorites(User user) {
    List<FavoriteViewApi> favorites = new ArrayList<>();
    List<FavoriteViewApi> newFavoritesShareToMe = FavoriteViewApi.fromNewShare(services.favorite().newFavoritesShareToUser(user.id));
    favorites.addAll(newFavoritesShareToMe);

    List<FavoriteViewApi> favoritesAlpha = new ArrayList<>();
    List<FavoriteViewApi> oldFavoritesShareToMe = FavoriteViewApi.from(services.favorite().oldFavoritesShareToUser(user.id));
    favoritesAlpha.addAll(oldFavoritesShareToMe);
    List<FavoriteViewApi> myFavorites = FavoriteViewApi.from(services.favorite().favoritesforUser(user.id));
    favoritesAlpha.addAll(myFavorites);
    favoritesAlpha = favoritesAlpha.stream().sorted((f1, f2) -> f1.getName().compareTo(f2.getName())).collect(Collectors.toList());
    favorites.addAll(favoritesAlpha);

    return favorites;
  }

  @Secured("ROLE_USER")
  @RequestMapping(value = RestApi.WS_SEC_FAVORITE)
  public ResponseEntity<?> favorite(@PathVariable long favoriteId, Principal principal) {

    String messageError;
    User user = services.user().withUserName(principal.getName());
    List<FavoriteViewApi> myFavorites = FavoriteViewApi.from(services.favorite().favoritesforUser(user.id));

    boolean isFavoriteBelowToMe = myFavorites.stream().anyMatch(favoriteModalView -> favoriteModalView.getId() == favoriteId);
    if (!isFavoriteBelowToMe) {
      messageError = messageByLocaleService.getMessage("favorite_not_below_to_you");
      return new ResponseEntity<>(messageError, HttpStatus.FORBIDDEN);
    }

    FavoriteViewApi myFavorite = FavoriteViewApi.from(services.favorite().withId(favoriteId));

    return  new ResponseEntity<>(myFavorite, HttpStatus.OK);
  }

  @Secured("ROLE_USER")
  @RequestMapping(value = RestApi.WS_SEC_FAVORITES_VIDEOS)
  public ResponseEntity<?> videosFavorite(@PathVariable long favoriteId, Principal principal) {

    String messageError;
    User user = services.user().withUserName(principal.getName());
    Favorite favorite = services.favorite().withId(favoriteId);
    if (favorite.type == FavoriteType.Individual) {
      List<FavoriteViewApi> myFavorites = FavoriteViewApi.from(services.favorite().favoritesforUser(user.id));
      boolean isFavoriteBelowToMe = myFavorites.stream().anyMatch(favoriteModalView -> favoriteModalView.getId() == favoriteId);
      if (!isFavoriteBelowToMe) {
        messageError = messageByLocaleService.getMessage("favorite_not_below_to_you");
        return new ResponseEntity<>(messageError, HttpStatus.FORBIDDEN);
      }
    }

    List<Object[]> queryVideos = services.video().VideosForFavoriteView(favoriteId);
    List<VideoViewData> videoViewsData = queryVideos.stream()
      .map(objectArray -> new VideoViewData(objectArray))
      .collect(Collectors.toList());

    List<Long> videoWithCommentList = Arrays.asList(services.favorite().NbCommentForAllVideoByFavorite(favoriteId));


    List<VideoView2>  videoViews = videoViewsData.stream()
      .map(videoViewData -> new VideoView2(
        videoViewData,
        videoWithCommentList.contains(videoViewData.videoId),
        VideoView2.createdAfterLastDeconnection(videoViewData.createDate, user == null ? null : user.lastDeconnectionDate),
        videoViewData.nbView > 0,
        videoViewData.averageRate > 0,
        true))
      .collect(Collectors.toList());


    VideosViewSort videosViewSort = new VideosViewSort();
    videoViews = videosViewSort.sort(videoViews);


    return  new ResponseEntity<>(videoViews, HttpStatus.OK);
  }

  @Secured("ROLE_USER")
  @RequestMapping(value = RestApi.WS_SEC_FAVORITE, method = RequestMethod.DELETE)
  public FavoriteResponseApi deleteFavorite(@PathVariable long favoriteId, HttpServletResponse response, Principal principal)  {

    FavoriteResponseApi favoriteResponseApi = new FavoriteResponseApi();
    User user = services.user().withUserName(principal.getName());
    List<FavoriteViewApi> myFavorites = FavoriteViewApi.from(services.favorite().favoritesforUser(user.id));

    boolean isFavoriteBelowToMe = myFavorites.stream().anyMatch(favoriteModalView -> favoriteModalView.getId() == favoriteId);
    if (!isFavoriteBelowToMe) {
      response.setStatus(HttpServletResponse.SC_FORBIDDEN);
      favoriteResponseApi.errorMessage = messageByLocaleService.getMessage("favorite_not_below_to_you");
      return favoriteResponseApi;
    }

    Favorite favorite = services.favorite().withId(favoriteId);
    services.favorite().delete(favorite);

    response.setStatus(HttpServletResponse.SC_OK);
    return favoriteResponseApi;
  }

  @Secured("ROLE_USER")
  @RequestMapping(value = RestApi.WS_SEC_FAVORITES, method = RequestMethod.POST)
  public FavoriteResponseApi createFavorite(@RequestBody FavoriteCreationViewApi favoriteCreationViewApi, HttpServletResponse response, Principal principal) {
    FavoriteResponseApi favoriteResponseApi = new FavoriteResponseApi();
    User user = services.user().withUserName(principal.getName());

    Favorite favorite = services.favorite().create(user.id, favoriteCreationViewApi.getName());

    if (favoriteCreationViewApi.getVideoIdToAdd() != null) {
      services.favorite().changeFavoriteVideos(favorite.id, java.util.Arrays.asList(favoriteCreationViewApi.getVideoIdToAdd()));
    }

    response.setStatus(HttpServletResponse.SC_OK);
    favoriteResponseApi.favoriteId = favorite.id;
    return favoriteResponseApi;

  }

  @Secured("ROLE_USER")
  @RequestMapping(value = RestApi.WS_SEC_FAVORITE, method = RequestMethod.PUT)
  public FavoriteResponseApi updateFavorite(@RequestBody FavoriteCreationViewApi favoriteCreationViewApi, @PathVariable long favoriteId, HttpServletResponse response, Principal principal) {

    FavoriteResponseApi favoriteResponseApi = new FavoriteResponseApi();
    User user = services.user().withUserName(principal.getName());
    List<FavoriteViewApi> myFavorites = FavoriteViewApi.from(services.favorite().favoritesforUser(user.id));

    boolean isFavoriteBelowToMe = myFavorites.stream().anyMatch(favoriteModalView -> favoriteModalView.getId() == favoriteId);
    if (!isFavoriteBelowToMe) {
      response.setStatus(HttpServletResponse.SC_FORBIDDEN);
      favoriteResponseApi.errorMessage = messageByLocaleService.getMessage("favorite_not_below_to_you");
      return favoriteResponseApi;
    }

    Favorite favorite = services.favorite().withId(favoriteId);
    if (favoriteCreationViewApi.getName() != null) {
      if (!favorite.name.equals(favoriteCreationViewApi.getName())) {
        Long maxIdForName = services.favorite().maxIdForName(favoriteCreationViewApi.getName(), favoriteId);
        if (maxIdForName != null) {
            favoriteResponseApi.errorMessage = messageByLocaleService.getMessage("favorite.name_already_exist");
            response.setStatus(HttpServletResponse.SC_CONFLICT);
            return favoriteResponseApi;
        }
        services.favorite().updateName(favoriteId, favoriteCreationViewApi.getName());
        favoriteResponseApi.errorMessage = messageByLocaleService.getMessage("favorite.renamed", new Object[]{favorite.name, favoriteCreationViewApi.getName()});
      } else {
        favoriteResponseApi.errorMessage = messageByLocaleService.getMessage("favorite.name_already_exist");
        response.setStatus(HttpServletResponse.SC_CONFLICT);
        return favoriteResponseApi;
      }
    }

    if (favoriteCreationViewApi.getVideosIds() != null) {
      services.favorite().changeFavoriteVideos(favorite.id, favoriteCreationViewApi.getVideosIds());
    } else {
      if (favoriteCreationViewApi.getVideoIdToAdd() != null) {
        Long videoIdToAdd = favoriteCreationViewApi.getVideoIdToAdd();
        favorite = favorite.loadVideos();
        List<Long> videosIds = favorite.videosIds();
        if (!videosIds.contains(videoIdToAdd)) {
          videosIds.add(videoIdToAdd);
          services.favorite().changeFavoriteVideos(favorite.id, videosIds);
        }
      }
    }

    response.setStatus(HttpServletResponse.SC_OK);
    return favoriteResponseApi;

  }

  @Secured("ROLE_USER")
  @RequestMapping(value = RestApi.WS_SEC_FAVORITE_DUPLICATE, method = RequestMethod.POST)
  public FavoriteResponseApi duplicateFavorite(@RequestBody FavoriteCreationViewApi favoriteCreationViewApi, @PathVariable long favoriteId, HttpServletResponse response, Principal principal) {

    FavoriteResponseApi favoriteResponseApi = new FavoriteResponseApi();
    User user = services.user().withUserName(principal.getName());
    List<FavoriteViewApi> myFavorites = FavoriteViewApi.from(services.favorite().favoritesforUser(user.id));

    boolean isFavoriteBelowToMe = myFavorites.stream().anyMatch(favoriteModalView -> favoriteModalView.getId() == favoriteId);
    if (!isFavoriteBelowToMe) {
      response.setStatus(HttpServletResponse.SC_FORBIDDEN);
      favoriteResponseApi.errorMessage = messageByLocaleService.getMessage("favorite_not_below_to_you");
      return favoriteResponseApi;
    }

    Long maxIdForName = services.favorite().maxIdForName(favoriteCreationViewApi.getName(), favoriteId);
    if (maxIdForName != null) {
      favoriteResponseApi.errorMessage = messageByLocaleService.getMessage("favorite.name_already_exist");
      response.setStatus(HttpServletResponse.SC_CONFLICT);
      return favoriteResponseApi;
    }

    Favorite favorite = services.favorite().withId(favoriteId);
    favorite = favorite.loadVideos();
    Favorite duplicateFavorite = services.favorite().create(user.id, favoriteCreationViewApi.getName());
    if (favorite.videos != null) {
      services.favorite().changeFavoriteVideos(duplicateFavorite.id, favorite.videosIds());
    }
    favoriteResponseApi.favoriteId = duplicateFavorite.id;
    favoriteResponseApi.errorMessage = messageByLocaleService.getMessage("favorite.duplicated", new Object[]{favorite.name, favoriteCreationViewApi.getName()});

    response.setStatus(HttpServletResponse.SC_OK);
    return favoriteResponseApi;

  }

}
