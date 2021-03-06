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

package com.odiago.flumebase.plan;

import java.util.List;

import org.apache.hadoop.conf.Configuration;

import com.odiago.flumebase.parser.TypedField;
import com.odiago.flumebase.parser.WindowSpec;

/**
 * Join two input streams into an output stream based on an
 * expression criterion within a range interval.
 *
 * Requires that the test expression relies on a single "key" column
 * from each of the two input streams.
 */
public class HashJoinNode extends PlanNode {

  private String mLeftName; // name of the left stream.
  private String mRightName; // name of the right stream.
  private TypedField mLeftKey; // the key field from the left stream.
  private TypedField mRightKey; // the key field from the right stream.
  private WindowSpec mWindowWidth; // window spec over which the join is valid.
  private String mOutName; // name to assign to the output stream from this join.
  private List<TypedField> mLeftFields; // field names from the left stream.
  private List<TypedField> mRightFields; // field names from the right stream.
  private Configuration mConf; // user configuration.


  public HashJoinNode(String leftName, String rightName, TypedField leftKey, TypedField rightKey,
      WindowSpec windowWidth, String outName, List<TypedField> leftFieldNames,
      List<TypedField> rightFieldNames, Configuration conf) {
    mLeftName = leftName;
    mRightName = rightName;
    mLeftKey = leftKey;
    mRightKey = rightKey;
    mWindowWidth = windowWidth;
    mOutName = outName;
    mLeftFields = leftFieldNames;
    mRightFields = rightFieldNames;
    mConf = conf;
  }

  protected void formatParams(StringBuilder sb) {
    sb.append("Join mLeftName=");
    sb.append(mLeftName);
    sb.append(", mRightName=");
    sb.append(mRightName);
    sb.append(", mLeftKey=");
    sb.append(mLeftKey);
    sb.append(", mRightKey=");
    sb.append(mRightKey);
    sb.append(", width=");
    sb.append(mWindowWidth);
    sb.append(", outName=");
    sb.append(mOutName);
    formatAttributes(sb);
  }

  public String getLeftName() {
    return mLeftName;
  }

  public String getRightName() {
    return mRightName;
  }

  public TypedField getLeftKey() {
    return mLeftKey;
  }

  public TypedField getRightKey() {
    return mRightKey;
  }

  public WindowSpec getWindowWidth() {
    return mWindowWidth;
  }

  public String getOutputName() {
    return mOutName;
  }

  public Configuration getConf() {
    return mConf;
  }

  public List<TypedField> getLeftFields() {
    return mLeftFields;
  }

  public List<TypedField> getRightFields() {
    return mRightFields;
  }
}
