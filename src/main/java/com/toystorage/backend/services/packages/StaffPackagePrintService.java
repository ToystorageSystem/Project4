package com.toystorage.backend.services.packages;

import com.toystorage.backend.entity.packages.Packages;
import com.toystorage.backend.entity.transfers.StockTransfer;

import lombok.extern.slf4j.Slf4j;

import org.springframework.stereotype.Service;

@Service
@Slf4j
public class StaffPackagePrintService {

    public void printPackageLabel(
            Packages packageEntity,
            StockTransfer transfer
    ) {

        log.info(
                "PRINT PACKAGE LABEL package={}, transfer={}, destination={}",
                packageEntity.getPackagesCode(),
                transfer.getTransferCode(),
                transfer.getToWarehouse().getName()
        );

        /*
         * Sau này nối Print Agent tại đây.
         */
    }
}