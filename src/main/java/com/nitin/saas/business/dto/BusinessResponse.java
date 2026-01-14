package com.nitin.saas.business.dto;

import com.nitin.saas.business.entity.Business;

public class BusinessResponse {

    private Long id;
    private String name;
    private String slug;

    public static BusinessResponse from(Business business) {
        BusinessResponse r = new BusinessResponse();
        r.id = business.getId();
        r.name = business.getName();
        r.slug = business.getSlug();
        return r;
    }
}
