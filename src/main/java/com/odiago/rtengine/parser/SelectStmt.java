// (c) Copyright 2010 Odiago, Inc.

package com.odiago.rtengine.parser;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.avro.Schema;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.odiago.rtengine.exec.HashSymbolTable;
import com.odiago.rtengine.exec.SymbolTable;

import com.odiago.rtengine.plan.ConsoleOutputNode;
import com.odiago.rtengine.plan.FlowSpecification;
import com.odiago.rtengine.plan.MemoryOutputNode;
import com.odiago.rtengine.plan.PlanContext;
import com.odiago.rtengine.plan.PlanNode;
import com.odiago.rtengine.plan.ProjectionNode;
import com.odiago.rtengine.plan.StrMatchFilterNode;

/**
 * SELECT statement.
 */
public class SelectStmt extends SQLStatement {

  private static final Logger LOG = LoggerFactory.getLogger(SelectStmt.class.getName());

  /**
   * Configuration key that specifies how we should deliver output records of a
   * top-level RTSQL statement to the client. If this is set to "$console," we
   * print to the screen. Other strings cause us to allocate a list buffer that
   * can be retrieved later by the client.
   */
  public static final String CLIENT_SELECT_TARGET_KEY = "rtsql.client.select.target";

  /** Special value for rtsql.client.select.target that prints to stdout. */
  public static final String CONSOLE_SELECT_TARGET = "$console";

  /** The default for rtsql.client.select.target is to use the console. */
  public static final String DEFAULT_CLIENT_SELECT_TARGET = CONSOLE_SELECT_TARGET;

  private FieldList mFields;
  // Source stream for the FROM clause. must be a LiteralSource or a SelectStmt.
  // (That fact is proven by a TypeChecker visitor.)
  private SQLStatement mSource;
  private WhereConditions mWhere;

  public SelectStmt(FieldList fields, SQLStatement source, WhereConditions where) {
    mFields = fields;
    mSource = source;
    mWhere = where;
  }

  public FieldList getFields() {
    return mFields;
  }

  public SQLStatement getSource() {
    return mSource;
  }

  public WhereConditions getWhereConditions() {
    return mWhere;
  }

  @Override
  public void format(StringBuilder sb, int depth) {
    pad(sb, depth);
    sb.append("SELECT");
    sb.append("\n");
    pad(sb, depth);
    sb.append("fields:\n");
    if (mFields.isAllFields()) {
      pad(sb, depth + 1);
      sb.append("(all)\n");
    } else {
      for (String fieldName : mFields) {
        pad(sb, depth + 1);
        sb.append(fieldName);
        sb.append("\n");
      }
    }
    pad(sb, depth);
    sb.append("FROM:\n");
    mSource.format(sb, depth + 1);
    if (null != mWhere) {
      pad(sb, depth);
      sb.append("WHERE:\n");
      pad(sb, depth + 1);
      sb.append(mWhere.getText());
      sb.append("\n");
    }
  }

  @Override
  public PlanContext createExecPlan(PlanContext planContext) {
    SQLStatement source = getSource();
    WhereConditions where = getWhereConditions();

    // Create an execution plan to build the source (it may be a single node
    // representing a Flume source or file, or it may be an entire DAG because
    // we use another SELECT statement as a source) inside a new context.
    PlanContext sourceInCtxt = new PlanContext(planContext);
    sourceInCtxt.setRoot(false);
    sourceInCtxt.setFlowSpec(new FlowSpecification());
    PlanContext sourceOutCtxt = source.createExecPlan(sourceInCtxt);

    // Now incorporate that entire plan into our plan.
    FlowSpecification flowSpec = planContext.getFlowSpec();
    flowSpec.addNodesFromDAG(sourceOutCtxt.getFlowSpec());

    // Add a projection level that grabs only the fields we care about.
    Schema sourceSchema = sourceOutCtxt.getSchema();
    Set<String> allRequiredFieldNames = new HashSet<String>();

    // Create a list containing the (ordered) set of fields we want emitted to the console.
    List<String> consoleFields = new ArrayList<String>();

    // Start with all the fields the user explicitly selected.
    FieldList fieldList = getFields();
    if (fieldList.isAllFields()) {
      // TODO(aaron): Figure out how to get a concrete list of names from this.
      throw new RuntimeException("Do not know how to project to field list '*'");
    } else {
      for (String field : fieldList) {
        allRequiredFieldNames.add(field);
        consoleFields.add(field);
      }
    }

    if (null != where) {
      // Add to this all the fields required by the where clause.
      allRequiredFieldNames.addAll(where.getRequiredFields());
    }

    // Create the projected schema based on the symbol table returned by our source. 
    Schema projectedSchema = createFieldSchema(allRequiredFieldNames,
        sourceOutCtxt.getSymbolTable());
    ProjectionNode projectionNode = new ProjectionNode();
    projectionNode.setAttr(PlanNode.INPUT_SCHEMA_ATTR, sourceSchema);
    projectionNode.setAttr(PlanNode.OUTPUT_SCHEMA_ATTR, projectedSchema);
    flowSpec.attachToLastLayer(projectionNode);

    if (where != null) {
      // Non-null filter conditions; apply the filter to all of our sources.
      String filterText = where.getText();
      PlanNode filterNode = new StrMatchFilterNode(filterText);
      flowSpec.attachToLastLayer(filterNode);
    }

    PlanContext outContext = planContext;
    if (planContext.isRoot()) {
      String selectTarget = planContext.getConf().get(CLIENT_SELECT_TARGET_KEY,
          DEFAULT_CLIENT_SELECT_TARGET);
      if (CONSOLE_SELECT_TARGET.equals(selectTarget)) {
        // SELECT statements that are root queries go to the console.
        flowSpec.attachToLastLayer(new ConsoleOutputNode(consoleFields));
      } else {
        // Client has specified that outputs of this root query go to a named memory buffer.
        flowSpec.attachToLastLayer(new MemoryOutputNode(selectTarget, consoleFields));
      }
    } else {
      // If the initial projection contained both explicitly selected fields as
      // well as implicitly selected fields (e.g., for the WHERE clause), attach another
      // projection layer that extracts only the explicitly selected fields.

      // SELECT as a sub-query needs to create an output context with a
      // symbol table that contains the fields we expose through projection. 
      outContext = new PlanContext(planContext);
      SymbolTable inTable = planContext.getSymbolTable();
      SymbolTable outTable = new HashSymbolTable(inTable);
      Set<String> outputFieldNames = new HashSet<String>();
      if (fieldList.isAllFields()) {
        // TODO(aaron): Build a way to get the source to enumerate all fields it exposes.
        throw new RuntimeException("Cannot project to field list '*' in a subquery yet.");
      } else {
        // Resolve the list of fields against the symbols exposed by the source.
        // Copy those symbols we're interested in to the returned symbol table. 
        SymbolTable sourceSymTable = sourceOutCtxt.getSymbolTable();
        for (String fieldName : fieldList) {
          outTable.addSymbol(sourceSymTable.resolve(fieldName));
          outputFieldNames.add(fieldName);
        }
      }
      Schema outputSchema = createFieldSchema(outputFieldNames,
          sourceOutCtxt.getSymbolTable());
      ProjectionNode cleanupProjection = new ProjectionNode();
      projectionNode.setAttr(PlanNode.OUTPUT_SCHEMA_ATTR, outputSchema);
      flowSpec.attachToLastLayer(cleanupProjection);

      outContext.setSymbolTable(outTable);
      outContext.setSchema(outputSchema);
    }

    return outContext;
  }
}

