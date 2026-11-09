package com.toystorage.backend.services.inventories.stockcount.manager;

import com.toystorage.backend.entity.users.Users;
import com.toystorage.backend.entity.inventories.StockCountItems;
import com.toystorage.backend.entity.inventories.StockCounts;
import com.toystorage.backend.exceptions.BadRequest;
import com.toystorage.backend.exceptions.Forbidden;
import com.toystorage.backend.exceptions.NotFound;
import com.toystorage.backend.exceptions.Unauthorized;
import com.toystorage.backend.repository.users.UserRepository;
import com.toystorage.backend.repository.inventories.stockcount.StockCountItemRepository;
import com.toystorage.backend.repository.inventories.stockcount.StockCountRepository;

import lombok.RequiredArgsConstructor;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class StockCountValidationService {

    private final UserRepository userRepository;

    private final StockCountRepository
            stockCountRepository;

    private final StockCountItemRepository
            stockCountItemRepository;


    public Users getCurrentUser() {

        Authentication authentication =
                SecurityContextHolder
                        .getContext()
                        .getAuthentication();

        if (authentication == null
                || !authentication.isAuthenticated()
                || "anonymousUser".equals(
                authentication.getPrincipal()
        )) {

            throw new Unauthorized(
                    "User is not authenticated"
            );
        }

        return userRepository
                .findByEmail(authentication.getName())

                .orElseThrow(() ->
                        new NotFound(
                                "Authenticated user not found"
                        )
                );
    }


    public StockCounts getStockCount(
            Long stockCountId
    ) {

        return stockCountRepository
                .findById(stockCountId)

                .orElseThrow(() ->
                        new NotFound(
                                "Stock count not found: "
                                        + stockCountId
                        )
                );
    }


    public StockCountItems getItem(
            Long stockCountId,
            Long itemId
    ) {

        StockCountItems item =
                stockCountItemRepository
                        .findById(itemId)

                        .orElseThrow(() ->
                                new NotFound(
                                        "Stock count item not found"
                                )
                        );

        if (!item.getStockCount()
                .getId()
                .equals(stockCountId)) {

            throw new BadRequest(
                    "Item does not belong to stock count"
            );
        }

        return item;
    }


    public void validateWarehouse(
            Users user,
            StockCounts stockCount
    ) {

        if (user.getWarehouse() == null) {

            throw new Forbidden(
                    "User is not assigned to warehouse"
            );
        }

        if (!user.getWarehouse()
                .getId()
                .equals(
                        stockCount
                                .getWarehouse()
                                .getId()
                )) {

            throw new Forbidden(
                    "Stock count belongs to another warehouse"
            );
        }
    }
}
