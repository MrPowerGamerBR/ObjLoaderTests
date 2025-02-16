package net.perfectdreams.objloadertests

import org.joml.Matrix4f
import org.joml.Quaternionf
import org.joml.Vector3f
import java.io.DataInputStream
import java.io.File
import java.lang.Math.pow
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.abs
import kotlin.math.pow

object TheSimsSKNLoader {
    fun loadTheSimsSKN(content: String): TheSimsSKN {
        val lines = content.lines().iterator()
        val sknFileName = lines.next()
        val bitmapFileName = lines.next()
        val boneCount = lines.next().toInt()
        val bones = mutableListOf<String>()

        repeat(boneCount) {
            bones.add(lines.next())
        }

        val faceCount = lines.next().toInt()
        val faces = mutableListOf<TheSimsSKN.Face>()

        repeat(faceCount) {
            val (v0, v1, v2) = lines.next().split(" ").map { it.toInt() }
            faces.add(TheSimsSKN.Face(v0, v1, v2))
        }

        val boneBindingsCount = lines.next().toInt()
        val boneBindings = mutableListOf<TheSimsSKN.BoneBindings>()

        repeat(boneBindingsCount) {
            val (v0, v1, v2, v3, v4) = lines.next().split(" ").map { it.toInt() }
            boneBindings.add(TheSimsSKN.BoneBindings(v0, v1, v2, v3, v4))
        }

        val textureCoordinatesCount = lines.next().toInt()
        val textureCoordinates = mutableListOf<TheSimsSKN.TextureCoordinates>()

        repeat(textureCoordinatesCount) {
            val (u, v) = lines.next().split(" ").map { it.toFloat() }
            textureCoordinates.add(TheSimsSKN.TextureCoordinates(u, v))
        }

        val blendDataCount = lines.next().toInt()

        repeat(blendDataCount) {
            lines.next()
        }

        val verticesCount = lines.next().toInt()
        val vertices = mutableListOf<TheSimsSKN.Vertex>()

        repeat(verticesCount) {
            val result = lines.next().split(" ").map { it.toFloat() }

            vertices.add(
                TheSimsSKN.Vertex(
                    result[0],
                    result[1],
                    result[2],

                    result[3],
                    result[4],
                    result[5]
                )
            )
        }

        return TheSimsSKN(
            sknFileName,
            bitmapFileName,
            bones,
            faces,
            boneBindings,
            textureCoordinates,
            vertices
        )
    }

    fun loadTheSimsCMX(content: String): TheSimsCMX {
        val lines = content.lines().iterator()
        val comment = lines.next()
        val version = lines.next()
        val skeletonCount = lines.next().toInt()
        val skeletons = mutableListOf<TheSimsCMX.Skeleton>()

        repeat(skeletonCount) {
            val type = lines.next()
            val suitCount = lines.next().toInt()

            val suits = mutableListOf<TheSimsCMX.Suit>()

            repeat(suitCount) {
                println("Parsing...")
                val boneName = lines.next()
                println(boneName)
                val parentBone = lines.next()
                val propCount = lines.next().toInt()
                repeat(propCount) {
                    val arrayElements = lines.next().toInt()
                    repeat(arrayElements) {
                        val name = lines.next()
                        val type = lines.next()
                    }
                }
                val xyz = lines.next().removePrefix("| ").removeSuffix(" |").trim().split(" ").map { it.toFloat() }
                val rotationQuaternion =
                    lines.next().removePrefix("| ").removeSuffix(" |").trim().split(" ").map { it.toFloat() }
                val canTranslate = lines.next().toInt() == 1
                val canRotate = lines.next().toInt() == 1
                val canBend = lines.next().toInt() == 1
                val wiggleValue = lines.next()
                val wigglePower = lines.next()

                println(boneName)
                println(parentBone)

                suits.add(
                    TheSimsCMX.Suit(
                        boneName,
                        parentBone,
                        Vector3f(xyz[0], xyz[1], xyz[2]),
                        Quaternionf(
                            rotationQuaternion[0],
                            rotationQuaternion[1],
                            rotationQuaternion[2],
                            rotationQuaternion[3]
                        )
                    )
                )
            }

            skeletons.add(TheSimsCMX.Skeleton(suits))
        }

        // TODO: Suits
        // TODO: Skills

        return TheSimsCMX(version, skeletons, listOf())
    }

