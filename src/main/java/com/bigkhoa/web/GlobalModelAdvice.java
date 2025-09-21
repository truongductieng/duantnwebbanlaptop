package com.bigkhoa.web;

import com.bigkhoa.model.Announcement;
import com.bigkhoa.service.AnnouncementService;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

@ControllerAdvice
public class GlobalModelAdvice {
  private final AnnouncementService svc;
  public GlobalModelAdvice(AnnouncementService svc){ this.svc = svc; }

  @ModelAttribute("activeAnn")
  public Announcement activeAnn(){ return svc.getActive().orElse(null); }
}
