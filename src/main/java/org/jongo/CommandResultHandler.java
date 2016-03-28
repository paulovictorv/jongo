package org.jongo;

import org.bson.Document;

/**
 * Created by paulo on 25/03/16.
 */
public interface CommandResultHandler<T> {
    T map(Document commandResult);
}