    fun loadTheSimsBCF(content: ByteArray): Pair<TheSimsCMX, MutableMap<Int, MutableList<FrameWrapper>>> {
        val buffer = ByteBuffer.wrap(content)
        buffer.order(ByteOrder.LITTLE_ENDIAN)

        val skeletonCount = buffer.getInt()
        val skeletons = mutableListOf<TheSimsCMX.Skeleton>()

        // TODO: Skeletons
        val suitCount = buffer.getInt()
        // TODO: Suits

        val skillCount = buffer.getInt()

        val skills = mutableListOf<TheSimsCMX.Skill>()

        repeat(skillCount) {
            val skillName = buffer.getPascalString()
            val animationName = buffer.getPascalString()
            println("skillName: $skillName")
            println("animationName: $animationName")

            val duration = buffer.getFloat()
            val distance = buffer.getFloat()
            val movingFlag = buffer.getInt()
            val positionCount = buffer.getInt().toUInt()
            val rotationCount = buffer.getInt().toUInt()

            println("duration: $duration; distance: $distance; movingFlag: $movingFlag; positionCount: $positionCount; rotationCount: $rotationCount")
            val motionCount = buffer.getInt()

            println("motionCount: $motionCount")

            val motions = mutableListOf<TheSimsCMX.Motion>()

            repeat(motionCount) {
                val boneName = buffer.getPascalString()
                println(boneName)
                val frameCount = buffer.getInt()
                val duration = buffer.getFloat()
                val positionsUsedFlag = buffer.getInt()
                val rotationsUsedFlag = buffer.getInt()
                val positionOffset = buffer.getInt()
                val rotationOffset = buffer.getInt()

                println("Frame Count: $frameCount; Duration: $duration; positionsUsedFlag: $positionsUsedFlag; rotationsUsedFlag: $rotationsUsedFlag; positionOffset: $positionOffset; rotationOffset: $rotationOffset")

                val propertyCount = buffer.getInt()
                println("prop count: $propertyCount")
                val timePropertyCount = buffer.getInt()
                println("time prop count: $timePropertyCount")

                repeat(timePropertyCount) {
                    val count = buffer.getInt()

                    repeat(count) {
                        val time = buffer.getInt()
                        println("time: $time")
                        val count = buffer.getInt()

                        repeat(count) {
                            val name = buffer.getPascalString()
                            val value = buffer.getPascalString()

                            println("Name: $name")
                            println("Name: $value")
                        }
                    }
                }

                motions.add(
                    TheSimsCMX.Motion(
                        boneName,
                        frameCount,
                        duration,
                        positionsUsedFlag == 1,
                        rotationsUsedFlag == 1,
                        positionOffset,
                        rotationOffset,
                    )
                )
            }

            skills.add(
                TheSimsCMX.Skill(
                    skillName,
                    animationName,
                    duration,
                    distance,
                    movingFlag,
                    positionCount,
                    rotationCount,
                    motions
                )
            )
        }

        // Do the rest
        val skill = skills.first { it.skillName == "a2a-apologizer" }

        // Attempt to parse CFP
        val floats = loadTheSimsCFP(
            File(".\\TheSims1Skin\\xskill-a2a-apologizer.cfp").readBytes(),
            skill.positionCount.toInt(),
            skill.rotationCount.toInt()
        )

        val positionsX = floats.subList(0, skill.positionCount.toInt())
        val positionsY = floats.subList(skill.positionCount.toInt(), skill.positionCount.toInt() * 2)
        val positionsZ = floats.subList(skill.positionCount.toInt() * 2, skill.positionCount.toInt() * 3)

        val rotationOffset = skill.positionCount.toInt() * 3


        // TODO: I'm not actually sure if this is correct
        // https://github.com/mixiate/ts1-blender-io/blob/main/addons/io_scene_ts1/cfp.py the first field is rotationsX
        // https://web.archive.org/web/20050501050322/http://simtech.sourceforge.net/tech/cfp.html the first field is rotationsW
        // I *think* that ts1-blender-io is correct because the animation does look fine like this
        val rotationsX = floats.subList(rotationOffset, rotationOffset + skill.rotationCount.toInt())
        val rotationsY = floats.subList(rotationOffset + skill.rotationCount.toInt(), rotationOffset + (skill.rotationCount.toInt() * 2))
        val rotationsZ = floats.subList(rotationOffset + (skill.rotationCount.toInt() * 2), rotationOffset + (skill.rotationCount.toInt() * 3))
        val rotationsW = floats.subList(rotationOffset + (skill.rotationCount.toInt() * 3), rotationOffset + (skill.rotationCount.toInt() * 4))

        println("Probably all offsets: ${rotationOffset + (skill.rotationCount.toInt() * 4)}")
        println("Floats: ${floats.size}")

        for (motion in skill.motions) {
            println(motion.boneName)
            println(motion.frameCount)

            // Pass it along
            if (motion.positionsUsedFlag) {
                val motionPosX = positionsX.subList(motion.positionOffset, motion.positionOffset + motion.frameCount)
                val motionPosY = positionsY.subList(motion.positionOffset, motion.positionOffset + motion.frameCount)
                val motionPosZ = positionsZ.subList(motion.positionOffset, motion.positionOffset + motion.frameCount)
            }

            if (motion.rotationsUsedFlag) {
                val motionRotX = rotationsX.subList(motion.rotationOffset, motion.rotationOffset + motion.frameCount)
                val motionRotY = rotationsY.subList(motion.rotationOffset, motion.rotationOffset + motion.frameCount)
                val motionRotZ = rotationsZ.subList(motion.rotationOffset, motion.rotationOffset + motion.frameCount)
                val motionRotW = rotationsW.subList(motion.rotationOffset, motion.rotationOffset + motion.frameCount)
            }
        }

        val frameWrappers = mutableMapOf<Int, MutableList<FrameWrapper>>()

        for (motion in skill.motions) {
            println(motion.boneName)
            println(motion.frameCount)

            repeat(motion.frameCount) {
                // Pass it along
                val pos = Vector3f()
                val rot = Quaternionf()

                if (motion.positionsUsedFlag) {
                    val motionPosX = positionsX[it + motion.positionOffset]
                    val motionPosY = positionsY[it + motion.positionOffset]
                    val motionPosZ = positionsZ[it + motion.positionOffset]

                    pos.set(motionPosX, motionPosY, motionPosZ)
                }

                if (motion.rotationsUsedFlag) {
                    val motionRotX = rotationsX[it + motion.rotationOffset]
                    val motionRotY = rotationsY[it + motion.rotationOffset]
                    val motionRotZ = rotationsZ[it + motion.rotationOffset]
                    val motionRotW = rotationsW[it + motion.rotationOffset]

                    println("Motions: $motionRotX, $motionRotY, $motionRotZ, $motionRotW")
                    rot.set(motionRotX, motionRotY, motionRotZ, motionRotW)
                }

                frameWrappers.getOrPut(it) { mutableListOf() }.add(
                    FrameWrapper(
                        motion.boneName,
                        pos,
                        rot
                    )
                )
            }
        }

        return Pair(TheSimsCMX("version 300", skeletons, skills), frameWrappers)
    }

