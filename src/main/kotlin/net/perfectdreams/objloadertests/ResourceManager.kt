package net.perfectdreams.objloadertests

import org.lwjgl.opengl.GL30.*
import org.lwjgl.stb.STBImage
import org.lwjgl.system.MemoryStack.stackPush
import java.io.File
import java.nio.ByteBuffer

class ResourceManager {
    fun loadTexture(path: String): LoadedImage {
        val textureID: Int
        val imageBuffer: ByteBuffer?

        val imageBytes = File(path).readBytes()

        val t = ByteBuffer.allocateDirect(imageBytes.size)
        t.put(imageBytes)
        t.rewind()

        val (width, height) = stackPush().use { stack ->
            val width = stack.mallocInt(1)
            val height = stack.mallocInt(1)
            val channels = stack.mallocInt(1)

            imageBuffer = STBImage.stbi_load_from_memory(t, width, height, channels, 4)
            if (imageBuffer == null) {
                throw java.lang.RuntimeException("Failed to load texture: " + STBImage.stbi_failure_reason())
            }

            textureID = glGenTextures()
            glBindTexture(GL_TEXTURE_2D, textureID)

            glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA, width[0], height[0], 0, GL_RGBA, GL_UNSIGNED_BYTE, imageBuffer)

            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_NEAREST)
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_NEAREST)
            STBImage.stbi_image_free(imageBuffer)

            Pair(width.get(), height.get())
        }
        return LoadedImage(textureID, width, height)
    }

    data class LoadedImage(val textureId: Int, val width: Int, val height: Int)
}