#version 430 core

in vec2 FragTexCoords;
out vec4 FragColor;

uniform sampler2D image;

void main() {
    // TODO: Use texture!
    // FragColor = vec4(1.0f, 1.0f, 1.0f, 1.0f);
    FragColor = texture(image, FragTexCoords);
}