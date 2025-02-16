package net.perfectdreams.objloadertests

import org.joml.Matrix4f
import org.joml.Quaternionf
import org.joml.Vector3f
import org.lwjgl.Version
import org.lwjgl.glfw.Callbacks.glfwFreeCallbacks
import org.lwjgl.glfw.GLFW
import org.lwjgl.glfw.GLFW.*
import org.lwjgl.glfw.GLFWErrorCallback
import org.lwjgl.glfw.GLFWVidMode
import org.lwjgl.opengl.GL
import org.lwjgl.opengl.GL20.*
import org.lwjgl.opengl.GL30
import org.lwjgl.opengl.GL30.glUseProgram
import org.lwjgl.opengl.GL32
import org.lwjgl.opengl.GL43
import org.lwjgl.system.MemoryStack.stackPush
import org.lwjgl.system.MemoryUtil
import org.lwjgl.system.MemoryUtil.NULL
import java.io.File
import java.nio.IntBuffer
import kotlin.random.Random

class ObjLoaderTests {
    // The window handle
    private var window: Long = 0

    private val windowWidth = 1280
    private val windowHeight = 720

    // Camera position
    lateinit var cameraPosition: Vector3f

    // Projection Matrix
    lateinit var projection: Matrix4f
    var useOrthographicProjection = false

    // Camera matrix
    lateinit var view: Matrix4f
    var cameraRotationY = 0.0f

    fun start() {
        useOrthographicProjection = false
        println("Hello LWJGL " + Version.getVersion() + "!")

        init()
        loop()

        // Free the window callbacks and destroy the window
        glfwFreeCallbacks(window)
        glfwDestroyWindow(window)

        // Terminate GLFW and free the error callback
        glfwTerminate()
        glfwSetErrorCallback(null)!!.free()
    }

    private fun init() {
        // Setup an error callback. The default implementation
        // will print the error message in System.err.
        GLFWErrorCallback.createPrint(System.err).set()

        // Initialize GLFW. Most GLFW functions will not work before doing this.
        check(glfwInit()) { "Unable to initialize GLFW" }

        // Configure GLFW
        glfwDefaultWindowHints() // optional, the current window hints are already the default
        glfwWindowHint(GLFW_VISIBLE, GLFW_FALSE) // the window will stay hidden after creation
        glfwWindowHint(GLFW_RESIZABLE, GLFW_TRUE) // the window will be resizable

        glfwWindowHint(GLFW_CLIENT_API, GLFW_OPENGL_API)
        glfwWindowHint(GLFW_CONTEXT_VERSION_MAJOR, 4)
        glfwWindowHint(GLFW_CONTEXT_VERSION_MINOR, 3)
        // Enable core profile
        glfwWindowHint(GLFW.GLFW_OPENGL_PROFILE, GLFW.GLFW_OPENGL_CORE_PROFILE)
        glfwWindowHint(GLFW.GLFW_OPENGL_FORWARD_COMPAT, GLFW.GLFW_TRUE)

        // Create the window
        window = glfwCreateWindow(windowWidth, windowHeight, "OBJ Loader Tests", NULL, NULL)
        if (window == NULL) throw RuntimeException("Failed to create the GLFW window")

        // Setup a key callback. It will be called every time a key is pressed, repeated or released.
        glfwSetKeyCallback(window) { window, key, scancode, action, mods ->
            // We will detect this in the rendering loop
            if (key == GLFW_KEY_ESCAPE && action == GLFW_RELEASE) {
                glfwSetWindowShouldClose(
                    window,
                    true
                )
            }

            if (key == GLFW_KEY_A && action == GLFW_RELEASE) {
                cameraRotationY -= 10f
                updateViewMatrix()
            }

            if (key == GLFW_KEY_D && action == GLFW_RELEASE) {
                cameraRotationY += 10f
                updateViewMatrix()
            }
        }

        stackPush().use { stack ->
            val pWidth: IntBuffer = stack.mallocInt(1) // int*
            val pHeight: IntBuffer = stack.mallocInt(1) // int*

            // Get the window size passed to glfwCreateWindow
            glfwGetWindowSize(window, pWidth, pHeight)

            // Get the resolution of the primary monitor
            val vidmode: GLFWVidMode = glfwGetVideoMode(glfwGetPrimaryMonitor())!!

            // Center the window
            glfwSetWindowPos(
                window,
                (vidmode.width() - pWidth[0]) / 2,
                (vidmode.height() - pHeight[0]) / 2
            )
        }

        // Make the OpenGL context current
        glfwMakeContextCurrent(window)
        // Enable v-sync (to disable, use 0)
        glfwSwapInterval(1)

        // Make the window visible
        glfwShowWindow(window)
    }

