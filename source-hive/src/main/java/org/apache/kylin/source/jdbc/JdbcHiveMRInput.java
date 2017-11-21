/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 * 
 *     http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
*/

package org.apache.kylin.source.jdbc;

import org.apache.kylin.common.KylinConfig;
import org.apache.kylin.cube.CubeManager;
import org.apache.kylin.engine.mr.steps.CubingExecutableUtil;
import org.apache.kylin.job.JoinedFlatTable;
import org.apache.kylin.job.constant.ExecutableConstants;
import org.apache.kylin.job.execution.AbstractExecutable;
import org.apache.kylin.job.execution.DefaultChainedExecutable;
import org.apache.kylin.metadata.model.IJoinedFlatTableDesc;
import org.apache.kylin.metadata.model.PartitionDesc;
import org.apache.kylin.metadata.model.TblColRef;
import org.apache.kylin.source.hive.HiveMRInput;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class JdbcHiveMRInput extends HiveMRInput {

    private static final Logger logger = LoggerFactory.getLogger(JdbcHiveMRInput.class);

    public IMRBatchCubingInputSide getBatchCubingInputSide(IJoinedFlatTableDesc flatDesc) {
        return new BatchCubingInputSide(flatDesc);
    }

    public static class BatchCubingInputSide extends HiveMRInput.BatchCubingInputSide {

        public BatchCubingInputSide(IJoinedFlatTableDesc flatDesc) {
            super(flatDesc);
        }

        @Override
        protected void addStepPhase1_DoCreateFlatTable(DefaultChainedExecutable jobFlow) {
            final String cubeName = CubingExecutableUtil.getCubeName(jobFlow.getParams());
            final String hiveInitStatements = JoinedFlatTable.generateHiveInitStatements(flatTableDatabase);
            final String jobWorkingDir = getJobWorkingDir(jobFlow);

            jobFlow.addTask(createSqoopToFlatHiveStep(jobWorkingDir, cubeName));
            jobFlow.addTask(createFlatHiveTableFromFiles(hiveInitStatements, jobWorkingDir));
        }

        private AbstractExecutable createFlatHiveTableFromFiles(String hiveInitStatements, String jobWorkingDir) {
            final String dropTableHql = JoinedFlatTable.generateDropTableStatement(flatDesc);
            KylinConfig config = KylinConfig.getInstanceFromEnv();
            String filedDelimiter = config.getFieldDelimiter();
            // Sqoop does not support exporting SEQUENSEFILE to Hive now SQOOP-869
            final String createTableHql = JoinedFlatTable.generateCreateTableStatement(flatDesc, jobWorkingDir,
                    "TEXTFILE", filedDelimiter);

            HiveCmdStep step = new HiveCmdStep();
            step.setCmd(hiveInitStatements + dropTableHql + createTableHql);
            return step;
        }

        private AbstractExecutable createSqoopToFlatHiveStep(String jobWorkingDir, String cubeName) {
            KylinConfig config = CubeManager.getInstance(KylinConfig.getInstanceFromEnv()).getCube(cubeName)
                    .getConfig();
            PartitionDesc partitionDesc = flatDesc.getDataModel().getPartitionDesc();
            String partCol = null;
            String partitionString = null;
            TblColRef splitColRef;
            if (partitionDesc.isPartitioned()) {
                partCol = partitionDesc.getPartitionDateColumn();//tablename.colname
                partitionString = partitionDesc.getPartitionConditionBuilder().buildDateRangeCondition(partitionDesc,
                        flatDesc.getSegRange());
                splitColRef = partitionDesc.getPartitionDateColumnRef();
            } else {
                splitColRef = flatDesc.getAllColumns().iterator().next();
            }

            String splitTable = null;
            String splitColumn = null;
            String splitDatabase = null;
            splitTable = splitColRef.getTableRef().getTableName();
            splitColumn = splitColRef.getName();
            splitDatabase = splitColRef.getColumnDesc().getTable().getDatabase();

            //using sqoop to extract data from jdbc source and dump them to hive
            String selectSql = JoinedFlatTable.generateSelectDataStatement(flatDesc, true, new String[] { partCol });
            String hiveTable = flatDesc.getTableName();
            String connectionUrl = config.getJdbcSourceConnectionUrl();
            String driverClass = config.getJdbcSourceDriver();
            String jdbcUser = config.getJdbcSourceUser();
            String jdbcPass = config.getJdbcSourcePass();
            String sqoopHome = config.getSqoopHome();
            String filedDelimiter = config.getFieldDelimiter();

            String bquery = String.format("SELECT min(%s), max(%s) FROM %s.%s", splitColumn, splitColumn, splitDatabase,
                    splitTable);
            if (partitionString != null) {
                bquery += " where " + partitionString;
            }

            String cmd = String.format(String.format(
                    "%s/sqoop import -Dorg.apache.sqoop.splitter.allow_text_splitter=true "
                            + "--connect \"%s\" --driver %s --username %s --password %s --query \"%s AND \\$CONDITIONS\" "
                            + "--target-dir %s/%s --split-by %s.%s --boundary-query \"%s\" --null-string '' "
                            + "--null-non-string '' --fields-terminated-by '" + filedDelimiter + "'",
                    sqoopHome, connectionUrl, driverClass, jdbcUser, jdbcPass, selectSql, jobWorkingDir, hiveTable,
                    splitTable, splitColumn, bquery));
            logger.debug(String.format("sqoop cmd:%s", cmd));
            CmdStep step = new CmdStep();
            step.setCmd(cmd);
            step.setName(ExecutableConstants.STEP_NAME_CREATE_FLAT_HIVE_TABLE);
            return step;
        }

        @Override
        protected void addStepPhase1_DoMaterializeLookupTable(DefaultChainedExecutable jobFlow) {
            // skip
        }
    }
}
