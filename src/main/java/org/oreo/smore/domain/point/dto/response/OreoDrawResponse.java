package org.oreo.smore.domain.point.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class OreoDrawResponse {
    private String result;

    private String updatedLevel;

    private long updatedPoints;
}