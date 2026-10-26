package com.toystorage.backend.stockcount;

import com.toystorage.backend.entity.inventories.*;
import com.toystorage.backend.entity.users.Users;
import com.toystorage.backend.entity.products.Products;
import com.toystorage.backend.entity.warehouses.WarehouseLocations;
import com.toystorage.backend.enums.warehouses.WarehouseStatus;
import com.toystorage.backend.repository.products.ProductRepository;
import com.toystorage.backend.repository.stockcount.StockCountAuditRepository;
import com.toystorage.backend.repository.stockcount.StockCountBalanceLockRepository;
import com.toystorage.backend.repository.stockcount.StockCountLockedRepository;
import com.toystorage.backend.repository.stockcount.StockCountNoticeRepository;
import com.toystorage.backend.repository.warehouses.putaway.WarehouseLocationRepository;
import com.toystorage.backend.enums.inventories.*;
import com.toystorage.backend.enums.warehouses.WarehouseTaskType;
import com.toystorage.backend.repository.inventories.InventoryBalanceRepository;
import com.toystorage.backend.repository.inventories.InventoryTransactionRepository;
import com.toystorage.backend.repository.inventories.stockcount.StockCountItemRepository;
import com.toystorage.backend.repository.users.UserRepository;
import com.toystorage.backend.repository.warehouses.taskclaim.WarehouseTaskClaimRepository;
import com.toystorage.backend.services.inventories.stockcount.manager.StockCountValidationService;
import com.toystorage.backend.services.warehouses.taskclaim.WarehouseTaskClaimService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;

import static com.toystorage.backend.stockcount.StockCountDTO.*;

@Service
@RequiredArgsConstructor
public class StockCountWorkflowService {
    private final StockCountLockedRepository plans;
    private final StockCountItemRepository items;
    private final StockCountBalanceLockRepository balanceLocks;
    private final InventoryBalanceRepository balances;
    private final InventoryTransactionRepository transactions;
    private final StockCountAuditRepository audits;
    private final StockCountNoticeRepository notices;
    private final UserRepository users;
    private final ProductRepository productCatalog;
    private final WarehouseLocationRepository warehouseLocations;
    private final StockCountValidationService validation;
    private final StockCountManagerAccess managerAccess;

    private final WarehouseTaskClaimService claims;
    private final WarehouseTaskClaimRepository claimRepository;

    private static final ZoneId WAREHOUSE_ZONE = ZoneId.of("Asia/Ho_Chi_Minh");

    private void ensureCountingWindow(StockCounts count) {
        LocalDate today = LocalDate.now(WAREHOUSE_ZONE);
        if (!StockCountWindowPolicy.canCount(count.getCountType(), count.getScheduledDate(), today)) {
            String window = StockCountWindowPolicy.forPlan(count.getCountType(), count.getScheduledDate())
                    .map(w -> w.start() + " to " + w.end() + " (Mon-Fri)")
                    .orElse("not defined: the scheduled date is outside both stock count workweeks");
            throw bad("Stock count is only allowed in its assigned workweek: " + window
                    + ". Today in Vietnam: " + today);
        }
    }

    private List<Products> catalogForScope(StockCounts count) {
        String scopeType = count.getScopeType() == null ? "ALL" : count.getScopeType();
        return productCatalog.findAll().stream()
                .filter(p -> !"PRODUCT".equals(scopeType) ||
                        Long.toString(p.getId()).equals(count.getScopeValue()))
                .sorted(Comparator.comparing(Products::getProductsCode))
                .toList();
    }

    private List<InventoryBalances> balancesForScope(StockCounts count) {
        return balances.findByWarehouseId(count.getWarehouse().getId()).stream()
                .filter(b -> inScope(count, b))
                .sorted(Comparator.comparing((InventoryBalances b) -> b.getProduct().getProductsCode())
                        .thenComparing(b -> b.getLocation().getWarehouseCode()))
                .toList();
    }

    private List<Products> missingCatalogItems(StockCounts count, List<InventoryBalances> current) {
        Set<Long> found = new HashSet<>();
        for (InventoryBalances balance : current) found.add(balance.getProduct().getId());
        return catalogForScope(count).stream().filter(p -> !found.contains(p.getId())).toList();
    }

    private WarehouseLocations fallbackLocation(StockCounts count, Long requestedId) {
        String scopeType = count.getScopeType() == null ? "ALL" : count.getScopeType();
        Long effective = "LOCATION".equals(scopeType) ? Long.valueOf(count.getScopeValue()) : requestedId;
        if (effective == null) throw bad("Choose a real warehouse location for products with zero recorded stock");
        WarehouseLocations location = warehouseLocations.findById(effective)
                .orElseThrow(() -> bad("Counting location does not exist"));
        if (!location.getWarehouse().getId().equals(count.getWarehouse().getId()) ||
                location.getStatus() != WarehouseStatus.ACTIVE)
            throw bad("Counting location must be active and belong to this warehouse");
        if ("ZONE".equals(scopeType) && !Objects.equals(location.getZone(), count.getScopeValue()))
            throw bad("The counting location must belong to the selected zone");
        return location;
    }

