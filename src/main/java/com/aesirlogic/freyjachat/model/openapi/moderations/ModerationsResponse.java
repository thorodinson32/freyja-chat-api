package com.aesirlogic.freyjachat.model.openapi.moderations;

import lombok.Data;

import java.util.List;

@Data
public class ModerationsResponse {
    private List<ModerationsResult> results;
}
