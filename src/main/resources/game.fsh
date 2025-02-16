#version 330 core

in vec2 FragTexCoords;
// in vec3 FragRandomColor;
out vec4 FragColor;

uniform vec3 cameraPos; // Camera position in world space
uniform sampler2D image;

void main()
{
    // TODO: Use texture!
    // FragColor = vec4(FragRandomColor.x, FragRandomColor.y, FragRandomColor.z, 1.0f);
    FragColor = texture(image, FragTexCoords);
}