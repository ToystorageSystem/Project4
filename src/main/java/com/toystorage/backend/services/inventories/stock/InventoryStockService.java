package com.toystorage.backend.services.inventories.stock;

import com.toystorage.backend.dto.response.inventories.stock.InventoryStockResponse;
import com.toystorage.backend.entity.inventories.InventoryBalances;
import com.toystorage.backend.entity.products.Products;
import com.toystorage.backend.entity.users.Users;
import com.toystorage.backend.entity.warehouses.WarehouseLocations;
import com.toystorage.backend.repository.inventories.InventoryBalanceRepository;
import com.toystorage.backend.repository.users.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class InventoryStockService {

    private final InventoryBalanceRepository balanceRepository;
    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public List<InventoryStockResponse> listCurrentWarehouseStock() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || "anonymousUser".equals(auth.getPrincipal())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Login required");
        }
        Users user = userRepository.findByEmail(auth.getName())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User not found"));
        if (user.getWarehouse() == null) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "No warehouse assigned");
        }

        // Reuses the repository and entity already present in the backend branch.
        // Keep collection and lazy relations inside this read-only transaction.
        Map<Long, List<InventoryBalances>> byProduct = balanceRepository
                .findByWarehouseId(user.getWarehouse().getId())
                .stream().collect(Collectors.groupingBy(b -> b.getProduct().getId()));

        return byProduct.values().stream().map(this::toProductStock)
                .sorted(Comparator.comparing(r -> r.productName().toLowerCase(Locale.ROOT)))
                .toList();
    }

    private InventoryStockResponse toProductStock(List<InventoryBalances> balances) {
        Products product = balances.get(0).getProduct();
        List<InventoryStockResponse.LocationStock> locations = balances.stream()
                .map(this::toLocationStock)
                .sorted(Comparator.comparing(InventoryStockResponse.LocationStock::locationCode))
                .toList();
        return new InventoryStockResponse(
                product.getId(), product.getProductsCode(), product.getName(),
                product.getBarcode(), product.getBaseUnit(),
                locations.stream().mapToLong(InventoryStockResponse.LocationStock::onHand).sum(),
                locations.stream().mapToLong(InventoryStockResponse.LocationStock::reserved).sum(),
                locations.stream().mapToLong(InventoryStockResponse.LocationStock::available).sum(),
                locations
        );
    }

    private InventoryStockResponse.LocationStock toLocationStock(InventoryBalances balance) {
        WarehouseLocations location = balance.getLocation();
        return new InventoryStockResponse.LocationStock(
                location.getId(), location.getWarehouseCode(), location.getName(),
                location.getZone(), location.getShelf(), location.getLocationType().name(),
                safe(balance.getQuantity()), safe(balance.getReservedQuantity()),
                safe(balance.getAvailableQuantity())
        );
    }

    private long safe(Integer number) { return number == null ? 0L : number.longValue(); }
}
