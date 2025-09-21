package com.bigkhoa.service;

import com.bigkhoa.model.Announcement;

import java.util.List;
import java.util.Optional;

public interface AnnouncementService {

  List<Announcement> findAll();

  Optional<Announcement> findById(Long id);

  Optional<Announcement> getActive();

  Announcement save(Announcement a);

  void delete(Long id);
}
