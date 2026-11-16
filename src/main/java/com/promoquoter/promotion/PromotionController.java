package com.promoquoter.promotion;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class PromotionController {

    private final PromotionCommandService commands;

    public PromotionController(PromotionCommandService commands) {
        this.commands = commands;
    }

    @PostMapping("/promotions")
    @ResponseStatus(HttpStatus.CREATED)
    public List<PromotionResponse> create(
            @RequestBody @NotEmpty List<@Valid @NotNull CreatePromotionRequest> promotions) {
        return commands.create(promotions);
    }
}
