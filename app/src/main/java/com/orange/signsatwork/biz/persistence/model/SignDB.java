package com.orange.signsatwork.biz.persistence.model;

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

import com.fasterxml.jackson.annotation.JsonBackReference;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import javax.persistence.*;
import javax.validation.constraints.NotNull;
import java.util.Date;
import java.util.List;

// we want to save 'Sign' objects in the 'signs' DB table
@Table(name = "signs")
@Entity
// default constructor only exists for the sake of JPA
@NoArgsConstructor
@Getter
@Setter
public class SignDB {
  // An autogenerated id (unique for each video in the db)
  @Id
  @GeneratedValue(strategy = GenerationType.AUTO)
  private Long id;

  @Column(unique = true)
  @NotNull
  private String name;

  @NotNull
  private String url;

  @NotNull
  private Date createDate;

  @NotNull
  private long lastVideoId;

  private long nbVideo;

  @OneToMany(mappedBy = "sign", fetch = FetchType.LAZY)
  private List<VideoDB> videos;

  @ManyToMany(mappedBy = "signs", fetch = FetchType.LAZY)
  @JsonBackReference
  private List<FavoriteDB> favorites;

  @ManyToMany(fetch = FetchType.LAZY)
  @JoinTable(name="associate_sign",
          joinColumns={@JoinColumn(name="sign_id")},
          inverseJoinColumns={@JoinColumn(name="associate_sign_id")})
  private List<SignDB> associates;

  @ManyToMany(mappedBy="associates", fetch = FetchType.LAZY)
  private List<SignDB> referenceBy;

  public SignDB(String name, String url, Date createDate) {
    this.name = name;
    this.url = url;
    this.createDate = createDate;
  }
}
