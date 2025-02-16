package net.perfectdreams.objloadertests

class WavefrontOBJModel(
    val vertices: List<Vertex>,
    val faces: List<Face>,
    val textureCoordinates: List<TextureCoordinates>
) {
    data class Vertex(
        val x: Float,
        val y: Float,
        val z: Float,
        val w: Float
    )

    data class Face(
        val mtl: String?,
        // Remember that the face indexes START AT 1 NOT 0!!!
        val v0: VertexInfo,
        val v1: VertexInfo,
        val v2: VertexInfo
    )

    data class VertexInfo(
        val vertexIndex: Int,
        val textureCoordinatesIndex: Int?
    )

    data class TextureCoordinates(
        val u: Float,
        val v: Float,
        val w: Float
    )
}