package com.leegality.api.support;

import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.Map;

public final class BookingData {
    private BookingData() {}
    public static Map<String,Object> valid() {
        Map<String,Object> dates = new LinkedHashMap<>(); dates.put("checkin", LocalDate.now().plusDays(30).toString()); dates.put("checkout", LocalDate.now().plusDays(33).toString());
        Map<String,Object> b = new LinkedHashMap<>(); b.put("firstname", "QA" + System.nanoTime()); b.put("lastname", "Automation"); b.put("totalprice", 275); b.put("depositpaid", true); b.put("bookingdates", dates); b.put("additionalneeds", "Breakfast"); return b;
    }
}
