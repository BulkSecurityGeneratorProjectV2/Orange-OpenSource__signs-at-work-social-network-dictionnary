package com.orange.signsatwork.biz.persistence.service;

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

public interface EmailService {

  public void sendSimpleMessage(String to, String subject, String text);

  public void sendRequestMessage(String[] to, String subject, String userName, String requestName, String url);

  public void sendFavoriteShareMessage(String[] to, String subject, String userName, String favoriteName, String url);

  public void sendCommunityCreateMessage(String[] to, String subject, String userName, String communityName, String url);

  public void sendCommunityDeleteMessage(String[] to, String subject, String userName, String communityName);

  public void sendCommunityRemoveMessage(String[] to, String subject, String communityName);

  public void sendCommunityRenameMessage(String[] to, String subject, String oldName, String newName, String url);

  public void sendResetPasswordMessage(String to, String subject, String url);

  public void sendCreatePasswordMessage(String to, String subject, String username, String url);

  public void sendCommunityAddDescriptionMessage(String[] to, String subject, String userName, String communityName, String url);
}
