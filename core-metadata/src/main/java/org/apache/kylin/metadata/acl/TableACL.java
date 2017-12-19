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

package org.apache.kylin.metadata.acl;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.kylin.common.persistence.RootPersistentEntity;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonProperty;

@SuppressWarnings("serial")
@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.NONE,
        getterVisibility = JsonAutoDetect.Visibility.NONE,
        isGetterVisibility = JsonAutoDetect.Visibility.NONE,
        setterVisibility = JsonAutoDetect.Visibility.NONE)
public class TableACL extends RootPersistentEntity {

    //user1 : [DB.TABLE1, DB.TABLE2], means that user1 can not query DB.TABLE1, DB.TABLE2
    @JsonProperty()
    private Map<String, TableBlackList> userTableBlackList;

    public TableACL() {
        userTableBlackList = new HashMap<>();
    }

    public Map<String, TableBlackList> getUserTableBlackList() {
        return userTableBlackList;
    }

    public List<String> getTableBlackList(String username) {
        TableBlackList tableBlackList = userTableBlackList.get(username);
        if (tableBlackList == null) {
            tableBlackList = new TableBlackList();
        }
        return tableBlackList.getTables();
    }

    //get users that can not query the table
    public List<String> getBlockedUserByTable(String table) {
        List<String> results = new ArrayList<>();
        for (String user : userTableBlackList.keySet()) {
            TableBlackList tables = userTableBlackList.get(user);
            if (tables.contains(table)) {
                results.add(user);
            }
        }
        return results;
    }

    public TableACL add(String username, String table) {
        if (userTableBlackList == null) {
            userTableBlackList = new HashMap<>();
        }
        TableBlackList tableBlackList = userTableBlackList.get(username);

        if (tableBlackList == null) {
            tableBlackList = new TableBlackList();
            userTableBlackList.put(username, tableBlackList);
        }

        //before add, check exists
        checkACLExists(username, table, tableBlackList);
        tableBlackList.add(table);
        return this;
    }

    private void checkACLExists(String username, String table, TableBlackList tableBlackList) {
        if (tableBlackList.contains(table)) {
            throw new RuntimeException("Operation fail, can not revoke user's table query permission.Table ACL " + table
                    + ":" + username + " already exists!");
        }
    }

    public TableACL delete(String username, String table) {
        if (isTableInBlackList(username, table)) {
            throw new RuntimeException("Operation fail, can not grant user table query permission.Table ACL " + table
                    + ":" + username + " is not found!");
        }
        TableBlackList tableBlackList = userTableBlackList.get(username);
        tableBlackList.remove(table);
        return this;
    }

    private boolean isTableInBlackList(String username, String table) {
        return userTableBlackList == null
                || userTableBlackList.get(username) == null
                || (!userTableBlackList.get(username).contains(table));
    }

    @JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.NONE,
            getterVisibility = JsonAutoDetect.Visibility.NONE,
            isGetterVisibility = JsonAutoDetect.Visibility.NONE,
            setterVisibility = JsonAutoDetect.Visibility.NONE)
    static class TableBlackList {
        @JsonProperty()
        List<String> tables;

        TableBlackList() {
            tables = new ArrayList<>();
        }

        public int size() {
            return tables.size();
        }

        public boolean isEmpty() {
            return tables.isEmpty();
        }

        public boolean contains(String s) {
            return tables.contains(s);
        }

        public boolean add(String s) {
            return tables.add(s);
        }

        public boolean remove(String s) {
            return tables.remove(s);
        }

        public List<String> getTables() {
            return tables;
        }
    }
}
