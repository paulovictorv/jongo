/*
 * Copyright (C) 2011 Benoit GUEROUT <bguerout at gmail dot com> and Yves AMSELLEM <amsellem dot yves at gmail dot com>
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jongo;

import com.mongodb.BasicDBObject;
import com.mongodb.DBObject;
import com.mongodb.WriteConcern;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.UpdateOptions;
import com.mongodb.client.result.UpdateResult;
import org.bson.LazyBSONObject;
import org.jongo.query.Query;
import org.jongo.query.QueryFactory;

public class Update {

    private final MongoCollection<BasicDBObject> collection;
    private final Query query;
    private final QueryFactory queryFactory;

    private WriteConcern writeConcern;
    private boolean upsert = false;
    private boolean multi = false;

    Update(MongoCollection<BasicDBObject> collection, WriteConcern writeConcern, QueryFactory queryFactory, String query, Object... parameters) {
        this.collection = collection;
        this.writeConcern = writeConcern;
        this.queryFactory = queryFactory;
        this.query = createQuery(query, parameters);
    }

    public UpdateResult with(String modifier) {
        return with(modifier, new Object[0]);
    }

    public UpdateResult with(String modifier, Object... parameters) {
        Query updateQuery = queryFactory.createQuery(modifier, parameters);
        UpdateOptions updateOptions = new UpdateOptions();
        updateOptions.upsert(this.upsert);

        if (multi) {
            return collection.updateMany(this.query.toBson(), updateQuery.toBson(), updateOptions);
        } else {
            return collection.updateOne(this.query.toBson(), updateQuery.toBson(), updateOptions);
        }
    }

    public UpdateResult with(Object pojo) {
        Query updateDbo = queryFactory.createQuery("{$set:#}", pojo);
        removeIdField(updateDbo.toDBObject());

        if (multi) {
            return collection.updateMany(this.query.toBson(), updateDbo.toBson());
        } else {
            return collection.updateOne(this.query.toBson(), updateDbo.toBson());
        }
    }

    private void removeIdField(DBObject updateDbo) {
        DBObject pojoAsDbo = (DBObject) updateDbo.get("$set");
        if (pojoAsDbo.containsField("_id")) {
            // Need to materialize lazy objects which are read only
            if (pojoAsDbo instanceof LazyBSONObject) {
                BasicDBObject expanded = new BasicDBObject();
                expanded.putAll(pojoAsDbo);
                updateDbo.put("$set", expanded);
                pojoAsDbo = expanded;
            }
            pojoAsDbo.removeField("_id");
        }
    }

    public Update upsert() {
        this.upsert = true;
        return this;
    }

    public Update multi() {
        this.multi = true;
        return this;
    }

    private Query createQuery(String query, Object[] parameters) {
        try {
            return this.queryFactory.createQuery(query, parameters);
        } catch (Exception e) {
            String message = String.format("Unable execute update operation using query %s", query);
            throw new IllegalArgumentException(message, e);
        }
    }
}
