package com.toystorage.backend.dto.response.warehouses;

import jakarta.validation.constraints.*;
import java.time.*;
import java.util.List;

public final class WarehouseOperationsDTO {
  private WarehouseOperationsDTO() {}

  public record NewTask(
      @NotNull Long staffId,
      @NotBlank @Size(max = 200) String title,
      @NotBlank String kind,
      @Size(max = 1000) String instructions,
      @NotNull LocalDate workDate) {}

  public record TaskAction(
      @NotBlank String action, @Size(max = 1000) String note, @Min(1) @Max(5) Integer score) {}

  public record ReportRequest(@Size(max = 1000) String note) {}

  public record Staff(Long id, String name) {}

  public record Task(
      Long id,
      Long staffId,
      String staff,
      String title,
      String kind,
      String instructions,
      LocalDate workDate,
      String status,
      LocalDateTime startedAt,
      LocalDateTime submittedAt,
      LocalDateTime completedAt,
      Integer score,
      String completionNote,
      String reviewNote) {}

  public record Performance(
      Long staffId,
      String staff,
      long assigned,
      long completed,
      long overdue,
      long onTime,
      Double averageScore) {}

  public record StockLevel(Long id, String sku, String product, String location, Integer minimum) {}

  public record MinimumRequest(@NotNull @Min(0) Integer minimum) {}

  public record LowStock(
      Long productId, String sku, String name, long available, long minimum, long suggested) {}

  public record Report(
      Long id,
      String warehouse,
      String submittedBy,
      LocalDateTime submittedAt,
      String note,
      List<LowStock> items,
      String acknowledgedBy,
      LocalDateTime acknowledgedAt) {}
}
