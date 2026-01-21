package com.evlo.controller;

import com.evlo.entity.Event;
import com.evlo.entity.enums.EventLevel;
import com.evlo.entity.enums.LogChannel;
import com.evlo.repository.EventRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

@Controller
@RequiredArgsConstructor
public class EventController {

    private final EventRepository eventRepository;

    /**
     * 홈 페이지
     */
    @GetMapping("/")
    public String index() {
        return "index";
    }

    /**
     * 로그 업로드 페이지
     */
    @GetMapping("/upload")
    public String uploadPage() {
        return "upload";
    }

    /**
     * 이벤트 리스트 페이지
     */
    @GetMapping("/events")
    public String eventsPage(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "timeCreated") String sortBy,
            @RequestParam(defaultValue = "DESC") String sortDir,
            Model model) {

        Sort sort = sortDir.equalsIgnoreCase("ASC") 
                ? Sort.by(sortBy).ascending() 
                : Sort.by(sortBy).descending();
        
        Pageable pageable = PageRequest.of(page, size, sort);
        Page<Event> eventPage = eventRepository.findAll(pageable);

        model.addAttribute("events", eventPage.getContent());
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", eventPage.getTotalPages());
        model.addAttribute("totalElements", eventPage.getTotalElements());
        model.addAttribute("pageSize", size);
        model.addAttribute("sortBy", sortBy);
        model.addAttribute("sortDir", sortDir);

        return "events";
    }
}
