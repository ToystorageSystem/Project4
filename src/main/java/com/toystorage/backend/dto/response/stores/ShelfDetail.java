package com.toystorage.backend.dto.response.stores;

import java.util.List;

public record ShelfDetail(Long id, String code, String name, String description,
                          List<ShelfProductLine> products) {
}