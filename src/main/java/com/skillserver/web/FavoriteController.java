package com.skillserver.web;

import com.skillserver.security.SecurityUtils;
import com.skillserver.service.FavoriteService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/skills/{skillUid}/favorite")
@RequiredArgsConstructor
public class FavoriteController {

    private final FavoriteService favoriteService;

    @PostMapping
    public void favorite(@PathVariable String skillUid) {
        favoriteService.favorite(skillUid, SecurityUtils.currentUser());
    }

    @DeleteMapping
    public void unfavorite(@PathVariable String skillUid) {
        favoriteService.unfavorite(skillUid, SecurityUtils.currentUser());
    }
}