    private fun loop() {
        // This line is critical for LWJGL's interoperation with GLFW's
        // OpenGL context, or any context that is managed externally.
        // LWJGL detects the context that is current in the current thread,
        // creates the GLCapabilities instance and makes the OpenGL
        // bindings available for use.
        GL.createCapabilities()

        // Required for transparent textures!
        glEnable(GL_BLEND)
        glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA)

        glEnable(GL_DEPTH_TEST)

        GL43.glEnable(GL43.GL_DEBUG_OUTPUT)
        GL43.glEnable(GL43.GL_DEBUG_OUTPUT_SYNCHRONOUS)
        GL43.glDebugMessageCallback({ source: Int, type: Int, id: Int, severity: Int, length: Int, messagePointer: Long, userParamPointer: Long ->
            val debugMessage: String = MemoryUtil.memUTF8(messagePointer, length)

            val sourceStr = when (source) {
                GL43.GL_DEBUG_SOURCE_API -> "API"
                else -> "Unknown ($source)"
            }
            println("[$sourceStr]: $debugMessage")

            try {
                error("test")
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }, MemoryUtil.NULL)

        // Set the clear color
        glClearColor(1f, 0.0f, 0.0f, 1.0f)

        updateProjectionMatrix()
        updateViewMatrix()

        val shaderManager = ShaderManager()
        val programId = shaderManager.loadShader("game.vsh", "game.fsh")
        val simsSknProgramId = shaderManager.loadShader("sim.vsh", "sim.fsh")

        val cubeObj = WavefrontLoader.loadWavefrontOBJ(File("pomni_hat_triangulated.obj").readText())
        // val cubeMtl = loadWavefrontMTL(File("pomni_hat_triangulated_cube.mtl").readText())

        val resourceManager = ResourceManager()
        val texture = resourceManager.loadTexture("texture.png")

        val cubeObjVAO = createWavefrontOBJModelVAO(cubeObj)

        // val skn = TheSimsSKNLoader.loadTheSimsSKN(File("C:\\Users\\leona\\IdeaProjects\\ObjLoaderTests\\TheSims1Skin\\test.skn").readText())
        val bodySkn = TheSimsSKNLoader.loadTheSimsSKN(File(".\\TheSims1Skin\\xskin-bj80fafit_JVDPRMiniFlareSleeve-PELVIS-BODY.skn").readText())
        val bodyBitmapId = resourceManager.loadTexture(".\\TheSims1Skin\\${bodySkn.bitmapFileName}.bmp")
        val skeleton = TheSimsSKNLoader.loadTheSimsCMX(File(".\\TheSims1Skin\\adult-skeleton.cmx").readText())
        val headSkn = TheSimsSKNLoader.loadTheSimsSKN(File(".\\TheSims1Skin\\xskin-c798fa_realbuffy-HEAD-HEAD.skn").readText())
        val headBitmapId = resourceManager.loadTexture(".\\TheSims1Skin\\${headSkn.bitmapFileName}.bmp")

        skeleton.skeletons[0].suits.forEachIndexed { index, suit ->
            println("Index $index is ${suit.boneName}")
        }

        val (cmx, frames) = TheSimsSKNLoader.loadTheSimsBCF(File(".\\TheSims1Skin\\a2a-apologizer.cmx.bcf").readBytes())

        val bodySknVAO = createTheSimsSKNModelVAO(bodySkn, skeleton)
        val headSknVAO = createTheSimsSKNModelVAO(headSkn, skeleton)

        println("Triangle Count: ${cubeObj.vertices.size}")

        // Run the rendering loop until the user has attempted to close
        // the window or has pressed the ESCAPE key.
        var fortniteAnimationIdx = 0
        while (!glfwWindowShouldClose(window)) {
            glClear(GL43.GL_COLOR_BUFFER_BIT or GL43.GL_DEPTH_BUFFER_BIT)

            cameraRotationY += 0.4f
            updateViewMatrix()

            println("Frames: ${frames.size}")
            drawCube(programId, cubeObjVAO, texture.textureId, Vector3f(0f, 2f, 0f), false, cubeObj.faces.size * 9)
            val frame = (195 / 16) % 250

            println("Real frame: $frame")
            println(skeleton.skeletons.first().suits)
            drawTheSimsSKN(simsSknProgramId, bodySknVAO, bodyBitmapId.textureId, Vector3f(0f, 8f, 0f), bodySkn.vertices.size * 9, skeleton)
            drawTheSimsSKN(simsSknProgramId, headSknVAO, headBitmapId.textureId, Vector3f(0f, 8f, 0f), headSkn.vertices.size * 9, skeleton)
            // drawTheSimsSKN(simsSknProgramId, bodySknVAO, bodyBitmapId.textureId, Vector3f(0f, 8f, 0f), bodySkn.vertices.size * 9, skeleton, frames[((GLFW.glfwGetTime() / 0.033)).toInt() % frames.size]!!)
            // drawTheSimsSKN(simsSknProgramId, headSknVAO, headBitmapId.textureId, Vector3f(0f, 8f, 0f), headSkn.vertices.size * 9, skeleton, frames[((GLFW.glfwGetTime() / 0.033)).toInt() % frames.size]!!)

            glfwSwapBuffers(window) // swap the color buffers

            // Poll for window events. The key callback above will only be
            // invoked during this call.
            glfwPollEvents()

            fortniteAnimationIdx++
        }
    }