    private List<Item> plannedCatalogPreview(StockCounts count) {
        List<InventoryBalances> current = balancesForScope(count);
        List<Item> result = new ArrayList<>();
        for (InventoryBalances balance : current) {
            var p = balance.getProduct(); var loc = balance.getLocation();
            result.add(new Item(null, p.getId(), p.getProductsCode(), p.getName(), p.getBarcode(),
                    loc.getId(), loc.getWarehouseCode(), loc.getZone(), loc.getShelf(),
                    balance.getQuantity(), null, null, null, null, false, null, null, null));
        }
        WarehouseLocations scopedLocation = "LOCATION".equals(count.getScopeType()) ?
                fallbackLocation(count, null) : null;
        for (Products p : missingCatalogItems(count, current)) {
            // A catalog SKU without a balance at this warehouse is still checked as zero.
            result.add(new Item(null, p.getId(), p.getProductsCode(), p.getName(), p.getBarcode(),
                    scopedLocation == null ? null : scopedLocation.getId(),
                    scopedLocation == null ? "Location required" : scopedLocation.getWarehouseCode(),
                    scopedLocation == null ? null : scopedLocation.getZone(),
                    scopedLocation == null ? null : scopedLocation.getShelf(),
                    0, null, null, null, null, false, null, null, null));
        }
        result.sort(Comparator.comparing(Item::sku).thenComparing(i -> i.location() == null ? "" : i.location()));
        return result;
    }

    @Transactional(readOnly = true)
    public List<CountLocation> locations() {
        Long warehouseId = current().getWarehouse().getId();
        return warehouseLocations.findByWarehouseIdAndStatus(warehouseId, WarehouseStatus.ACTIVE).stream()
                .sorted(Comparator.comparing(WarehouseLocations::getWarehouseCode))
                .map(loc -> new CountLocation(loc.getId(), loc.getWarehouseCode(), loc.getName(),
                        loc.getZone(), loc.getShelf())).toList();
    }

