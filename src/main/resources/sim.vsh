#version 430 core

layout (location = 0) in vec3 aPos;
layout (location = 1) in vec2 TexCoords;
layout (location = 2) in float BoneId; // Which bone are we affected by, currently we will only support ONE SINGULAR BONE

// Values that stay constant for the whole mesh.
uniform mat4 model;
uniform mat4 view;
uniform mat4 projection;

out vec2 FragTexCoords;

uniform mat4 boneMatrices[29];

void main() {
    FragTexCoords = TexCoords;

    // Initialize transformed position
    vec4 transformedPos = vec4(0.0);

    // Apply bone transformations
    mat4 boneTransform = boneMatrices[int(BoneId)]; // Convert float to int

    transformedPos += (boneTransform * vec4(aPos, 1.0));

    mat4 mvp = projection * view * model;

    gl_Position = mvp * vec4(transformedPos.x, transformedPos.y, transformedPos.z, 1.0);
}