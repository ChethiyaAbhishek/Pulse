package com.ecads.pulse.android.dto;

import android.net.Uri;

import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ReportDto {
    private String imageUri;
    private String label;
    private String uploadedDateTime;
}
