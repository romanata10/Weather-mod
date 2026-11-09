#version 150
// Original bounded raymarch through a tapering, swirling density field.
// PMWeather shaders and textures are NOT copied or overridden here.
in vec2 volumeUV;
in vec4 volumeTint;
out vec4 fragColor;

float hash31(vec3 p) {
    p = fract(p * 0.1031);
    p += dot(p, p.yzx + 19.19);
    return fract((p.x + p.y) * p.z);
}
float density(vec3 p) {
    float height = clamp(p.y, 0.0, 1.0);
    float radius = mix(0.10, 0.75, pow(height, 1.25));
    float swirl = 0.09 * sin(13.0 * height + atan(p.z, p.x) * 4.0);
    float distanceToAxis = length(p.xz);
    float shell = 1.0 - smoothstep(0.0, 0.14, abs(distanceToAxis - radius + swirl));
    float innerMist = 0.25 * (1.0 - smoothstep(0.0, radius, distanceToAxis));
    float breakup = 0.65 + 0.35 * hash31(floor(p * 24.0));
    return (shell + innerMist) * breakup * smoothstep(0.0, 0.1, height)
           * (1.0 - smoothstep(0.88, 1.0, height));
}
void main() {
    vec2 coord = vec2((volumeUV.x - 0.5) * 2.0, 1.0 - volumeUV.y);
    if (coord.y < 0.0 || coord.y > 1.0 || abs(coord.x) > 0.99) discard;
    float accumulated = 0.0;
    float light = 0.0;
    for (int i = 0; i < 24; ++i) {
        float depth = (float(i) + 0.5) / 24.0;
        vec3 samplePoint = vec3(coord.x, coord.y, depth * 2.0 - 1.0);
        float d = density(samplePoint) * 0.08;
        light += (1.0 - accumulated) * d * (0.5 + 0.5 * depth);
        accumulated += (1.0 - accumulated) * d;
    }
    if (accumulated < 0.012) discard;
    vec3 shaded = volumeTint.rgb * mix(0.42, 1.1, clamp(light * 3.0, 0.0, 1.0));
    fragColor = vec4(shaded, clamp(accumulated * volumeTint.a, 0.0, 0.84));
}
