package net.perfectdreams.objloadertests

object WavefrontLoader {
    fun loadWavefrontOBJ(content: String): WavefrontOBJModel {
        val vertices = mutableListOf<WavefrontOBJModel.Vertex>()
        val textureCoordinates = mutableListOf<WavefrontOBJModel.TextureCoordinates>()
        val faces = mutableListOf<WavefrontOBJModel.Face>()
        var activeMaterialLibrary: String? = null

        for (line in content.lines()) {
            try {
                // A comment
                if (line.startsWith("#"))
                    continue

                // Geometric Vertices
                // List of geometric vertices, with (x, y, z, [w]) coordinates, w is optional and defaults to 1.0.
                if (line.startsWith("v ")) {
                    val elements = line.split(" ").drop(1).filter { it.isNotBlank() }.map { it.trim().toFloat() }

                    val x = elements[0]
                    val y = elements[1]
                    val z = elements[2]
                    val w = elements.getOrNull(3) ?: 1.0f

                    vertices.add(WavefrontOBJModel.Vertex(x, y, z, w))
                }

                // Element faces
                if (line.startsWith("f ")) {
                    val elements = line.split(" ").drop(1)

                    val vertexIndexes = mutableListOf<WavefrontOBJModel.VertexInfo>()

                    for ((index, element) in elements.withIndex()) {
                        val indexes = element.split("/")

                        // The indexes in the slashes do not denotate each VERTEX OF THE FACE
                        // They actually mean other things (shocking), what is REALLY the VERTEX is the FIRST element before the slash
                        // OBJ files can also not have any slashes in the f section!!!!

                        val vertexIndex = indexes[0].toInt()
                        val textureCoordinatesIndex = indexes.getOrNull(1)?.toInt()

                        vertexIndexes.add(WavefrontOBJModel.VertexInfo(vertexIndex, textureCoordinatesIndex))
                    }

                    if (vertexIndexes.size != 3)
                        error("Unsupported face count: ${vertexIndexes.size}")

                    faces.add(
                        WavefrontOBJModel.Face(
                            activeMaterialLibrary,
                            vertexIndexes[0],
                            vertexIndexes[1],
                            vertexIndexes[2]
                        )
                    )
                }

                if (line.startsWith("o ")) {
                    // Named Object
                    // This resets the activeMaterialLibrary because some parsers do that
                    activeMaterialLibrary = null
                }

                // Texture Coordinates
                if (line.startsWith("vt ")) {
                    val elements = line.split(" ").drop(1).filter { it.isNotBlank() }.map { it.trim().toFloat() }

                    val u = elements[0]
                    val v = elements[1]
                    val w = elements.getOrNull(3) ?: 0.0f

                    textureCoordinates.add(
                        WavefrontOBJModel.TextureCoordinates(
                            u,
                            v,
                            w
                        )
                    )
                }

                // Material Template Library
                if (line.startsWith("usemtl ")) {
                    // Materials from a mtl file
                    activeMaterialLibrary = line.split(" ").drop(1).first()
                }
            } catch (e: Exception) {
                println("Failed on line $line")
                throw RuntimeException(e)
            }
        }

        return WavefrontOBJModel(vertices, faces, textureCoordinates)
    }
}