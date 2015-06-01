//uniform mat4 u_Matrix;
//uniform vec3 u_VectorToLight;
//attribute vec3 a_Normal;
//attribute vec3 a_Position;
//varying vec3 v_Color;
//void main() {
//    v_Color = mix(vec3(0.180, 0.467, 0.153),
//                  vec3(0.660, 0.670, 0.680),
//                  a_Position.y);
//    vec3 scaledNormal = a_Normal;
//    scaledNormal.y *= 10.0;
//    scaledNormal = normalize(scaledNormal);
//    float diffuse = max(dot(scaledNormal, u_VectorToLight), 0.0);
//    v_Color *= diffuse;
//    float ambient = 0.2;
//    v_Color += ambient;
//    gl_Position = u_Matrix * vec4(a_Position, 1.0);
//}
uniform mat4 u_MVMatrix;
uniform mat4 u_IT_MVMatrix;
uniform mat4 u_MVPMatrix;

uniform vec3 u_VectorToLight;   // In eye space
uniform vec4 u_PointLightPositions[3];  // In eye space
uniform vec3 u_PointLightColors[3];

attribute vec4 a_Position;
attribute vec3 a_Normal;

varying vec3 v_Color;

vec3 materialColor;
vec4 eyeSpacePosition;
vec3 eyeSpaceNormal;

vec3 getAmbientLighting();
vec3 getDirectionalLighting();
vec3 getPointLighting();

void main() {
    materialColor = mix(vec3(0.180, 0.467, 0.153),
                        vec3(0.660, 0.670, 0.680),
                        a_Position.y);
    eyeSpacePosition = u_MVMatrix * a_Position;

    eyeSpaceNormal = normalize(vec3(u_IT_MVMatrix * vec4(a_Normal, 0.0)));

    v_Color = getAmbientLighting();
    v_Color += getDirectionalLighting();
    v_Color += getPointLighting();

    gl_Position = u_MVPMatrix * a_Position;
}

vec3 getAmbientLighting() {
    return materialColor * 0.2;
}

vec3 getDirectionalLighting()
{
    float diffuse = max(dot(eyeSpaceNormal, u_VectorToLight), 0.0);
    return materialColor * 0.3 * diffuse;
}
vec3 getPointLighting() {
    vec3 lightingSum = vec3(0.0);
    for (int i = 0;i < 3;i++) {
        vec3 toPointLight = vec3(u_PointLightPositions[i]) - vec3(eyeSpacePosition);
        float distance = length(toPointLight);
        toPointLight = normalize(toPointLight);

        float cosine = max(dot(eyeSpaceNormal, toPointLight), 0.0);
        lightingSum += (materialColor * u_PointLightColors[i] * 5.0 * cosine) / distance;
    }
    return lightingSum;
}
