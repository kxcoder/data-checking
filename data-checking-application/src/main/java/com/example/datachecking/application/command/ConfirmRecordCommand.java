package com.example.datachecking.application.command;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class ConfirmRecordCommand {
    @NotNull Long id;
    @NotNull Integer confirmType;
    String confirmUser;
    String remark;
}
