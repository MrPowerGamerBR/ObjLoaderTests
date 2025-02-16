#version 330 core

layout (location = 0) in vec3 aPos;
layout (location = 1) in vec2 TexCoords;
layout (location = 2) in float VertexId;
layout (location = 3) in float BoneId; // Which bone are we affected by, currently we will only support ONE SINGULAR BONE
layout (location = 4) in vec3 RandomColor;

// Values that stay constant for the whole mesh.
uniform mat4 model;
uniform mat4 view;
uniform mat4 projection;

// uniform mat4 boneMatrices[3]; // Max 2 bones (JUST FOR FUN!!)

// out vec3 fragPos; // Pass the vertex position to the fragment shader
out vec2 FragTexCoords;
// out vec3 FragRandomColor;

void main() {
    // FragRandomColor = RandomColor;
    FragTexCoords = TexCoords;

    // Initialize transformed position
    // vec4 transformedPos = vec4(0.0);

    // Apply bone transformations
    // int boneIndex = int(BoneId);  // Convert float to int
    // mat4 boneTransform = boneMatrices[boneIndex];

    // transformedPos += (boneTransform * vec4(aPos, 1.0));

    // fragPos = vec3(model * vec4(aPos, 1.0));  // Calculate the vertex position in world space
    mat4 mvp = projection * view * model;

    gl_Position = mvp * vec4(aPos.x, aPos.y, aPos.z, 1.0);
}