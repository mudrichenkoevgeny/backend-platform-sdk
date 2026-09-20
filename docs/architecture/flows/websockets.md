# WebSocket Flows

This document details how real-time synchronization is handled across multiple instances of the backend platform.

## 1. Multi-Instance WebSocket Synchronization (Stateless)

The `KtorWebSocketManager` leverages Redis Pub/Sub (`websocket_messages` channel) to broadcast WebSocket messages across the cluster, maintaining statelessness in individual node memory. This ensures that users connected to different physical or virtual instances can still exchange real-time updates seamlessly.

```mermaid
sequenceDiagram
    participant ClientA as Client A (Connected to Node 1)
    participant WS1 as WebSocketManager (Node 1)
    participant Redis as Redis (Pub/Sub)
    participant WS2 as WebSocketManager (Node 2)
    participant ClientB as Client B (Connected to Node 2)

    Note over ClientA, ClientB: Users are connected to different Ktor instances via WSS
    ClientA->>WS1: Establish connection
    ClientB->>WS2: Establish connection
    
    Note over WS1, Redis: A system event triggers a broadcast<br/>(e.g., TargetType.ALL)
    WS1->>WS1: publishMessage(TargetType.ALL, frame)
    WS1->>Redis: publish "websocket_messages"
    
    par Node 1 receives its own broadcast
        Redis-->>WS1: receive pub/sub event
        WS1->>WS1: handlePubSubMessageLocally()
        WS1->>ClientA: forward frame (if target matches session/user)
    and Node 2 receives broadcast
        Redis-->>WS2: receive pub/sub event
        WS2->>WS2: handlePubSubMessageLocally()
        WS2->>ClientB: forward frame (if target matches session/user)
    end
```
