### OpenAPI's definitions of the Rarible Protocol

**Be careful of backward compatibility!**
- do not change types of existing fields in incompatible way
  - unless properly coordinated with all possible clients
- do not add new *required* fields
  - there may be Kafka queues, which are left from previous deploys, containing old objects not having this field. 
  When Kafka consumers from those queues try to read the new model, they may fail with "No such field" exception.
 This actually has happened to us when we made `NftItemDto.meta` to be a required field:
  > com.fasterxml.jackson.module.kotlin.MissingKotlinParameterException: Instantiation of [simple type, class com.rarible.protocol.dto.NftItemDto] value failed for JSON property meta due to missing (therefore NULL) value for creator parameter meta which is a non-nullable type

  - now to overcome this: 
    - *firstly*, deploy the application with *optional* type (as before) but always having a value
    - wait for some time to guarantee all Kafka queues are emptied
    - *secondly*, make the field as *required*
- adding new model objects is backward compatible
- adding new *optional* fields is backward compatible