    // VAO = Vertex Array Object
    // VBO = Vertex Buffer Object
    private fun createDefaultCubeVAO(): Int {
        // A cube!
        val vertices = floatArrayOf(
            -1.0f,-1.0f,-1.0f, // triangle 1 : begin
            -1.0f,-1.0f, 1.0f,
            -1.0f, 1.0f, 1.0f, // triangle 1 : end
            1.0f, 1.0f,-1.0f, // triangle 2 : begin
            -1.0f,-1.0f,-1.0f,
            -1.0f, 1.0f,-1.0f, // triangle 2 : end
            1.0f,-1.0f, 1.0f,
            -1.0f,-1.0f,-1.0f,
            1.0f,-1.0f,-1.0f,
            1.0f, 1.0f,-1.0f,
            1.0f,-1.0f,-1.0f,
            -1.0f,-1.0f,-1.0f,
            -1.0f,-1.0f,-1.0f,
            -1.0f, 1.0f, 1.0f,
            -1.0f, 1.0f,-1.0f,
            1.0f,-1.0f, 1.0f,
            -1.0f,-1.0f, 1.0f,
            -1.0f,-1.0f,-1.0f,
            -1.0f, 1.0f, 1.0f,
            -1.0f,-1.0f, 1.0f,
            1.0f,-1.0f, 1.0f,
            1.0f, 1.0f, 1.0f,
            1.0f,-1.0f,-1.0f,
            1.0f, 1.0f,-1.0f,
            1.0f,-1.0f,-1.0f,
            1.0f, 1.0f, 1.0f,
            1.0f,-1.0f, 1.0f,
            1.0f, 1.0f, 1.0f,
            1.0f, 1.0f,-1.0f,
            -1.0f, 1.0f,-1.0f,
            1.0f, 1.0f, 1.0f,
            -1.0f, 1.0f,-1.0f,
            -1.0f, 1.0f, 1.0f,
            1.0f, 1.0f, 1.0f,
            -1.0f, 1.0f, 1.0f,
            1.0f,-1.0f, 1.0f
        )

        // Create and bind VAO
        val quadVAO = GL32.glGenVertexArrays()
        GL30.glBindVertexArray(quadVAO)

        // Generate two VBOs (one for the vertex positions, another for the colors)
        val vbosArray = IntArray(2)
        glGenBuffers(vbosArray)

        // Position VBO
        glBindBuffer(GL_ARRAY_BUFFER, vbosArray[0])
        glBufferData(GL_ARRAY_BUFFER, vertices, GL_STATIC_DRAW)
        // When reading pointers like this, think like this
        // The "size" is how much is the TARGET array that will be passed to the vertex shader
        // The "stride" is how much data WILL BE READ
        // The "pointer" is WHERE the data is in the ARRAY THAT WAS READ
        glVertexAttribPointer(0, 3, GL_FLOAT, false, 0, 0)
        glEnableVertexAttribArray(0)

        // Unbind
        glBindBuffer(GL_ARRAY_BUFFER, 0)
        GL30.glBindVertexArray(0)
        glDisableVertexAttribArray(0)

        return quadVAO
    }

