/*
 * Copyright (c) 2019 Red Hat, Inc.
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
package org.candlepin.subscriptions.inventory.db;

import org.candlepin.subscriptions.inventory.db.model.InventoryHost;
import org.candlepin.subscriptions.inventory.db.model.InventoryHostFacts;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.UUID;
import java.util.stream.Stream;

/**
 * Interface that Spring Data will turn into a read-only DAO.
 */
@SuppressWarnings({ "linelength", "indentation" })
public interface InventoryRepository extends Repository<InventoryHost, UUID> {

    @Query(nativeQuery = true)
    Stream<InventoryHostFacts> getFacts(
        @Param("accounts") Collection<String> accounts, @Param("culledOffsetDays") Integer culledOffsetDays);

    /**
     * Get a mapping of hypervisor ID to associated hypervisor host's subscription-manager ID.
     * If the hypervisor hasn't been reported, then the hyp_subman_id value will be null.
     *
     * If a reported hypervisor is invalid (0 sockets), omit it from the results so guest hosts are treated
     * the same as a guest with an unknown hypervisor - tallying 1 socket for the guest.
     *
     * @param accounts the accounts to filter hosts by.
     * @return a stream of Object[] with each entry representing a hypervisor mapping. Each Object[]
     * represents a row of the result set
     */
    @Query(nativeQuery = true, value = "select hyp_id, hyp_subman_id " +
        "from (" +
        "           select distinct h.facts -> 'rhsm' ->> 'VM_HOST_UUID' as hyp_id," +
        "           h_.canonical_facts ->> 'subscription_manager_id' as hyp_subman_id," +
        "           jsonb_extract_path(h_.system_profile_facts, 'number_of_sockets') as sockets" +
        "           from hosts h left outer join hosts h_ on h.facts -> 'rhsm' ->> 'VM_HOST_UUID' = h_.canonical_facts ->> 'subscription_manager_id'" +
        "           where h.facts -> 'rhsm' -> 'VM_HOST_UUID' is not null and h.account IN (:accounts)" +
        "       union all" +
        "           select distinct h.facts -> 'satellite' ->> 'virtual_host_uuid'  as hyp_id," +
        "           h_.canonical_facts ->> 'subscription_manager_id' as hyp_subman_id," +
        "           jsonb_extract_path (h_.system_profile_facts, 'number_of_sockets') as sockets" +
        "           from hosts h left outer join hosts h_ on h.facts -> 'satellite' ->> 'virtual_host_uuid' = h_.canonical_facts ->> 'subscription_manager_id'" +
        "           where h.facts -> 'satellite' -> 'virtual_host_uuid' is not null and h.account IN (:accounts)" +
        "   ) as validHypTbl" +
        "   where sockets <> '0'")
    Stream<Object[]> getReportedHypervisors(@Param("accounts") Collection<String> accounts);
}
