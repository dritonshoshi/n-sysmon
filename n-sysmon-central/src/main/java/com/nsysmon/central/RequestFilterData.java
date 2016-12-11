package com.nsysmon.central;

import java.time.LocalDateTime;

/**
 * Created by torsten on 11.12.2016.
 */
public class RequestFilterData {
    private String servername;
    private LocalDateTime start;
    private LocalDateTime end;

    public String getServername() {
        return servername;
    }

    public void setServername(String servername) {
        this.servername = servername;
    }

    public LocalDateTime getStart() {
        return start;
    }

    public void setStart(LocalDateTime start) {
        this.start = start;
    }

    public LocalDateTime getEnd() {
        return end;
    }

    public void setEnd(LocalDateTime end) {
        this.end = end;
    }
}
