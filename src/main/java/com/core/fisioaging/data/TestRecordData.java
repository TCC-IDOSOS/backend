package com.core.fisioaging.data;

import lombok.Data;

@Data
public class TestRecordData {

    private Long time;
    private Double x;
    private Double y;
    private Double z;
    private String sensor;
}
