# NexDrive Backend

---

The backend follows a decoupled, event-driven ecosystem structured across eight domain-driven microservices.

Key Architectural Pillars:

* Database-per-Service: Each microservice owns its private datastore (mixing relational and non-relational systems) to enforce loose coupling and independent        scalability.

* Multi-Tenancy Isolation: Implements logical data segregation using the Admin ID as the Tenant Identifier embedded inside service-to-service headers. This          prevents data leaks between independent educational centers.

* Composite Pattern for Hierarchical Storage: Folders and files are structured via composite entity patterns to permit infinite nesting with performant tree-        traversal indexing.

* Asynchronous Offloading: Time-consuming operations (e.g., file metadata indexing, audit log generation, and notifications) are offloaded to an event bus to keep   the user-facing APIs highly responsive.

---

## 🗺️ System Architecture

```mermaid
flowchart TD

subgraph group_edge["Edge"]
  node_clients(("Clients public callers"))
  node_gateway["API Gateway edge router"]
end

subgraph group_platform["Platform"]
  node_config["Config Server central config"]
  node_eureka["Eureka Server service discovery"]
end

subgraph group_users["User service"]
  node_user_api["User API controller layer"]
  node_user_core["User Logic service layer"]
  node_user_data[("User Store repository")]
  node_user_events(("User Events domain event"))
  node_user_mail["Mail Service notification"]
end

subgraph group_groups["Group service"]
  node_group_api["Group API controller layer"]
  node_membership_api["Membership API controller layer"]
  node_group_core["Group Logic service layer"]
  node_membership_core["Membership Logic service layer"]
  node_group_data[("Group Store repository")]
  node_group_client["User Client internal client"]
  node_group_events(("Delete Listener event consumer"))
end

subgraph group_storage["Storage service"]
  node_storage_api["Storage API controller layer"]
  node_storage_core["Storage Logic service layer"]
end

subgraph group_external["External"]
  node_s3[("S3 external object store")]
end

node_clients -->|"public traffic"| node_gateway
node_gateway -->|"routes"| node_user_api
node_gateway -->|"routes"| node_group_api
node_gateway -->|"routes"| node_storage_api
node_user_api -->|"uses"| node_user_core
node_user_core -->|"persists"| node_user_data
node_user_core -->|"notifies"| node_user_mail
node_user_core -->|"publishes"| node_user_events
node_group_api -->|"uses"| node_group_core
node_membership_api -->|"uses"| node_membership_core
node_group_core -->|"persists"| node_group_data
node_membership_core -->|"persists"| node_group_data
node_group_core -->|"calls"| node_group_client
node_group_events -->|"reacts"| node_group_core
node_user_events -.->|"event flow"| node_group_events
node_storage_api -->|"uses"| node_storage_core
node_storage_core -->|"issues URLs"| node_s3
node_gateway -.->|"discovers"| node_eureka
node_user_api -.->|"registers"| node_eureka
node_group_api -.->|"registers"| node_eureka
node_storage_api -.->|"registers"| node_eureka
node_gateway -.->|"loads config"| node_config
node_user_api -.->|"loads config"| node_config
node_group_api -.->|"loads config"| node_config
node_storage_api -.->|"loads config"| node_config
node_user_api -.->|"behind"| node_gateway
node_group_api -.->|"behind"| node_gateway
node_storage_api -.->|"behind"| node_gateway

click node_gateway "https://github.com/abhip47/nexdrive-backend/blob/main/apiGateway/src/main/java/com/appShala/apiGateway/ApiGatewayApplication.java"
click node_config "https://github.com/abhip47/nexdrive-backend/blob/main/configserver/src/main/java/com/appshala/configserver/ConfigserverApplication.java"
click node_eureka "https://github.com/abhip47/nexdrive-backend/blob/main/eurekaServer/src/main/java/com/appShala/eurekaServer/EurekaServerApplication.java"
click node_user_api "https://github.com/abhip47/nexdrive-backend/blob/main/userService/src/main/java/com/appshala/userService/Controller/UserController.java"
click node_user_core "https://github.com/abhip47/nexdrive-backend/blob/main/userService/src/main/java/com/appshala/userService/ServcieImpl/UserServiceImpl.java"
click node_user_data "https://github.com/abhip47/nexdrive-backend/blob/main/userService/src/main/java/com/appshala/userService/Repository/UserRepository.java"
click node_user_events "https://github.com/abhip47/nexdrive-backend/blob/main/userService/src/main/java/com/appshala/userService/Event/UserDeletedEvent.java"
click node_user_mail "https://github.com/abhip47/nexdrive-backend/blob/main/userService/src/main/java/com/appshala/userService/ServcieImpl/MailService.java"
click node_group_api "https://github.com/abhip47/nexdrive-backend/blob/main/userGroupService/src/main/java/com/appShala/userGroupService/Controller/UserGroupController.java"
click node_membership_api "https://github.com/abhip47/nexdrive-backend/blob/main/userGroupService/src/main/java/com/appShala/userGroupService/Controller/MembershipController.java"
click node_group_core "https://github.com/abhip47/nexdrive-backend/blob/main/userGroupService/src/main/java/com/appShala/userGroupService/ServiceImpl/UserGroupServiceImpl.java"
click node_membership_core "https://github.com/abhip47/nexdrive-backend/blob/main/userGroupService/src/main/java/com/appShala/userGroupService/ServiceImpl/MembershipServiceImpl.java"
click node_group_data "https://github.com/abhip47/nexdrive-backend/blob/main/userGroupService/src/main/java/com/appShala/userGroupService/Repository/UserGroupRepository.java"
click node_group_client "https://github.com/abhip47/nexdrive-backend/blob/main/userGroupService/src/main/java/com/appShala/userGroupService/client/UserServiceClient.java"
click node_group_events "https://github.com/abhip47/nexdrive-backend/blob/main/userGroupService/src/main/java/com/appShala/userGroupService/listener/UserDeletedEventListener.java"
click node_storage_api "https://github.com/abhip47/nexdrive-backend/blob/main/storageservice/src/main/java/com/appshala/storageservice/controller/StorageController.java"
click node_storage_core "https://github.com/abhip47/nexdrive-backend/blob/main/storageservice/src/main/java/com/appshala/storageservice/serviceImpl/StorageServiceImpl.java"

classDef toneBlue fill:#dbeafe,stroke:#2563eb,stroke-width:1.5px,color:#172554
classDef toneAmber fill:#fef3c7,stroke:#d97706,stroke-width:1.5px,color:#78350f
classDef toneMint fill:#dcfce7,stroke:#16a34a,stroke-width:1.5px,color:#14532d
classDef toneRose fill:#ffe4e6,stroke:#e11d48,stroke-width:1.5px,color:#881337
classDef toneIndigo fill:#e0e7ff,stroke:#4f46e5,stroke-width:1.5px,color:#312e81
classDef toneTeal fill:#ccfbf1,stroke:#0f766e,stroke-width:1.5px,color:#134e4a

class node_clients,node_gateway toneBlue
class node_config,node_eureka toneAmber
class node_user_api,node_user_core,node_user_data,node_user_events,node_user_mail toneMint
class node_group_api,node_membership_api,node_group_core,node_membership_core,node_group_data,node_group_client,node_group_events toneRose
class node_storage_api,node_storage_core toneIndigo
class node_s3 toneTeal
```
