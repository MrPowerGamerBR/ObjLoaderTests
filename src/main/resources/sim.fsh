#version 430 core

in vec2 FragTexCoords;
in float FragBoneId;
out vec4 FragColor;

uniform sampler2D image;

void main() {
    // if (FragBoneId == 20.0f) {
    //     FragColor = vec4(0.0f, 1.0f, 1.0f, 1.0f);
    //     return;
    // }

    FragColor = texture(image, FragTexCoords);
}