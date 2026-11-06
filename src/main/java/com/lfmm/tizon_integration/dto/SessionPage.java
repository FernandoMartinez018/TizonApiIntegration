package com.lfmm.tizon_integration.dto;

import java.util.List;

public record SessionPage(List<SessionSummary> items, int page, int size, long total) {}
