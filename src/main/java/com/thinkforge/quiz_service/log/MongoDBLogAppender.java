package com.thinkforge.quiz_service.log;

import ch.qos.logback.core.AppenderBase;
import ch.qos.logback.classic.spi.ILoggingEvent;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoDatabase;
import org.bson.Document;

public class MongoDBLogAppender extends AppenderBase<ILoggingEvent> {

    private MongoClient mongoClient;
    private MongoCollection<Document> collection;

    private String uri;
    private String databaseName;
    private String collectionName;

    public void setUri(String uri) {
        this.uri = uri;
    }

    public void setDatabaseName(String databaseName) {
        this.databaseName = databaseName;
    }

    public void setCollectionName(String collectionName) {
        this.collectionName = collectionName;
    }

    @Override
    public void start() {
        super.start();
        mongoClient = MongoClients.create(uri);
        MongoDatabase database = mongoClient.getDatabase(databaseName);
        collection = database.getCollection(collectionName);
    }

    @Override
    protected void append(ILoggingEvent eventObject) {
        if (collection != null) {
            Document doc = new Document()
                    .append("timestamp", eventObject.getTimeStamp())
                    .append("level", eventObject.getLevel().toString())
                    .append("thread", eventObject.getThreadName())
                    .append("logger", eventObject.getLoggerName())
                    .append("message", eventObject.getFormattedMessage());

            collection.insertOne(doc);
        }
    }

    @Override
    public void stop() {
        super.stop();
        if (mongoClient != null) {
            mongoClient.close();
        }
    }
}
