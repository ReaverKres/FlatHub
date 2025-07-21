package mappers

interface AdditionalParamMapper<ServerResponse, Entity> {

    fun map(baseEntity: Entity, data: ServerResponse): Entity
}