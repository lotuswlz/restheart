/*
 * RESTHeart - the Web API for MongoDB
 * Copyright (C) SoftInstigate Srl
 * 
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 * 
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.restheart.handlers.database;

import com.mongodb.DBObject;
import org.restheart.utils.HttpStatus;
import org.restheart.handlers.PipedHttpHandler;
import org.restheart.handlers.RequestContext;
import io.undertow.server.HttpServerExchange;
import java.util.List;
import java.util.stream.Collectors;
import org.bson.BsonDocument;
import org.restheart.db.Database;
import org.restheart.db.DbsDAO;
import org.restheart.hal.Representation;
import org.restheart.utils.JsonUtils;
import org.restheart.utils.ResponseHelper;

/**
 *
 * @author Andrea Di Cesare {@literal <andrea@softinstigate.com>}
 */
public class GetDBHandler extends PipedHttpHandler {

    /**
     * Creates a new instance of GetDBHandler
     */
    public GetDBHandler() {
        super();
    }

    public GetDBHandler(PipedHttpHandler next) {
        super(next, new DbsDAO());
    }

    public GetDBHandler(PipedHttpHandler next, Database dbsDAO) {
        super(next, dbsDAO);
    }

    /**
     *
     * @param exchange
     * @param context
     * @throws Exception
     */
    @Override
    public void handleRequest(HttpServerExchange exchange, RequestContext context) throws Exception {
        List<String> colls = getDatabase().getCollectionNames(getDatabase().getDB(context.getDBName()));

        List<BsonDocument> data = null;

        if (context.getPagesize() > 0) {
            data = getDatabase().getDatabaseData(
                    context.getDBName(),
                    colls,
                    context.getPage(),
                    context.getPagesize());
        }

        //TODO remove this after migration to mongodb driver 3.2 completes
        List<DBObject> _data = null;
        if (data != null) {
            _data = data.stream().map(props -> {
                return JsonUtils.convertBsonValueToDBObject(props);
            }).collect(Collectors.toList());
        }

        DBRepresentationFactory repf = new DBRepresentationFactory();
        Representation rep = repf.getRepresentation(exchange, context, _data, getDatabase().getDBSize(colls));

        ResponseHelper.injectEtagHeader(exchange, context.getDbProps());

        exchange.setStatusCode(HttpStatus.SC_OK);

        // call the next handler if existing
        if (getNext() != null) {
            DBObject responseContent = rep.asDBObject();
            context.setResponseContent(responseContent);

            getNext().handleRequest(exchange, context);
        }

        repf.sendRepresentation(exchange, context, rep);

        exchange.endExchange();
    }
}
