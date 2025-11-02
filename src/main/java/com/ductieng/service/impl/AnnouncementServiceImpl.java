package com.ductieng.service.impl;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ductieng.model.Announcement;
import com.ductieng.repository.AnnouncementRepository;
import com.ductieng.service.AnnouncementService;

import java.util.List;
import java.util.Optional;

@Service
public class AnnouncementServiceImpl implements AnnouncementService {

  private final AnnouncementRepository repo;

  public AnnouncementServiceImpl(AnnouncementRepository repo) {
    this.repo = repo;
  }

  @Override
  public Optional<Announcement> getActive() {
    return repo.findFirstByEnabledTrueOrderByUpdatedAtDesc();
  }

  @Override
  public List<Announcement> findAll() {
    return repo.findAll();
  }

  @Override
  @Transactional
  public Announcement save(Announcement a) {
    // Nếu đang bật, tắt các banner khác trước khi lưu
    if (a.isEnabled()) {
      if (a.getId() == null) {
        repo.disableAll();            // tạo mới kích hoạt
      } else {
        repo.disableOthers(a.getId()); // cập nhật kích hoạt
      }
    }
    return repo.save(a);
  }

  @Override
  public void delete(Long id) {
    repo.deleteById(id);
  }

  @Override
  public Optional<Announcement> findById(Long id) {
    return repo.findById(id);
  }
}
