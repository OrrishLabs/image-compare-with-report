package com.orrish.automation.model;

import com.opencsv.bean.CsvBindByName;

public class CompareSpec {

    @CsvBindByName(column = "FILE_NAME")
    public String fileName;

    @CsvBindByName(column = "IGNORE_AREA")
    public String ignoreArea;

    @CsvBindByName(column = "COMPARE")
    public boolean compare;
}
