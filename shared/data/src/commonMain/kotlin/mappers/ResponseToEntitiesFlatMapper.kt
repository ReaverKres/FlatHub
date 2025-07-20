package mappers

interface ResponseToEntitiesFlatMapper<ServerResponse, Entity> {

    fun map(data: ServerResponse): Entity
}