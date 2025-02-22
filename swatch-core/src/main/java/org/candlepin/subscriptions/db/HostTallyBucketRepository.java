/*
 * Copyright Red Hat, Inc.
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
package org.candlepin.subscriptions.db;

import static org.hibernate.jpa.QueryHints.HINT_FETCH_SIZE;
import static org.hibernate.jpa.QueryHints.HINT_READONLY;

import java.util.stream.Stream;
import javax.persistence.QueryHint;
import org.candlepin.subscriptions.db.model.AccountBucketTally;
import org.candlepin.subscriptions.db.model.HostBucketKey;
import org.candlepin.subscriptions.db.model.HostTallyBucket;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.QueryHints;
import org.springframework.data.repository.CrudRepository;

public interface HostTallyBucketRepository extends CrudRepository<HostTallyBucket, HostBucketKey> {

  @Query(
      """
          select
            b.key.productId as productId, h.accountNumber as accountNumber, b.measurementType as measurementType, b.key.usage as usage,
            b.key.sla as sla, b.key.billingProvider as billingProvider, b.key.billingAccountId as billingAccountId,
            sum(b.cores) as cores, sum(b.sockets) as sockets, count(h.id) as instances
          from Host h inner join h.buckets b
            where h.id = b.host.id and h.orgId=:orgId and h.instanceType=:instanceType
          group by b.key.productId, h.accountNumber, b.measurementType, b.key.usage, b.key.sla, b.key.billingProvider, b.key.billingAccountId
  """)
  @QueryHints(
      value = {
        @QueryHint(name = HINT_FETCH_SIZE, value = "1024"),
        @QueryHint(name = HINT_READONLY, value = "true")
      })
  public Stream<AccountBucketTally> tallyHostBuckets(String orgId, String instanceType);
}