    // VAO = Vertex Array Object
    // VBO = Vertex Buffer Object
    private fun createWavefrontOBJModelVAO(wavefrontOBJModel: WavefrontOBJModel): Int {
        // All values are initialized to zero
        val vertices = FloatArray(wavefrontOBJModel.faces.size * 9)
        val textureCoordinates = FloatArray(wavefrontOBJModel.faces.size * 9)
        val vertexIds = FloatArray(wavefrontOBJModel.faces.size * 3)
        val boneIds = FloatArray(wavefrontOBJModel.faces.size * 3)
        val faceColors = FloatArray(wavefrontOBJModel.faces.size * 9)

        var idx = 0
        var textureCoordinatesIdx = 0
        var vertexId = 0
        var faceIdx = 0
        var boneIdx = 0
        for (face in wavefrontOBJModel.faces) {
            println("Processing face $face - Total vertices: ${wavefrontOBJModel.vertices.size}")
            val baseIdx = idx

            val vertex0 = wavefrontOBJModel.vertices[face.v0.vertexIndex - 1]
            val vertex1 = wavefrontOBJModel.vertices[face.v1.vertexIndex - 1]
            val vertex2 = wavefrontOBJModel.vertices[face.v2.vertexIndex - 1]

            fun appendVertex(vertex: WavefrontOBJModel.Vertex) {
                vertices[idx++] = vertex.x
                vertices[idx++] = vertex.y
                vertices[idx++] = vertex.z
            }

            fun appendTexCoords(vertex: WavefrontOBJModel.TextureCoordinates?) {
                textureCoordinates[textureCoordinatesIdx++] = vertex?.u ?: 0f
                // The V value (vertical) corresponds to the Y-axis, but in most cases, it is flipped compared to image coordinate systems
                textureCoordinates[textureCoordinatesIdx++] = vertex?.v?.let { 1f - it } ?: 0f
                textureCoordinates[textureCoordinatesIdx++] = 0f
            }

            appendVertex(vertex0)
            appendVertex(vertex1)
            appendVertex(vertex2)

            appendTexCoords(face.v0.textureCoordinatesIndex?.let { wavefrontOBJModel.textureCoordinates[it - 1] })
            appendTexCoords(face.v1.textureCoordinatesIndex?.let { wavefrontOBJModel.textureCoordinates[it - 1] })
            appendTexCoords(face.v2.textureCoordinatesIndex?.let { wavefrontOBJModel.textureCoordinates[it - 1] })

            val vertexId0 = vertexId++
            val vertexId1 = vertexId++
            val vertexId2 = vertexId++

            vertexIds[vertexId0] = vertexId0.toFloat()
            vertexIds[vertexId1] = vertexId1.toFloat()
            vertexIds[vertexId2] = vertexId2.toFloat()

            faceColors[baseIdx] = Random.nextFloat()
            faceColors[baseIdx + 1] = Random.nextFloat()
            faceColors[baseIdx + 2] = Random.nextFloat()

            faceColors[baseIdx + 3] = Random.nextFloat()
            faceColors[baseIdx + 4] = Random.nextFloat()
            faceColors[baseIdx + 5] = Random.nextFloat()

            faceColors[baseIdx + 6] = Random.nextFloat()
            faceColors[baseIdx + 7] = Random.nextFloat()
            faceColors[baseIdx + 8] = Random.nextFloat()

            if (faceIdx in 24..36) {
                println("Face $faceIdx: Bone 2")
                boneIds[boneIdx++] = 2f
                boneIds[boneIdx++] = 2f
                boneIds[boneIdx++] = 2f
            } else if (faceIdx in 12..23) {
                println("Face $faceIdx: Bone 1")
                boneIds[boneIdx++] = 1f
                boneIds[boneIdx++] = 1f
                boneIds[boneIdx++] = 1f
            } else {
                println("Face $faceIdx: Bone 0")
                boneIds[boneIdx++] = 0f
                boneIds[boneIdx++] = 0f
                boneIds[boneIdx++] = 0f
            }
            faceIdx++
        }
        println("Total: $idx - Array Size: ${vertices.size} - Faces: ${wavefrontOBJModel.faces.size} - Vertex IDs: $vertexId")

        /* var idx2 = 0
        vertices.toList().chunked(3).forEach {
            println("$idx2: ${it.joinToString(", ")}")
            idx2++
        } */
        // Create and bind VAO
        val quadVAO = GL32.glGenVertexArrays()
        GL30.glBindVertexArray(quadVAO)

        // Generate two VBOs, one for the vertices another for the textcoords
        val vbosArray = IntArray(5)
        glGenBuffers(vbosArray)

        // Position VBO
        glEnableVertexAttribArray(0)
        glBindBuffer(GL_ARRAY_BUFFER, vbosArray[0])
        glBufferData(GL_ARRAY_BUFFER, vertices, GL_STATIC_DRAW)
        // When reading pointers like this, think like this
        // The "size" is how much is the TARGET array that will be passed to the vertex shader
        // The "stride" is how much data WILL BE READ
        // The "pointer" is WHERE the data is in the ARRAY THAT WAS READ
        glVertexAttribPointer(0, 3, GL_FLOAT, false, 0, 0)

        // Tex Coords VBO
        glEnableVertexAttribArray(1)
        glBindBuffer(GL_ARRAY_BUFFER, vbosArray[1])
        glBufferData(GL_ARRAY_BUFFER, textureCoordinates, GL_STATIC_DRAW)
        // When reading pointers like this, think like this
        // The "size" is how much is the TARGET array that will be passed to the vertex shader
        // The "stride" is how much data WILL BE READ
        // The "pointer" is WHERE the data is in the ARRAY THAT WAS READ
        glVertexAttribPointer(1, 3, GL_FLOAT, false, 0, 0)

        // Vertex ID
        // Tex Coords VBO
        glEnableVertexAttribArray(2)
        glBindBuffer(GL_ARRAY_BUFFER, vbosArray[2])
        glBufferData(GL_ARRAY_BUFFER, vertexIds, GL_STATIC_DRAW)
        // When reading pointers like this, think like this
        // The "size" is how much is the TARGET array that will be passed to the vertex shader
        // The "stride" is how much data WILL BE READ
        // The "pointer" is WHERE the data is in the ARRAY THAT WAS READ
        glVertexAttribPointer(2, 1, GL_FLOAT, false, 0, 0)

        // Bone ID
        // Tex Coords VBO
        glEnableVertexAttribArray(3)
        glBindBuffer(GL_ARRAY_BUFFER, vbosArray[3])
        glBufferData(GL_ARRAY_BUFFER, boneIds, GL_STATIC_DRAW)
        // When reading pointers like this, think like this
        // The "size" is how much is the TARGET array that will be passed to the vertex shader
        // The "stride" is how much data WILL BE READ
        // The "pointer" is WHERE the data is in the ARRAY THAT WAS READ
        glVertexAttribPointer(3, 1, GL_FLOAT, false, 0, 0)

        // Random Colors
        // Tex Coords VBO
        glEnableVertexAttribArray(4)
        glBindBuffer(GL_ARRAY_BUFFER, vbosArray[4])
        glBufferData(GL_ARRAY_BUFFER, faceColors, GL_STATIC_DRAW)
        // When reading pointers like this, think like this
        // The "size" is how much is the TARGET array that will be passed to the vertex shader
        // The "stride" is how much data WILL BE READ
        // The "pointer" is WHERE the data is in the ARRAY THAT WAS READ
        glVertexAttribPointer(4, 3, GL_FLOAT, false, 0, 0)

        // Unbind
        glBindBuffer(GL_ARRAY_BUFFER, 0)
        GL30.glBindVertexArray(0)
        glDisableVertexAttribArray(4)
        glDisableVertexAttribArray(3)
        glDisableVertexAttribArray(2)
        glDisableVertexAttribArray(1)
        glDisableVertexAttribArray(0)

        return quadVAO
    }

