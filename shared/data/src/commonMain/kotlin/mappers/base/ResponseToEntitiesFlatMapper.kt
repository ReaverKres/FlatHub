package mappers.base

interface ResponseToEntitiesFlatMapper<ServerResponse, Entity> {

    fun map(data: ServerResponse): Entity
}