    fun loadTheSimsCFP(content: ByteArray, positionCount: Int, rotationCount: Int): List<Float> {
        // position * 3 = xyz
        // rotation * 4 = xyzw
        val count = (positionCount * 3) + (rotationCount * 4)

        val buffer = ByteBuffer.wrap(content)
        buffer.order(ByteOrder.LITTLE_ENDIAN)

        val values = mutableListOf<Float>()
        var previousValue = 0.0f

        println("Expected count: $count")
        while (values.size != count) {
            println("Values: ${values.size}; Expected count: $count; ByteArray size: ${content.size}")

            val type = buffer.get().toUByte()
            when (type) {
                0xFF.toUByte() -> {
                    println("hewwo")
                    val float = buffer.getFloat()
                    println(float)
                    values.add(float)
                }

                0xFE.toUByte() -> {
                    val repeatCount = buffer.getShort().toUShort()
                    println("repeat count: $repeatCount")
                    // YES THE REPEAT COUNT +1 IS REQUIRED
                    repeat(repeatCount.toInt() + 1) {
                        values.add(previousValue)
                    }
                }

                else -> {
                    println("Delta $type")
                    values.add(previousValue + decodeDelta(type.toInt()))
                }
            }

            previousValue = values.last()
        }

        if (buffer.hasRemaining())
            error("Didn't read the entire file!")

        return values
    }