    // VAO = Vertex Array Object
    // VBO = Vertex Buffer Object
    private fun createTheSimsSKNModelVAO(sknModel: TheSimsSKNLoader.TheSimsSKN, cmx: TheSimsSKNLoader.TheSimsCMX): Int {
        val skeleton = cmx.skeletons.first()

        // All values are initialized to zero
        val vertices = FloatArray(sknModel.faces.size * 9)
        val uvCoordinates = FloatArray(sknModel.faces.size * 3 * 2)
        val vertexToBoneIds = FloatArray(sknModel.vertices.size * 9)

        println("Faces: ${sknModel.faces.size}")
        println("UV Coordinates: ${sknModel.textureCoordinates.size}")
        println("Vertices: ${sknModel.vertices.size}")

        run {
            var idx = 0

            var totalUvCoordinates = 0
            var uvCoordinatesIdx = 0
            var boneIdx = 0
            val lArmIndex = sknModel.bones.indexOf("PELVIS")
            println("lArmIndex: $lArmIndex")
            // val binding = sknModel.boneBindings.first { it.boneIndex == lArmIndex }
            // println("Vert Range is $vertRange")

            for (face in sknModel.faces) {
                fun appendVertex(index: Int) {
                    val vertex = sknModel.vertices[index]
                    val texturesCoordinates = sknModel.textureCoordinates[index]

                    uvCoordinates[uvCoordinatesIdx++] = texturesCoordinates.u
                    uvCoordinates[uvCoordinatesIdx++] = texturesCoordinates.v

                    vertices[idx++] = vertex.x
                    vertices[idx++] = vertex.y
                    vertices[idx++] = vertex.z

                    // THIS IS CORRECT ACTUALLY
                    val bindings = sknModel.boneBindings.filter { index in it.firstVert until (it.firstVert + it.vertCount) }
                    if (bindings.size != 1)
                        error("Invalid Binding Size! ${bindings.size}")

                    val binding = bindings.first()

                    // We need to convert the bone index to the skeleton bone index
                    val boneName = sknModel.bones[binding.boneIndex]

                    val skeletonBoneIndex = skeleton.suits.indexOfFirst { it.boneName == boneName }

                    vertexToBoneIds[boneIdx++] = skeletonBoneIndex.toFloat()
                }

                appendVertex(face.vertex0Index)
                appendVertex(face.vertex1Index)
                appendVertex(face.vertex2Index)
            }

            println("IDX: $idx")
            println("UV IDX: $uvCoordinatesIdx")
        }

        vertices.toList().chunked(3).forEachIndexed { index, it ->
            // println("Vertex $index: ${it.joinToString(", ")}")
        }

        uvCoordinates.toList().chunked(2).forEachIndexed { index, it ->
            // println("UV Coordinates $index: ${it.joinToString(", ")}")
        }

        /* var idx2 = 0
        vertices.toList().chunked(3).forEach {
            println("$idx2: ${it.joinToString(", ")}")
            idx2++
        } */
        // Create and bind VAO
        val quadVAO = GL32.glGenVertexArrays()
        GL30.glBindVertexArray(quadVAO)

        // Generate two VBOs, one for the vertices another for the textcoords
        val vbosArray = IntArray(3)
        glGenBuffers(vbosArray)

        // Position VBO
        glEnableVertexAttribArray(0)
        glBindBuffer(GL_ARRAY_BUFFER, vbosArray[0])
        glBufferData(GL_ARRAY_BUFFER, vertices, GL_STATIC_DRAW)
        // When reading pointers like this, think like this
        // The "size" is how much is the TARGET array that will be passed to the vertex shader
        // The "stride" is how much data WILL BE READ
        // The "pointer" is WHERE the data is in the ARRAY THAT WAS READ
        glVertexAttribPointer(0, 3, GL_FLOAT, false, 0, 0)

        // Tex Coords VBO
        glEnableVertexAttribArray(1)
        glBindBuffer(GL_ARRAY_BUFFER, vbosArray[1])
        glBufferData(GL_ARRAY_BUFFER, uvCoordinates, GL_STATIC_DRAW)
        // When reading pointers like this, think like this
        // The "size" is how much is the TARGET array that will be passed to the vertex shader
        // The "stride" is how much data WILL BE READ
        // The "pointer" is WHERE the data is in the ARRAY THAT WAS READ
        glVertexAttribPointer(1, 2, GL_FLOAT, false, 0, 0)

        // Bone ID VBO
        glEnableVertexAttribArray(2)
        glBindBuffer(GL_ARRAY_BUFFER, vbosArray[2])
        glBufferData(GL_ARRAY_BUFFER, vertexToBoneIds, GL_STATIC_DRAW)
        // When reading pointers like this, think like this
        // The "size" is how much is the TARGET array that will be passed to the vertex shader
        // The "stride" is how much data WILL BE READ
        // The "pointer" is WHERE the data is in the ARRAY THAT WAS READ
        glVertexAttribPointer(2, 1, GL_FLOAT, false, 0, 0)

        // Unbind
        glBindBuffer(GL_ARRAY_BUFFER, 0)
        GL30.glBindVertexArray(0)
        glDisableVertexAttribArray(2)
        glDisableVertexAttribArray(1)
        glDisableVertexAttribArray(0)

        return quadVAO
    }

