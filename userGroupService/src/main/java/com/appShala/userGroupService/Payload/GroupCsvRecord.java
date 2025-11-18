package com.appShala.userGroupService.Payload;

import com.opencsv.bean.CsvBindByName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class GroupCsvRecord {
    @CsvBindByName(column = "Name")
    String name;
    @CsvBindByName(column = "Role")
    String role;
    @CsvBindByName(column = "Email")
    String email;
}
