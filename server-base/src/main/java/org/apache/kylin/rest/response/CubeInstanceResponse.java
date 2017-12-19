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

package org.apache.kylin.rest.response;

import org.apache.kylin.cube.CubeInstance;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Created by luwei on 17-4-17.
 */
@SuppressWarnings("serial")
public class CubeInstanceResponse extends CubeInstance {

    public void setProject(String project) {
        this.project = project;
    }

    public void setModel(String model) {
        this.model = model;
    }

    public void setIs_streaming(boolean is_streaming) {
        this.is_streaming = is_streaming;
    }

    public void setDraft(boolean isDraft) {
        this.isDraft = isDraft;
    }

    public void setPartitionDateColumn(String partitionDateColumn) {
        this.partitionDateColumn = partitionDateColumn;
    }

    public void setPartitionDateStart(long partitionDateStart) {
        this.partitionDateStart = partitionDateStart;
    }

    @JsonProperty("project")
    private String project;
    @JsonProperty("model")
    private String model;
    @JsonProperty("is_streaming")
    private boolean is_streaming;
    @JsonProperty("is_draft")
    private boolean isDraft;
    @JsonProperty("partitionDateColumn")
    private String partitionDateColumn;
    @JsonProperty("partitionDateStart")
    private long partitionDateStart;

    public CubeInstanceResponse(CubeInstance cubeInstance) {
        setUuid(cubeInstance.getUuid());
        setVersion(cubeInstance.getVersion());
        setName(cubeInstance.getName());
        setOwner(cubeInstance.getOwner());
        setDescName(cubeInstance.getDescName());
        setCost(cubeInstance.getCost());
        setStatus(cubeInstance.getStatus());
        setSegments(cubeInstance.getSegments());
        setCreateTimeUTC(cubeInstance.getCreateTimeUTC());
    }
}