    fun drawCube(programId: Int, quadVAO: Int, textureId: Int, position: Vector3f, isActive: Boolean, triangleCount: Int) {
        glUseProgram(programId)

        glActiveTexture(GL_TEXTURE0)
        glBindTexture(GL_TEXTURE_2D, textureId)

        // val location = glGetUniformLocation(programId, "MVP")
        val modelLocation = glGetUniformLocation(programId, "model")
        val viewLocation = glGetUniformLocation(programId, "view")
        val projectionLocation = glGetUniformLocation(programId, "projection")
        val isActiveLocation = glGetUniformLocation(programId, "isActive")
        val timeLocation = glGetUniformLocation(programId, "time")
        val cameraPositionLocation = glGetUniformLocation(programId, "cameraPos")
        val boneMatricesLocation = glGetUniformLocation(programId, "boneMatrices")

        // Model matrix: where the mesh is in the world
        val model = Matrix4f()
            .translate(position)

        // The bones should ALWAYS be filled with the identity matrix if they aren't posed
        val stuff = FloatArray(48)
        Matrix4f().translate(0f, 0f, 0f).get(stuff, 0)

        val parentBone = Matrix4f()
            .rotateY(GLFW.glfwGetTime().toFloat())
            .translate(0f, 1f, 0f)

        parentBone.get(stuff, 16)

        Matrix4f(parentBone).translate(0f, Math.sin(GLFW.glfwGetTime()).toFloat(), 0f).get(stuff, 32)

        glUniformMatrix4fv(boneMatricesLocation, false, stuff)

        // Our ModelViewProjection: multiplication of our 3 matrices
        // val mvp = projection.mul(view, Matrix4f()).mul(model, Matrix4f()) // Remember, matrix multiplication is the other way around

        glUniformMatrix4fv(modelLocation, false, model.get(FloatArray(16)))
        glUniformMatrix4fv(viewLocation, false, view.get(FloatArray(16)))
        glUniformMatrix4fv(projectionLocation, false, projection.get(FloatArray(16)))
        glUniform1i(isActiveLocation, if (isActive) 1 else 0)
        glUniform1f(timeLocation, GLFW.glfwGetTime().toFloat())

        GL30.glBindVertexArray(quadVAO)
        glDrawArrays(GL_TRIANGLES, 0, triangleCount)
        GL30.glBindVertexArray(0)
    }

