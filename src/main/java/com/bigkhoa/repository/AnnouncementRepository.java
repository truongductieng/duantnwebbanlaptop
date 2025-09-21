package com.bigkhoa.repository;

import com.bigkhoa.model.Announcement;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface AnnouncementRepository extends JpaRepository<Announcement, Long> {

  Optional<Announcement> findFirstByEnabledTrueOrderByUpdatedAtDesc();

  @Modifying
  @Query("update Announcement a set a.enabled=false where a.enabled=true and a.id<>:id")
  int disableOthers(@Param("id") Long id);

  @Modifying
  @Query("update Announcement a set a.enabled=false where a.enabled=true")
  int disableAll();
}