    private ResponseStatusException bad(String message) {
        return new ResponseStatusException(HttpStatus.BAD_REQUEST, message);
    }
    private boolean managerRole() {
        // Same authoritative check used by the Controller's @PreAuthorize.
        // Never grant Manager privileges just because the UI shows "Warehouse".
        return managerAccess.canManage(
                SecurityContextHolder.getContext().getAuthentication());
    }
    private Users current() {
        Users u = validation.getCurrentUser();
        if (u.getWarehouse() == null) throw new ResponseStatusException(HttpStatus.FORBIDDEN, "No warehouse assigned");
        return u;
    }
    private StockCounts locked(Long id, Users u) {
        StockCounts c = plans.lock(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Stock count not found"));
        if (!c.getWarehouse().getId().equals(u.getWarehouse().getId()))
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Wrong warehouse");
        if (!managerRole() && (c.getAssignedTo() == null || !u.getId().equals(c.getAssignedTo().getId())))
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Not assigned to this count");
        return c;
    }
    private StockCounts found(Long id, Users u) {
        StockCounts c = plans.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Stock count not found"));
        if (!c.getWarehouse().getId().equals(u.getWarehouse().getId()))
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Wrong warehouse");
        if (!managerRole() && (c.getAssignedTo() == null || !u.getId().equals(c.getAssignedTo().getId())))
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Not assigned to this count");
        return c;
    }
    private StockCountItems getItem(StockCounts c, Long itemId) {
        StockCountItems i = items.findById(itemId).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Item not found"));
        if (!i.getStockCount().getId().equals(c.getId())) throw bad("Item does not belong to this stock count");
        return i;
    }
    private void log(StockCounts c, StockCountItems item, Users actor, String action,
                     Integer oldValue, Integer newValue, String reason) {
        StockCountAudit row = new StockCountAudit();
        row.setStockCount(c); row.setItem(item); row.setActor(actor);
        row.setAction(action); row.setOldQuantity(oldValue); row.setNewQuantity(newValue); row.setReason(reason);
        audits.save(row);
    }
    private Plan plan(StockCounts c) {
        List<StockCountItems> lines = items.findByStockCountId(c.getId());
        int counted = (int) lines.stream().filter(i -> {
            if (c.getStatus() == StockCountStatus.RECOUNTING && Boolean.TRUE.equals(i.getRecountRequested()))
                return i.getSecondCountQuantity() != null;
            return i.getFinalQuantity() != null || i.getFirstCountQuantity() != null;
        }).count();
        int changed = (int) lines.stream().filter(i -> i.getFinalQuantity() != null &&
                !i.getFinalQuantity().equals(i.getSystemQuantity())).count();
        return new Plan(c.getId(), c.getCountCode(), c.getCountType().name(), c.getScheduledDate(),
                c.getStatus().name(), c.getWarehouse().getName(),
                c.getAssignedTo() == null ? null : c.getAssignedTo().getId(),
                c.getAssignedTo() == null ? null : c.getAssignedTo().getName(),
                c.getScopeType() == null ? "ALL" : c.getScopeType(), c.getScopeValue(),
                lines.size(), counted, changed,
                c.getConfirmedBy() == null ? null : c.getConfirmedBy().getName(), c.getConfirmedAt());
    }
    @Transactional(readOnly = true)
    public List<Plan> list() {
        Users u = current();
        return plans.findByWarehouseIdOrderByIdDesc(u.getWarehouse().getId()).stream()
                .filter(c -> managerRole() || (c.getAssignedTo() != null && c.getAssignedTo().getId().equals(u.getId())))
                .map(this::plan).toList();
    }
    // A scope of ALL means every catalog SKU must be represented, even at zero stock.
    // Targeted ZONE / LOCATION / PRODUCT counts remain intentionally partial.
    private boolean isCatalogWide(StockCounts count) {
        return count.getCountType() == StockCountType.FULL
                || count.getCountType() == StockCountType.MID_MONTH
                || count.getCountType() == StockCountType.END_OF_MONTH
                || count.getScopeType() == null || "ALL".equals(count.getScopeType());
    }

    private List<String> missingCatalogSkus(StockCounts count, List<StockCountItems> lines) {
        if (!isCatalogWide(count)) return List.of();
        Set<Long> countedProducts = new HashSet<>();
        for (StockCountItems line : lines) countedProducts.add(line.getProduct().getId());
        return productCatalog.findAll().stream()
                .filter(product -> !countedProducts.contains(product.getId()))
                .map(Products::getProductsCode)
                .sorted().toList();
    }

    // Reject incomplete FULL snapshots before ANY inventory mutation.
    private void requireCompleteSnapshot(StockCounts count, List<StockCountItems> lines) {
        if (!isCatalogWide(count)) return;
        List<String> missing = missingCatalogSkus(count, lines);
        if (!missing.isEmpty()) {
            throw bad("Full stock count is missing " + missing.size() + " catalog SKU(s): " +
                    String.join(", ", missing.stream().limit(8).toList()) +
                    ". Create a new FULL stock count; an active snapshot cannot be silently rewritten.");
        }
        Set<String> countedPairs = new HashSet<>();
        for (StockCountItems item : lines)
            countedPairs.add(item.getProduct().getId() + ":" + item.getLocation().getId());
        for (InventoryBalances balance : balances.findByWarehouseId(count.getWarehouse().getId())) {
            String key = balance.getProduct().getId() + ":" + balance.getLocation().getId();
            if (!countedPairs.contains(key))
                throw bad("Full stock count is missing a warehouse product/location balance (" +
                        balance.getProduct().getProductsCode() + " / " +
                        balance.getLocation().getWarehouseCode() +
                        "). Create a fresh FULL stock count to resnapshot warehouse balances.");
        }
    }

    @Transactional(readOnly = true)
    public Detail detail(Long id) {
        StockCounts c = found(id, current());
        List<Item> rows = items.findByStockCountId(c.getId()).stream().map(i -> {
            Integer observed = Boolean.TRUE.equals(i.getRecountRequested()) && i.getSecondCountQuantity() != null
                    ? i.getSecondCountQuantity()
                    : (i.getFinalQuantity() != null ? i.getFinalQuantity() : i.getFirstCountQuantity());
            return new Item(
                i.getId(), i.getProduct().getId(), i.getProduct().getProductsCode(),
                i.getProduct().getName(), i.getProduct().getBarcode(),
                i.getLocation().getId(), i.getLocation().getWarehouseCode(),
                i.getLocation().getZone(), i.getLocation().getShelf(), i.getSystemQuantity(),
                i.getFirstCountQuantity(), i.getSecondCountQuantity(), i.getFinalQuantity(),
                observed == null ? null : observed - i.getSystemQuantity(),
                Boolean.TRUE.equals(i.getRecountRequested()),
                i.getCountedBy() == null ? null : i.getCountedBy().getName(),
                i.getRecountedBy() == null ? null : i.getRecountedBy().getName(),
                i.getCorrectionReason());
        }).toList();
        // Catalog is authoritative for FULL stock-count scope. A product with no
        // balance row in this warehouse is still represented as a zero-stock line.
        if (c.getStatus() == StockCountStatus.PLANNED && rows.isEmpty()) {
            rows = plannedCatalogPreview(c);
        }
        List<AuditEntry> history = audits.findByStockCountIdOrderByCreatedAtAscIdAsc(id).stream()
                .map(a -> new AuditEntry(a.getAction(), a.getActor().getName(),
                        a.getItem() == null ? null : a.getItem().getId(), a.getOldQuantity(),
                        a.getNewQuantity(), a.getReason(), a.getCreatedAt())).toList();
        Set<Long> covered = new HashSet<>();
        for (Item row : rows) covered.add(row.productId());
        List<StockCountItems> storedLines = items.findByStockCountId(c.getId());
        List<String> missing = c.getStatus() == StockCountStatus.PLANNED
                ? List.of() : missingCatalogSkus(c, storedLines);
        int catalogTotal = isCatalogWide(c) ? (int) productCatalog.count() : covered.size();
        return new Detail(plan(c), rows, history, catalogTotal, covered.size(), missing);
    }
    @Transactional(readOnly = true)
    public List<Staff> staff() {
        Long wh = current().getWarehouse().getId();
        return users.findActiveUsersByRoleCode("WAREHOUSE_STAFF").stream()
                .filter(u -> u.getWarehouse() != null && wh.equals(u.getWarehouse().getId()))
                .map(u -> new Staff(u.getId(), u.getName(), u.getEmail())).toList();
    }
    private Users staffInWarehouse(Long id, StockCounts c) {
        if (id == null) throw bad("Warehouse Staff is required");
        Users staff = users.findById(id).orElseThrow(() -> bad("Staff not found"));
        boolean allowed = users.findActiveUsersByRoleCode("WAREHOUSE_STAFF").stream().anyMatch(u -> u.getId().equals(id));
        if (!allowed || staff.getWarehouse() == null || !staff.getWarehouse().getId().equals(c.getWarehouse().getId()))
            throw bad("Choose an active Warehouse Staff from this warehouse");
        return staff;
    }
    private void scope(StockCounts c, String type, String value) {
        String key = type == null ? "ALL" : type.toUpperCase(Locale.ROOT);
        if (!Set.of("ALL", "ZONE", "LOCATION", "PRODUCT").contains(key)) throw bad("Invalid scope type");
        if (Set.of(StockCountType.FULL, StockCountType.MID_MONTH, StockCountType.END_OF_MONTH)
                .contains(c.getCountType()) && !"ALL".equals(key))
            throw bad("Full and periodic stock counts must include ALL catalog products");
        if (!key.equals("ALL") && (value == null || value.isBlank())) throw bad("Scope value is required");
        if (key.equals("LOCATION") || key.equals("PRODUCT")) {
            try { if (Long.parseLong(value) <= 0) throw bad("Invalid scope ID"); }
            catch (NumberFormatException e) { throw bad("Scope must be a numeric ID"); }
        }
        c.setScopeType(key); c.setScopeValue(key.equals("ALL") ? null : value.trim());
    }
    /** A FULL inventory plan for all catalog SKUs in the next eligible working-week window. */
    @Transactional
    public Plan createFullToday() {
        if (!managerRole()) throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Manager role required");
        Users manager = current();
        StockCounts count = new StockCounts();
        String code = "SC-FULL-" + UUID.randomUUID().toString().substring(0, 10).toUpperCase(Locale.ROOT);
        count.setCountCode(code); count.setStockCountsCode(code);
        count.setWarehouse(manager.getWarehouse());
        count.setCountType(StockCountType.FULL);
        count.setScheduledDate(StockCountWindowPolicy.nextEligibleDay(LocalDate.now(WAREHOUSE_ZONE)));
        count.setScopeType("ALL");
        count.setStatus(StockCountStatus.PLANNED);
        count.setCreatedBy(manager);
        plans.save(count);
        log(count, null, manager, "PLAN_CREATED", null, null, "Full catalog count for the next available stock count workweek");
        return plan(count);
    }

    @Transactional
    public Plan createAdHoc(CreateRequest req) {
        Users manager = current();
        StockCounts c = new StockCounts();
        String code = "SC-ADHOC-" + UUID.randomUUID().toString().substring(0, 10).toUpperCase(Locale.ROOT);
        c.setCountCode(code); c.setStockCountsCode(code); c.setWarehouse(manager.getWarehouse());
        c.setCountType(StockCountType.AD_HOC); c.setScheduledDate(StockCountWindowPolicy.nextEligibleDay(LocalDate.now(WAREHOUSE_ZONE)));
        c.setStatus(StockCountStatus.PLANNED); c.setCreatedBy(manager);
        scope(c, req.scopeType(), req.scopeValue());
        if (req.staffId() != null) c.setAssignedTo(staffInWarehouse(req.staffId(), c));
        plans.save(c); log(c, null, manager, "PLAN_CREATED", null, null, "Ad hoc plan");
        return plan(c);
    }
    @Transactional
    public Plan assign(Long id, ScopeRequest req) {
        Users manager = current(); StockCounts c = locked(id, manager);
        if (c.getStatus() != StockCountStatus.PLANNED) throw bad("Only PLANNED can be assigned or scoped");
        scope(c, req.scopeType(), req.scopeValue());
        c.setAssignedTo(staffInWarehouse(req.staffId(), c));
        log(c, null, manager, "ASSIGNED", null, null, "Assigned to " + c.getAssignedTo().getName());
        return plan(c);
    }
    private void assertAssigned(StockCounts c, Users staff) {
        if (c.getAssignedTo() == null || !c.getAssignedTo().getId().equals(staff.getId()))
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Not assigned to this staff member");
    }
    private boolean inScope(StockCounts c, InventoryBalances balance) {
        String key = c.getScopeType() == null ? "ALL" : c.getScopeType();
        return switch (key) {
            case "ZONE" -> Objects.equals(c.getScopeValue(), balance.getLocation().getZone());
            case "LOCATION" -> c.getScopeValue().equals(String.valueOf(balance.getLocation().getId()));
            case "PRODUCT" -> c.getScopeValue().equals(String.valueOf(balance.getProduct().getId()));
            default -> true;
        };
    }
    @Transactional
    public Plan start(Long id) {
        Users staff = current(); StockCounts c = locked(id, staff); assertAssigned(c, staff);
        if (c.getStatus() == StockCountStatus.COUNTING) return plan(c);
        if (c.getStatus() != StockCountStatus.PLANNED && c.getStatus() != StockCountStatus.RECOUNTING)
            throw bad("Only PLANNED or RECOUNTING can be started");
        ensureCountingWindow(c);
        claims.claim(WarehouseTaskType.STOCK_COUNT, id, staff);
        if (c.getStatus() == StockCountStatus.PLANNED) {
            List<InventoryBalances> currentBalances = balances.findByWarehouseId(c.getWarehouse().getId());
            int snapshots = 0;
            for (InventoryBalances balance : currentBalances) {
                if (!inScope(c, balance)) continue;
                if (items.existsByStockCountIdAndProductIdAndLocationId(id, balance.getProduct().getId(), balance.getLocation().getId())) continue;
                StockCountItems i = new StockCountItems();
                i.setStockCount(c); i.setProduct(balance.getProduct()); i.setLocation(balance.getLocation());
                i.setStockCountItemsCode("SCI-" + UUID.randomUUID().toString().substring(0, 12).toUpperCase(Locale.ROOT));
                i.setSystemQuantity(balance.getQuantity()); i.setRecountRequested(false); items.save(i); snapshots++;
            }
            if (snapshots == 0) throw bad("No inventory balance records matched the selected scope");
            c.setStartedAt(LocalDateTime.now()); c.setStatus(StockCountStatus.COUNTING);
        }
        log(c, null, staff, "COUNT_STARTED", null, null, null);
        return plan(c);
    }
    /** Manager-only direct count, separate from the Staff claim workflow.
     *  Only a PLANNED, unassigned count is eligible; Staff assignments cannot
     *  be taken over silently. Counts are snapped once on the server.
     */
    @Transactional
    public Plan startManager(Long id, StartManagerRequest request) {
        if (!managerRole()) throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Manager role required");
        Users manager = current();
        StockCounts c = locked(id, manager);
        if (c.getStatus() != StockCountStatus.PLANNED) throw bad("Only PLANNED counts can be started by Manager");
        ensureCountingWindow(c);
        // Manager may explicitly take over a PLANNED assignment. An active Staff
        // claim can never be taken over this way.
        if (claims.getActiveClaim(WarehouseTaskType.STOCK_COUNT, id) != null)
            throw new ResponseStatusException(HttpStatus.CONFLICT, "A Staff member has claimed this count");
        if (!items.findByStockCountId(id).isEmpty())
            throw bad("This PLANNED count already has stored items; please review before starting");
        List<InventoryBalances> currentBalances = balancesForScope(c);
        List<Products> missing = missingCatalogItems(c, currentBalances);
        WarehouseLocations fallback = missing.isEmpty() ? null :
                fallbackLocation(c, request == null ? null : request.fallbackLocationId());
        int snapshots = 0;
        for (InventoryBalances balance : currentBalances) {
            if (items.existsByStockCountIdAndProductIdAndLocationId(id,
                    balance.getProduct().getId(), balance.getLocation().getId())) continue;
            StockCountItems item = new StockCountItems();
            item.setStockCount(c); item.setProduct(balance.getProduct()); item.setLocation(balance.getLocation());
            item.setStockCountItemsCode("SCI-" + UUID.randomUUID().toString().substring(0, 12).toUpperCase(Locale.ROOT));
            item.setSystemQuantity(balance.getQuantity()); item.setRecountRequested(false);
            items.save(item); snapshots++;
        }
        for (Products product : missing) {
            // The Manager explicitly chose the counting location for this zero-stock SKU.
            // Creating a count item does NOT create or modify an inventory balance.
            StockCountItems item = new StockCountItems();
            item.setStockCount(c); item.setProduct(product); item.setLocation(fallback);
            item.setStockCountItemsCode("SCI-" + UUID.randomUUID().toString().substring(0, 12).toUpperCase(Locale.ROOT));
            item.setSystemQuantity(0); item.setRecountRequested(false);
            items.save(item); snapshots++;
        }
        if (snapshots == 0) throw bad("No catalog products matched this scope");
        if (c.getAssignedTo() != null && !c.getAssignedTo().getId().equals(manager.getId()))
            log(c, null, manager, "MANAGER_TAKEOVER", null, null,
                    "Replaced prior assignment: " + c.getAssignedTo().getName());
        c.setAssignedTo(manager);
        c.setStartedAt(LocalDateTime.now());
        c.setStatus(StockCountStatus.COUNTING);
        log(c, null, manager, "MANAGER_COUNT_STARTED", null, null, null);
        return plan(c);
    }

    /** All-or-nothing: no inventory is written until every count is supplied.
     *  confirm() verifies stale snapshots and reservations with row locks;
     *  any failure rolls back counted quantities, transaction rows and audits.
     */
    @Transactional
    public Plan confirmManager(Long id, ManagerCountRequest request) {
        if (!managerRole()) throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Manager role required");
        Users manager = current();
        StockCounts c = locked(id, manager);
        if (c.getStatus() != StockCountStatus.COUNTING || c.getAssignedTo() == null ||
                !c.getAssignedTo().getId().equals(manager.getId()))
            throw bad("This count is not in Manager direct-count mode");
        ensureCountingWindow(c); // Confirm Manager performs new physical counts and must stay in window.
        if (claims.getActiveClaim(WarehouseTaskType.STOCK_COUNT, id) != null)
            throw new ResponseStatusException(HttpStatus.CONFLICT, "A Staff claim is still active");
        List<StockCountItems> lines = items.findByStockCountId(id);
        requireCompleteSnapshot(c, lines);
        if (lines.isEmpty() || request == null || request.items() == null ||
                request.items().size() != lines.size())
            throw bad("Every product and location needs a counted quantity");
        Map<Long, Integer> quantities = new HashMap<>();
        for (ManagerCountLine entry : request.items()) {
            if (entry == null || entry.itemId() == null || entry.quantity() == null)
                throw bad("Quantity must be a non-negative integer for every item");
            int quantity;
            try { quantity = entry.quantity().intValueExact(); }
            catch (ArithmeticException ex) { throw bad("Quantity must be a whole number within integer range"); }
            if (quantity < 0) throw bad("Counted quantity cannot be negative");
            if (quantities.putIfAbsent(entry.itemId(), quantity) != null)
                throw bad("Duplicate stock count item");
        }
        for (StockCountItems line : lines) {
            Integer value = quantities.get(line.getId());
            if (value == null) throw bad("A product/location is missing from the count");
            Integer before = line.getFirstCountQuantity();
            line.setFirstCountQuantity(value);
            line.setFinalQuantity(value);
            line.setCountedBy(manager);
            line.calculateDifference();
            log(c, line, manager, "MANAGER_COUNT_ENTERED", before, value, null);
        }
        c.setStatus(StockCountStatus.PENDING_CONFIRMATION);
        log(c, null, manager, "MANAGER_COUNT_SUBMITTED", null, null, null);
        // Same DB transaction. No partial stock updates on an exception.
        return confirm(id);
    }

    @Transactional
    public Detail count(Long id, Long itemId, CountRequest req) {
        Users staff = current(); StockCounts c = locked(id, staff); assertAssigned(c, staff);
        if (c.getStatus() != StockCountStatus.COUNTING && c.getStatus() != StockCountStatus.RECOUNTING)
            throw bad("Count is not open");
        ensureCountingWindow(c); // Do not accept physical counts before/after the allowed workweek.
        claims.validateOwner(WarehouseTaskType.STOCK_COUNT, id, staff);
        if (req.quantity() == null || req.quantity() < 0) throw bad("Quantity must be zero or greater");
        StockCountItems i = getItem(c, itemId);
        if (req.barcode() != null && !req.barcode().isBlank() && !i.getProduct().getBarcode().equals(req.barcode().trim()))
            throw bad("Scanned product barcode does not match");
        if (req.locationCode() != null && !req.locationCode().isBlank() && !i.getLocation().getWarehouseCode().equalsIgnoreCase(req.locationCode().trim()))
            throw bad("Scanned location code does not match");
        Integer previous;
        if (c.getStatus() == StockCountStatus.RECOUNTING) {
            if (!Boolean.TRUE.equals(i.getRecountRequested())) throw bad("Item not selected for recount");
            previous = i.getSecondCountQuantity();
            i.setSecondCountQuantity(req.quantity()); i.setRecountedBy(staff);
        } else {
            previous = i.getFirstCountQuantity();
            i.setFirstCountQuantity(req.quantity()); i.setCountedBy(staff);
        }
        items.save(i); log(c, i, staff, "COUNT_ENTERED", previous, req.quantity(), null);
        return detail(id);
    }
    @Transactional
    public Plan submit(Long id) {
        Users staff = current(); StockCounts c = locked(id, staff); assertAssigned(c, staff);
        claims.validateOwner(WarehouseTaskType.STOCK_COUNT, id, staff);
        List<StockCountItems> lines = items.findByStockCountId(id);
        if (lines.isEmpty()) throw bad("Stock count has no items");
        boolean recount = c.getStatus() == StockCountStatus.RECOUNTING;
        if (!recount && c.getStatus() != StockCountStatus.COUNTING) throw bad("Not counting");
        for (StockCountItems i : lines) {
            if (!recount && i.getFirstCountQuantity() == null) throw bad("All first counts are required");
            if (recount && Boolean.TRUE.equals(i.getRecountRequested()) && i.getSecondCountQuantity() == null)
                throw bad("Complete every requested recount");
            i.setFinalQuantity(recount && Boolean.TRUE.equals(i.getRecountRequested())
                    ? i.getSecondCountQuantity() : i.getFirstCountQuantity());
            i.calculateDifference();
            i.setRecountRequested(false);
        }
        c.setStatus(StockCountStatus.PENDING_CONFIRMATION);
        claims.release(WarehouseTaskType.STOCK_COUNT, id, staff);
        log(c, null, staff, "SUBMITTED", null, null, null);
        return plan(c);
    }
    @Transactional
    public Plan requestRecount(Long id, RecountRequest req) {
        if (req.reason() == null || req.reason().trim().length() < 3)
            throw bad("Reason for recount is required");
        Users manager = current(); StockCounts c = locked(id, manager);
        if (c.getStatus() != StockCountStatus.PENDING_CONFIRMATION) throw bad("Only pending counts can be recounted");
        Users staff = staffInWarehouse(req.staffId(), c);
        List<StockCountItems> lines = items.findByStockCountId(id);
        Set<Long> selected = req.itemIds() == null || req.itemIds().isEmpty() ?
                new HashSet<>(lines.stream().filter(i -> !Objects.equals(i.getFinalQuantity(), i.getSystemQuantity())).map(StockCountItems::getId).toList()) :
                new HashSet<>(req.itemIds());
        if (selected.isEmpty() || lines.stream().noneMatch(i -> selected.contains(i.getId()))) throw bad("Select items to recount");
        if (lines.stream().filter(i -> selected.contains(i.getId())).count() != selected.size()) throw bad("Unknown recount item");
        for (StockCountItems i : lines) if (selected.contains(i.getId())) {
            i.setRecountRequested(true); i.setSecondCountQuantity(null);
        }
        c.setAssignedTo(staff); c.setStatus(StockCountStatus.RECOUNTING);
        log(c, null, manager, "RECOUNT_REQUESTED", null, null, req.reason());
        return plan(c);
    }
    @Transactional
    public Detail correct(Long id, Long itemId, CorrectionRequest req) {
        Users manager = current(); StockCounts c = locked(id, manager);
        if (c.getStatus() != StockCountStatus.PENDING_CONFIRMATION) throw bad("Only pending counts can be corrected");
        ensureCountingWindow(c);
        if (req.quantity() == null || req.quantity() < 0) throw bad("Quantity must be zero or greater");
        if (req.reason() == null || req.reason().trim().length() < 3) throw bad("Correction reason is required");
        StockCountItems i = getItem(c, itemId);
        Integer previous = i.getFinalQuantity();
        if (previous == null) throw bad("No final count to correct");
        i.setFinalQuantity(req.quantity()); i.setCorrectedQuantity(req.quantity());
        i.setCorrectedBy(manager); i.setCorrectedAt(LocalDateTime.now());
        i.setCorrectionReason(req.reason().trim()); i.calculateDifference();
        log(c, i, manager, "MANAGER_CORRECTION", previous, req.quantity(), req.reason().trim());
        return detail(id);
    }
    /**
     * Manager approves every row, not just discrepancies/corrections.
     * The submitted list must contain every ID exactly once, with the same final
     * quantities the Manager saw. This is checked inside the transaction.
     */
    @Transactional
    public Plan confirmAll(Long id, ConfirmAllRequest request) {
        if (!managerRole()) throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Manager role required");
        Users manager = current();
        StockCounts count = locked(id, manager);
        if (count.getStatus() != StockCountStatus.PENDING_CONFIRMATION)
            throw bad("Only counts awaiting confirmation can be approved");
        List<StockCountItems> lines = items.findByStockCountId(id);
        requireCompleteSnapshot(count, lines);
        if (lines.isEmpty() || request == null || request.items() == null ||
                request.items().size() != lines.size())
            throw bad("Manager must review every product/location row before Confirm All");
        Map<Long, Integer> approved = new HashMap<>();
        for (ConfirmAllLine line : request.items()) {
            if (line == null || line.itemId() == null || line.finalQuantity() == null ||
                    line.finalQuantity() < 0 || approved.putIfAbsent(line.itemId(), line.finalQuantity()) != null)
                throw bad("Invalid, negative or duplicated reviewed item");
        }
        for (StockCountItems line : lines) {
            Integer expectedFinal = approved.get(line.getId());
            if (expectedFinal == null || !expectedFinal.equals(line.getFinalQuantity()))
                throw new ResponseStatusException(HttpStatus.CONFLICT,
                        "A final quantity changed after review; reload and review every line again");
        }
        log(count, null, manager, "ALL_ROWS_REVIEWED", null, lines.size(), null);
        return confirm(id);
    }

    @Transactional
    public Plan confirm(Long id) {
        Users manager = current(); StockCounts c = locked(id, manager);
        if (c.getStatus() == StockCountStatus.COMPLETED) return plan(c); // idempotent
        if (c.getStatus() != StockCountStatus.PENDING_CONFIRMATION) throw bad("Not ready for confirmation");
        List<StockCountItems> lines = new ArrayList<>(items.findByStockCountId(id));
        requireCompleteSnapshot(c, lines);
        lines.sort(Comparator.comparing((StockCountItems i) -> i.getLocation().getId())
                .thenComparing(i -> i.getProduct().getId()));
        if (lines.isEmpty() || lines.stream().anyMatch(i -> i.getFinalQuantity() == null || i.getFinalQuantity() < 0))
            throw bad("Every item needs a final non-negative quantity");
        // Lock all balances first and reject stale snapshots; a partial update rolls back entirely.
        List<InventoryBalances> lockedBalances = new ArrayList<>();
        for (StockCountItems i : lines) {
            // A zero-stock catalog item is allowed to have no inventory balance.
            InventoryBalances b = balanceLocks.lockBalance(c.getWarehouse().getId(), i.getLocation().getId(),
                    i.getProduct().getId()).orElse(null);
            if (b == null && i.getSystemQuantity() != 0)
                throw new ResponseStatusException(HttpStatus.CONFLICT, "Existing stock balance was removed; recount required");
            if (b != null && !Objects.equals(b.getQuantity(), i.getSystemQuantity()))
                throw new ResponseStatusException(HttpStatus.CONFLICT, "Inventory changed since the snapshot for " + i.getProduct().getProductsCode() + "; cancel and create a fresh stock count");
            if (b != null && i.getFinalQuantity() < b.getReservedQuantity())
                throw new ResponseStatusException(HttpStatus.CONFLICT, "Final quantity is below reserved stock for " + i.getProduct().getProductsCode());
            lockedBalances.add(b);
        }
        for (int index = 0; index < lines.size(); index++) {
            StockCountItems i = lines.get(index); InventoryBalances b = lockedBalances.get(index);
            int before = b == null ? 0 : b.getQuantity(); int after = i.getFinalQuantity();
            if (before == after) continue; // No empty inventory row created for zero -> zero.
            if (b == null) {
                b = new InventoryBalances();
                b.setInventoryBalancesCode("IB-SC-" + UUID.randomUUID().toString().substring(0, 12).toUpperCase(Locale.ROOT));
                b.setWarehouse(c.getWarehouse()); b.setProduct(i.getProduct()); b.setLocation(i.getLocation());
                b.setQuantity(after); b.setReservedQuantity(0);
                // @PrePersist sets the available quantity, including quarantine rules.
            } else {
                b.setQuantity(after); // @PreUpdate also recalculates available quantity.
            }
            balances.saveAndFlush(b);
            InventoryTransactions t = new InventoryTransactions();
            t.setInventoryTransactionsCode("TX-SC-" + UUID.randomUUID().toString().substring(0, 12).toUpperCase(Locale.ROOT));
            t.setWarehouse(c.getWarehouse()); t.setLocation(i.getLocation()); t.setProduct(i.getProduct());
            t.setTransactionType(after > before ? InventoryTransactionType.ADJUSTMENT_INCREASE : InventoryTransactionType.ADJUSTMENT_DECREASE);
            t.setQuantityChange(after - before); t.setQuantityBefore(before); t.setQuantityAfter(after);
            t.setReferenceId(c.getId()); t.setReferenceType(InventoryReferenceType.STOCK_COUNT);
            t.setPerformedBy(manager); transactions.save(t);
            log(c, i, manager, "STOCK_UPDATED", before, after, null);
        }
        c.setStatus(StockCountStatus.COMPLETED); c.setConfirmedBy(manager);
        c.setConfirmedAt(LocalDateTime.now()); c.setCompletedAt(LocalDateTime.now());
        log(c, null, manager, "CONFIRMED", null, null, null);
        return plan(c);
    }
    @Transactional
    public Plan cancel(Long id, String reason) {
        Users manager = current(); StockCounts c = locked(id, manager);
        if (c.getStatus() == StockCountStatus.COMPLETED || c.getStatus() == StockCountStatus.CANCELLED)
            throw bad("Completed or cancelled plans cannot be cancelled again");
        var active = claims.getActiveClaim(WarehouseTaskType.STOCK_COUNT, id);
        if (active != null) {
            active.setReleasedAt(LocalDateTime.now());
            claimRepository.save(active);
        }
        c.setStatus(StockCountStatus.CANCELLED);
        log(c, null, manager, "CANCELLED", null, null, reason);
        return plan(c);
    }
    @Transactional(readOnly = true)
    public List<Notice> notifications() {
        Users manager = current();
        return notices.findTop30ByUserIdOrderByCreatedAtDesc(manager.getId()).stream()
                .filter(n -> n.getReadAt() == null)
                .map(n -> new Notice(n.getId(), n.getStockCount().getId(), n.getStockCount().getCountCode(),
                        n.getStockCount().getScheduledDate(), n.getStockCount().getCountType().name(), n.getCreatedAt())).toList();
    }
    @Transactional
    public void readNotice(Long id) {
        Users manager = current();
        StockCountNotice n = notices.findByIdAndUserId(id, manager.getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Notification not found"));
        n.setReadAt(LocalDateTime.now());
    }
}
