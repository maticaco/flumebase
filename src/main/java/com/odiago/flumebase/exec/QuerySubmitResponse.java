/**
 * Licensed to Odiago, Inc. under one or more contributor license
 * agreements.  See the NOTICE.txt file distributed with this work for
 * additional information regarding copyright ownership.  Odiago, Inc.
 * licenses this file to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance with the
 * License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the
 * License for the specific language governing permissions and limitations
 * under the License.
 */

package com.odiago.flumebase.exec;

import com.odiago.flumebase.thrift.TFlowId;
import com.odiago.flumebase.thrift.TQuerySubmitResponse;

/**
 * After the user submits a query to the planner, this response
 * is provided with information for the client to make use of.
 */
public class QuerySubmitResponse {
  private final String mMsg;
  private final FlowId mFlowId;

  public QuerySubmitResponse(String msg, FlowId flowId) {
    mMsg = msg;
    mFlowId = flowId;
  }

  /**
   * @return any message which the user should see regarding the query.
   */
  public String getMessage() {
    return mMsg;
  }

  /**
   * @return The FlowId, if any, which was generated by a successfully-submitted
   * flow based on this query.
   */
  public FlowId getFlowId() {
    return mFlowId;
  }

  public TQuerySubmitResponse toThrift() {
    TFlowId tId = mFlowId == null ? null : mFlowId.toThrift();
    TQuerySubmitResponse qsr = new TQuerySubmitResponse();
    qsr.setMsg(mMsg);
    qsr.setFlowId(tId);
    return qsr;
  }

  public static QuerySubmitResponse fromThrift(TQuerySubmitResponse other) {
    String msg = other.getMsg();
    TFlowId tId = other.getFlowId();
    FlowId id = tId == null ? null : FlowId.fromThrift(tId);
    return new QuerySubmitResponse(msg, id);
  }
}