    fun drawTheSimsSKN(programId: Int, quadVAO: Int, textureId: Int, position: Vector3f, triangleCount: Int, cmx: TheSimsSKNLoader.TheSimsCMX) {
        glUseProgram(programId)

        glActiveTexture(GL_TEXTURE0)
        glBindTexture(GL_TEXTURE_2D, textureId)

        // val location = glGetUniformLocation(programId, "MVP")
        val modelLocation = glGetUniformLocation(programId, "model")
        val viewLocation = glGetUniformLocation(programId, "view")
        val projectionLocation = glGetUniformLocation(programId, "projection")
        val boneMatricesLocation = glGetUniformLocation(programId, "boneMatrices")

        // The bones should ALWAYS be filled with the identity matrix if they aren't posed
        val stuff = FloatArray(16 * cmx.skeletons.first().suits.size)

        val bones = mutableMapOf<String, Matrix4f>()
        bones["NULL"] = Matrix4f()
            .scale(1f, 1f, -1f)
            .translate(0f, -8f, 0f)
            // .rotateZ(Math.toRadians(90.0).toFloat())

        repeat(cmx.skeletons.first().suits.size) {
            val suit = cmx.skeletons.first().suits[it]
            println("Processing suit ${suit.boneName} ($it)")
            val parentBone = bones[suit.parentBone] ?: error("Missing parent bone for ${suit.boneName}! Parent Bone is ${suit.parentBone}")

            println("Suit ${suit.boneName} attaches to parent ${suit.parentBone}")
            // intentionally inverted
            val thisBone = Matrix4f(parentBone)
            thisBone
                .apply {
                    translate(suit.position)
                    // I'm not sure why do we need to invert
                    // Milkshape 3D's skeleton shows up "correctly", but inverting also looks like a correct skeleton?!
                    // I think that Milkshape 3D is actually WRONG because this skeleton actually looks more correct than the one by MS3D
                    // After all, why would the toe bones be, by default, BELOW the feet?
                    rotate(Quaternionf(suit.rotation).invert())
                }
                .get(stuff, 16 * it)

            bones[suit.boneName] = thisBone
        }

        // Model matrix: where the mesh is in the world
        val model = Matrix4f()
            .translate(position)

        // Our ModelViewProjection: multiplication of our 3 matrices
        // val mvp = projection.mul(view, Matrix4f()).mul(model, Matrix4f()) // Remember, matrix multiplication is the other way around

        glUniformMatrix4fv(modelLocation, false, model.get(FloatArray(16)))
        glUniformMatrix4fv(viewLocation, false, view.get(FloatArray(16)))
        glUniformMatrix4fv(projectionLocation, false, projection.get(FloatArray(16)))
        glUniformMatrix4fv(boneMatricesLocation, false, stuff)
        // glUniform1i(isActiveLocation, if (isActive) 1 else 0)
        // glUniform1f(timeLocation, GLFW.glfwGetTime().toFloat())

        GL30.glBindVertexArray(quadVAO)
        glDrawArrays(GL_TRIANGLES, 0, triangleCount)
        GL30.glBindVertexArray(0)
    }

