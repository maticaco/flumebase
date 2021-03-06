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

package com.odiago.flumebase.flume;

import java.io.IOException;

import java.util.List;

import org.apache.avro.Schema;

import org.apache.thrift.TException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.odiago.flumebase.exec.FlowElementContext;
import com.odiago.flumebase.exec.StreamSymbol;

import com.odiago.flumebase.parser.TypedField;

/**
 * Container that launches an embedded Flume physical node and hosts
 * a logical node on it, configured with the appropriate EventSink to
 * deliver data to a processing flow.
 */
public class EmbeddedNode {
  private static final Logger LOG = LoggerFactory.getLogger(
      EmbeddedNode.class.getName());

  /**
   * Name of the flow and source we're providing data for; this is used as
   * the logical name of the embedded FlumeNode.
   */
  private String mFlowSourceId;

  /** FlowElementContext for the source FlowElement that wraps this EmbeddedNode. */
  private FlowElementContext mFlowElemContext;

  /** Manager for the logical Flume nodes in this process. */
  private EmbeddedFlumeConfig mFlumeConfig;

  /** Flume instruction for where to source the data from for this node. */
  private String mDataSource;

  /** Schema for records emitted by this node. */
  private Schema mOutputSchema;

  /** List of fields and types emitted by this node. */
  private List<TypedField> mFieldTypes;

  /** Symbol of the stream we represent. */
  private StreamSymbol mStreamSym;

  /**
   * Create a single embedded node instance.
   * @param flowSourceId - the flowId and source name within the flow being fulfilled.
   * @param flowContext - the context for the source FlowElement wrapping this object.
   * @param flumeConfig - the manager of the embedded Flume instance.
   * @param dataSource - the Flume 'source' argument for the logical node.
   * @param streamName - the name of the stream we are reading from into the query.
   */
  public EmbeddedNode(String flowSourceId, FlowElementContext flowContext,
      EmbeddedFlumeConfig flumeConfig, String dataSource, Schema outputSchema,
      List<TypedField> fieldTypes, StreamSymbol streamSymbol) {
    mFlowSourceId = flowSourceId;
    mFlowElemContext = flowContext;
    mFlumeConfig = flumeConfig;
    mDataSource = dataSource;
    mOutputSchema = outputSchema;
    mFieldTypes = fieldTypes;
    mStreamSym = streamSymbol;
  }

  /**
   * Start the embedded node instance.
   */
  public void open() throws IOException {
    LOG.debug("Opening sink binding for: " + mFlowSourceId);
    SinkContextBindings.get().bindContext(mFlowSourceId,
        new SinkContext(mFlowElemContext, mOutputSchema, mFieldTypes, mStreamSym));
    try {
      mFlumeConfig.createFlowSink(mFlowSourceId, mDataSource);
    } catch (TException te) {
      throw new IOException(te);
    }
  }

  /**
   * Stop the embedded node instance.
   */
  public void close() throws IOException {
    LOG.debug("Closing embedded node: " + mFlowSourceId);
    try {
      mFlumeConfig.stopFlowSink(mFlowSourceId);
    } catch (TException te) {
      throw new IOException(te);
    }
    SinkContextBindings.get().dropContext(mFlowSourceId);
  }
}
