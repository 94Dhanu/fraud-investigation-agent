package com.fraudagent.api.dto;

import com.fraudagent.domain.model.RecommendedAction;
import com.fraudagent.persistence.entity.ReviewDecisionEntity;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record ReviewDecisionRequest(

        @NotBlank(message = "reviewerId is required")
        @Size(max = 128)
        String reviewerId,

        @NotNull(message = "decision is required")
        ReviewDecisionEntity.ReviewDecision decision,

        /** Required when decision = OVERRIDE_RECOMMENDATION. */
        RecommendedAction finalAction,

        @Size(max = 2000)
        String justification
) {
}
