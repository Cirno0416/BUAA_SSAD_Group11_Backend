package com.innoshare.controller;

import com.innoshare.model.doc.PaperDoc;
import com.innoshare.service.SearchService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("search")
@RequiredArgsConstructor
public class SearchController {
    private final SearchService searchService;

    @GetMapping("achievements")
    public List<PaperDoc> searchPapers(@RequestParam String query,
                                       @RequestParam(defaultValue = "") String category,
                                       @RequestParam(defaultValue = "_score") String sortBy,
                                       @RequestParam(defaultValue = "desc") String order,
                                       @RequestParam(defaultValue = "0") Integer page,
                                       @RequestParam(defaultValue = "10") Integer limit) {
        return searchService.search(query, category, sortBy, order, page, limit);
    }
}
