/*
 * Copyright (c) 2009 - 2019 Red Hat, Inc.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 *
 * Red Hat trademarks are not licensed under GPLv3. No permission is
 * granted to use or replicate Red Hat trademarks that are incorporated
 * in this software or its documentation.
 */
package org.candlepin.insights.inventory;

import org.candlepin.insights.api.model.ConsumerInventory;
import org.candlepin.insights.api.model.OrgInventory;
import org.candlepin.insights.inventory.client.InventoryServiceProperties;
import org.candlepin.insights.inventory.client.model.CreateHostIn;
import org.candlepin.insights.inventory.client.model.FactSet;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 * Defines operations against the inventory service. This service allows batching host fact
 * updates. Once the maximum fact queue depth is reached, the service will auto flush the updates
 * so that we don't keep too many facts in memory before they are pushed to inventory.
 */
public abstract class InventoryService {

    private static final Logger log = LoggerFactory.getLogger(InventoryService.class);

    private int maxQueueDepth;
    private int staleHostOffset;
    private List<ConduitFacts> factQueue;

    public InventoryService(InventoryServiceProperties serviceProperties, int maxQueueDepth) {
        this.maxQueueDepth = maxQueueDepth;
        this.staleHostOffset = serviceProperties.getStaleHostOffsetInDays();
        this.factQueue = new LinkedList<>();
    }

    /**
     * Send host inventory updates for the specified facts.
     *
     * @param conduitFactsForOrg the host facts to send.
     */
    protected abstract void sendHostUpdate(List<ConduitFacts> conduitFactsForOrg);

    /**
     * Schedules the given host facts for update. When the max queue depth is reached,
     * fact updates are automatically flushed.
     *
     * @param facts the host facts to schedule for update.
     */
    public void scheduleHostUpdate(ConduitFacts facts) {
        synchronized (factQueue) {
            factQueue.add(facts);

            // Auto flush updates when max queue depth is reached.
            if (factQueue.size() == maxQueueDepth) {
                log.debug("Max queue depth reached. Auto flusing updates.");
                flushHostUpdates();
            }
        }
    }

    /**
     * Force the currently scheduled updates to be sent to inventory.
     */
    public void flushHostUpdates() {
        synchronized (factQueue) {
            if (!factQueue.isEmpty()) {
                sendHostUpdate(factQueue);
                factQueue.clear();
            }
        }
    }

    /**
     * Given a set of facts, report them as a host to the inventory service.
     *
     * @return the new host.
     */
    protected CreateHostIn createHost(ConduitFacts facts, OffsetDateTime syncTimestamp) {
        CreateHostIn host = new CreateHostIn();

        // fact namespace
        host.facts(Arrays.asList(new FactSet().namespace("rhsm").facts(buildFactMap(facts, syncTimestamp))));

        // required culling properties
        host.setReporter("rhsm-conduit");
        host.setStaleTimestamp(syncTimestamp.plusHours(staleHostOffset));

        // canonical facts.
        host.setAccount(facts.getAccountNumber());
        host.setFqdn(facts.getFqdn());
        host.setSubscriptionManagerId(facts.getSubscriptionManagerId());
        host.setBiosUuid(facts.getBiosUuid());
        host.setIpAddresses(facts.getIpAddresses());
        host.setMacAddresses(facts.getMacAddresses());
        host.setInsightsId(facts.getInsightsId());

        return host;
    }

    public OrgInventory getInventoryForOrgConsumers(List<ConduitFacts> conduitFactsForOrg) {
        List<ConsumerInventory> hosts = new ArrayList<>(conduitFactsForOrg);
        return new OrgInventory().consumerInventories(hosts);
    }

    private Map<String, Object> buildFactMap(ConduitFacts conduitFacts, OffsetDateTime syncTimestamp) {
        Map<String, Object> rhsmFactMap = new HashMap<>();
        rhsmFactMap.put("orgId", conduitFacts.getOrgId());
        if (conduitFacts.getCpuSockets() != null) {
            rhsmFactMap.put("CPU_SOCKETS", conduitFacts.getCpuSockets());
        }
        if (conduitFacts.getCpuCores() != null) {
            rhsmFactMap.put("CPU_CORES", conduitFacts.getCpuCores());
        }
        if (conduitFacts.getMemory() != null) {
            rhsmFactMap.put("MEMORY", conduitFacts.getMemory());
        }
        if (conduitFacts.getArchitecture() != null) {
            rhsmFactMap.put("ARCHITECTURE", conduitFacts.getArchitecture());
        }
        if (conduitFacts.getIsVirtual() != null) {
            rhsmFactMap.put("IS_VIRTUAL", conduitFacts.getIsVirtual());
        }
        if (conduitFacts.getVmHost() != null) {
            rhsmFactMap.put("VM_HOST", conduitFacts.getVmHost());
        }
        if (conduitFacts.getVmHostUuid() != null) {
            rhsmFactMap.put("VM_HOST_UUID", conduitFacts.getVmHostUuid());
        }
        if (conduitFacts.getGuestId() != null) {
            rhsmFactMap.put("GUEST_ID", conduitFacts.getGuestId());
        }
        if (conduitFacts.getRhProd() != null) {
            rhsmFactMap.put("RH_PROD", conduitFacts.getRhProd());
        }
        if (conduitFacts.getSysPurposeRole() != null && !conduitFacts.getSysPurposeRole().isEmpty()) {
            rhsmFactMap.put("SYSPURPOSE_ROLE", conduitFacts.getSysPurposeRole());
        }
        if (conduitFacts.getSysPurposeUsage() != null && !conduitFacts.getSysPurposeUsage().isEmpty()) {
            rhsmFactMap.put("SYSPURPOSE_USAGE", conduitFacts.getSysPurposeUsage());
        }
        if (conduitFacts.getSysPurposeAddons() != null) {
            rhsmFactMap.put("SYSPURPOSE_ADDONS", conduitFacts.getSysPurposeAddons());
        }

        rhsmFactMap.put("SYNC_TIMESTAMP", syncTimestamp);
        return rhsmFactMap;
    }
}
