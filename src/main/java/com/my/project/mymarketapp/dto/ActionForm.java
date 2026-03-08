package com.my.project.mymarketapp.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ActionForm {
    private Long id;
    private String action;
    private String search = "";
    private String sort = "NO";
    private int pageSize = 10;
    private int pageNumber = 1;
}