    fun drawTheSimsSKN(programId: Int, quadVAO: Int, textureId: Int, position: Vector3f, triangleCount: Int, cmx: TheSimsSKNLoader.TheSimsCMX, frameBones: List<TheSimsSKNLoader.FrameWrapper>) {
        for (frameBones in frameBones) {
            println(frameBones.boneName)
            println("Pos: ${frameBones.position.x}, ${frameBones.position.y}, ${frameBones.position.z}")
            println("Rot: ${frameBones.rotation.x}, ${frameBones.rotation.y}, ${frameBones.rotation.z}, ${frameBones.rotation.w}")
        }

        glUseProgram(programId)

        glActiveTexture(GL_TEXTURE0)
        glBindTexture(GL_TEXTURE_2D, textureId)

        // val location = glGetUniformLocation(programId, "MVP")
        val modelLocation = glGetUniformLocation(programId, "model")
        val viewLocation = glGetUniformLocation(programId, "view")
        val projectionLocation = glGetUniformLocation(programId, "projection")
        val boneMatricesLocation = glGetUniformLocation(programId, "boneMatrices")

        // The bones should ALWAYS be filled with the identity matrix if they aren't posed
        val stuff = FloatArray(16 * cmx.skeletons.first().suits.size)

        val bones = mutableMapOf<String, Matrix4f>()
        bones["NULL"] = Matrix4f()
            .scale(1f, 1f, -1f)
            .translate(0f, -8f, 0f)
        // .rotateZ(Math.toRadians(-180.0).toFloat())


        repeat(cmx.skeletons.first().suits.size) {
            val suit = cmx.skeletons.first().suits[it]

            val parentBone = bones[suit.parentBone] ?: error("Missing parent bone for ${suit.boneName}! Parent Bone is ${suit.parentBone}")

            // intentionally inverted
            val thisBone = Matrix4f(parentBone)
            thisBone
                // .translate(suit.position.x, suit.position.y, suit.position.z)
                // .rotate(suit.rotation)
                .apply {
                    var boneTarget = suit.boneName
                    val fw = frameBones.first { it.boneName == boneTarget }

                    translate(suit.position)
                    translate(fw.position)
                    // rotate(suit.rotation)

                    rotate(Quaternionf(fw.rotation.x, fw.rotation.y, fw.rotation.z, fw.rotation.w).invert())
                }
                .get(stuff, 16 * it)

            bones[suit.boneName] = thisBone
        }

        // Model matrix: where the mesh is in the world
        val model = Matrix4f()
            .translate(position)

        // Our ModelViewProjection: multiplication of our 3 matrices
        // val mvp = projection.mul(view, Matrix4f()).mul(model, Matrix4f()) // Remember, matrix multiplication is the other way around

        glUniformMatrix4fv(modelLocation, false, model.get(FloatArray(16)))
        glUniformMatrix4fv(viewLocation, false, view.get(FloatArray(16)))
        glUniformMatrix4fv(projectionLocation, false, projection.get(FloatArray(16)))
        glUniformMatrix4fv(boneMatricesLocation, false, stuff)
        // glUniform1i(isActiveLocation, if (isActive) 1 else 0)
        // glUniform1f(timeLocation, GLFW.glfwGetTime().toFloat())

        GL30.glBindVertexArray(quadVAO)
        glDrawArrays(GL_TRIANGLES, 0, triangleCount)
        GL30.glBindVertexArray(0)
    }

    /**
     * Updates the current projection matrix
     */
    private fun updateProjectionMatrix() {
        // We have a function to update the view matrix because we can switch the projection during runtime
        val projection = Matrix4f()

        if (this.useOrthographicProjection) {
            // We don't use the window width/height because that changes the aspect ratio of the projection and that looks a bit wonky
            projection.ortho(-4f, 4f, -2.25f, 2.25f, 0.5f, 10000.0f)
        } else {
            // Projection matrix: 45° Field of View, 4:3 ratio, display range: 0.1 unit <-> 100 units
            projection.perspective(Math.toRadians(50.0).toFloat(), windowWidth.toFloat() / windowHeight.toFloat(), 0.1f, 100.0f)
        }

        this.projection = projection
    }

    /**
     * Updates the current view matrix
     */
    private fun updateViewMatrix() {
        // We have a function to update the view matrix due to the camera rotation
        this.cameraPosition = Vector3f(-12f, 8f, 0f) // Camera is at (4,3,3), in World Space
            .rotateY(Math.toRadians(this.cameraRotationY.toDouble()).toFloat())

        // println("Camera Position: ${this.cameraPosition.x}, ${this.cameraPosition.y}, ${this.cameraPosition.z}")

        this.view = Matrix4f().lookAt(
            this.cameraPosition,
            Vector3f(0f, 4f, 0f),
            Vector3f(0f, 1f, 0f) // Head is up (set to 0,-1,0 to look upside-down)
        )
    }

    private fun loadWavefrontMTL(content: String): WavefrontMTL {
        class MaterialBuilder(val materialName: String, var diffuseTextureMap: String? = null)

        val materials = mutableMapOf<String, WavefrontMTL.Material>()
        var activeMaterial: MaterialBuilder? = null

        fun createFromActiveMaterial() {
            val activeMaterial = activeMaterial

            if (activeMaterial != null) {
                materials[activeMaterial.materialName] = WavefrontMTL.Material(activeMaterial.diffuseTextureMap)
            }
        }

        for (line in content.lines()) {
            try {
                // A comment
                if (line.startsWith("#"))
                    continue

                if (line.startsWith("newmtl ")) {
                    createFromActiveMaterial()

                    val elements = line.split(" ").drop(1).filter { it.isNotBlank() }

                    val materialName = elements[0]

                    materials[materialName] = WavefrontMTL.Material(materialName)
                    activeMaterial = MaterialBuilder(materialName)
                }

                if (line.startsWith("map_Kd ")) {
                    val elements = line.split(" ").drop(1).filter { it.isNotBlank() }

                    val fileName = elements[0]

                    activeMaterial!!.diffuseTextureMap = fileName
                }
            } catch (e: Exception) {
                println("Failed on line $line")
                throw RuntimeException(e)
            }
        }

        createFromActiveMaterial()

        return WavefrontMTL(materials)
    }
}