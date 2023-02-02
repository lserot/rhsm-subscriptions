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
package com.redhat.swatch;

import com.redhat.swatch.openapi.model.BillingProvider;
import com.redhat.swatch.openapi.model.Contract;
import com.redhat.swatch.openapi.resource.ApiException;
import com.redhat.swatch.openapi.resource.DefaultApi;
import java.util.List;
import javax.ws.rs.ProcessingException;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class TestResource implements DefaultApi {

  @Override
  public Contract createContract(Contract contract) throws ApiException, ProcessingException {
    return null;
  }

  @Override
  public void deleteContractByUUID(String uuid) throws ApiException, ProcessingException {

  }

  @Override
  public List<Contract> getContract(String uuid, String orgId, String productId, String metricId,
      BillingProvider billingProvider, String billingAccountId)
      throws ApiException, ProcessingException {
    return null;
  }

  @Override
  public Contract updateContract(String uuid, Contract contract)
      throws ApiException, ProcessingException {
    return null;
  }
}
