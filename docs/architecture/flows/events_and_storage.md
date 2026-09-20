# Events & Storage Infrastructure

This document describes cross-cutting infrastructure services: Kafka event publishing and S3/Local file storage.

## 1. Event Publishing & Subscribing (Kafka)

The `core:events` module provides a decoupled way to broadcast domain events to external systems or microservices via Apache Kafka.

```mermaid
sequenceDiagram
    participant UseCase as Feature UseCase
    participant Publisher as EventPublisherImpl
    participant Kafka as Kafka Broker
    participant Subscriber as EventSubscriberImpl
    participant Handler as Domain Event Handler

    %% Publishing
    UseCase->>Publisher: publish(topic, AppEvent)
    Publisher->>Publisher: Serialize to JSON (FoundationJson)
    Publisher->>Publisher: Attach headers (e.g., TraceID)
    Publisher->>Kafka: send(ProducerRecord)
    Kafka-->>Publisher: Acknowledge
    Publisher-->>UseCase: Success

    %% Subscribing (Background)
    Note over Subscriber, Kafka: Continually polling in a background Coroutine
    Kafka-->>Subscriber: fetch(ConsumerRecords)
    
    loop For each record
        Subscriber->>Subscriber: Deserialize JSON to AppEvent
        Subscriber->>Handler: invoke(AppEvent, MetadataHeaders)
        Handler-->>Subscriber: Processed
    end
    
    Subscriber->>Kafka: commitSync()
```

## 2. File Storage Abstraction (S3 / Local)

The `core:storage` module abstracts the complexity of blob storage, enabling seamless local development (using the filesystem) and production deployment (using S3/MinIO).

```mermaid
sequenceDiagram
    participant UseCase as Feature UseCase
    participant Storage as StorageService (S3 or Local)
    participant Backend as S3 (AWS) / Local Filesystem

    %% File Upload
    UseCase->>Storage: save(fileName, byteArray, contentType)
    
    alt Using S3StorageService
        Storage->>Backend: putObject(Bucket, Key, RequestBody)
        Backend-->>Storage: Uploaded
    else Using LocalStorageService
        Storage->>Backend: writeBytes(File(LOCAL_STORAGE_PATH, fileName))
        Backend-->>Storage: Saved
    end
    
    Storage-->>UseCase: Success (File Key)

    %% URL Generation
    UseCase->>Storage: getUrl(key)
    
    alt Using S3StorageService
        Storage->>Storage: Construct URL from S3_PUBLIC_URL + Key
    else Using LocalStorageService
        Storage->>Storage: Construct URL from S3_PUBLIC_URL + Key
    end
    
    Storage-->>UseCase: "https://assets.domain.com/avatar.png"
```