    /**
     * Decode a compressed delta to it's float value.
     */
    fun decodeDelta(delta: Int): Float {
        return 3.9676e-10f * (delta.toDouble() - 126.0).pow(3.0).toFloat() * abs(delta.toFloat() - 126f)
    }

    private fun ByteBuffer.getPascalString(): String {
        val length = this.get().toInt()
        val byteArray = ByteArray(length)
        this.get(byteArray, 0, length)
        return byteArray.toString(Charsets.US_ASCII)
    }

    // https://web.archive.org/web/20050502112039/http://simtech.sourceforge.net/tech/file_formats_skn.htm
    data class TheSimsSKN(
        val sknFileName: String, // unused
        val bitmapFileName: String,
        val bones: List<String>,
        val faces: List<Face>,
        val boneBindings: List<BoneBindings>,
        val textureCoordinates: List<TextureCoordinates>,
        val vertices: List<Vertex>
    ) {
        data class Face(
            val vertex0Index: Int,
            val vertex1Index: Int,
            val vertex2Index: Int
        )

        data class BoneBindings(
            val boneIndex: Int,
            val firstVert: Int,
            val vertCount: Int,
            val firstBlendedVert: Int,
            val blendedVertCount: Int
        )

        data class TextureCoordinates(
            val u: Float,
            val v: Float
        )

        data class Vertex(
            val x: Float,
            val y: Float,
            val z: Float,
            val normalX: Float,
            val normalY: Float,
            val normalZ: Float
        )
    }

    data class TheSimsCMX(
        val version: String,
        val skeletons: List<Skeleton>,
        val skills: List<Skill>
    ) {
        data class Skeleton(
            val suits: List<Suit>
        )

        data class Suit(
            val boneName: String,
            val parentBone: String,
            val position: Vector3f,
            val rotation: Quaternionf
        )

        data class Skill(
            val skillName: String,
            val animationName: String,
            val duration: Float,
            val distance: Float,
            val movingFlag: Int,
            val positionCount: UInt,
            val rotationCount: UInt,
            val motions: List<Motion>
        )

        data class Motion(
            val boneName: String,
            val frameCount: Int,
            val duration: Float,
            val positionsUsedFlag: Boolean,
            val rotationsUsedFlag: Boolean,
            val positionOffset: Int,
            val rotationOffset: Int
        )
    }

    data class FrameWrapper(
        val boneName: String,
        val position: Vector3f,
        val rotation: Quaternionf
    )
}

fun main() {
    if (true) {
        TheSimsSKNLoader.loadTheSimsBCF(File(".\\TheSims1Skin\\a2a-apologizer.cmx.bcf").readBytes())
        return
    }

    if (true) {
        TheSimsSKNLoader.loadTheSimsCMX(
            File(".\\TheSims1Skin\\adult-skeleton.cmx")
                .readText()
        ).let { println(it) }
        return
    }

    val skn = TheSimsSKNLoader.loadTheSimsSKN(
        File(".\\TheSims1Skin\\xskin-B899FAFit_eliseSSX-PELVIS-MBODY.skn")
            .readText()
    )

    println(skn)
}