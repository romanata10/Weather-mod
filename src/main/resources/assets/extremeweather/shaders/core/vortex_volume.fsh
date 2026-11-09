#version 150
// Original bounded raymarch through a tapering, swirling density field.
// PMWeather shaders and textures are NOT copied or overridden here.
in vec2 volumeUV;
in vec4 volumeTint;
uniform float GameTime;
out vec4 fragColor;

// Continuous angular/height variation, avoiding the old cubic voxel specks.
float hash21(vec2 p) {
    return fract(sin(dot(p, vec2(127.1, 311.7))) * 43758.5453);
}
float noise21(vec2 p) {
    vec2 cell = floor(p), blend = fract(p);
    blend = blend * blend * (3.0 - 2.0 * blend);
    return mix(mix(hash21(cell), hash21(cell + vec2(1, 0)), blend.x),
               mix(hash21(cell + vec2(0, 1)), hash21(cell + vec2(1, 1)), blend.x), blend.y);
}
float density(vec3 p) {
    float height = clamp(p.y, 0.0, 1.0);
    // A wandering axis and a low, diffuse ground skirt keep the silhouette
    // from reading as a rigid cylinder. All terms stay inside this one volume.
    vec2 wandering = vec2(sin(height * 8.0 + GameTime * 45.0),
                          cos(height * 6.0 - GameTime * 39.0)) * height * 0.09;
    p.xz -= wandering;
    float radius = mix(0.075, 0.70, pow(height, 1.32));
    float angle = atan(p.z, p.x);
    float rotation = angle * 4.0 + height * 14.0 + GameTime * 125.0;
    float gust = noise21(vec2(rotation * 0.8, height * 10.0 + GameTime * 35.0));
    float swirl = 0.07 * sin(rotation) + 0.12 * (gust - 0.5);
    float distanceToAxis = length(p.xz);
    float shell = 1.0 - smoothstep(0.03, 0.19, abs(distanceToAxis - radius + swirl));
    float innerMist = 0.18 * (1.0 - smoothstep(0.0, radius, distanceToAxis));
    float breakup = mix(0.48, 1.0, noise21(vec2(rotation * 1.5, height * 21.0 - GameTime * 20.0)));
    float skirt = (1.0 - smoothstep(0.02, 0.16, height))
                  * (1.0 - smoothstep(0.14, 0.86, distanceToAxis))
                  * (0.16 + 0.17 * gust);
    float wisps = (1.0 - smoothstep(0.10, 0.24, abs(distanceToAxis - radius - 0.17)))
                  * (0.05 + 0.12 * gust);
    return ((shell + innerMist + wisps) * breakup * smoothstep(0.0, 0.07, height)
           * (1.0 - smoothstep(0.88, 1.0, height))) + skirt;
}
void main() {
    vec2 coord = vec2((volumeUV.x - 0.5) * 2.0, 1.0 - volumeUV.y);
    if (coord.y < 0.0 || coord.y > 1.0 || abs(coord.x) > 0.99) discard;
    float accumulated = 0.0;
    float light = 0.0;
    for (int i = 0; i < 24; ++i) {
        float depth = (float(i) + 0.5) / 24.0;
        vec3 samplePoint = vec3(coord.x, coord.y, depth * 2.0 - 1.0);
        float d = density(samplePoint) * 0.36;
        light += (1.0 - accumulated) * d * (0.5 + 0.5 * depth);
        accumulated += (1.0 - accumulated) * d;
    }
    if (accumulated < 0.025) discard;
    vec3 shaded = volumeTint.rgb * mix(0.42, 1.1, clamp(light * 3.0, 0.0, 1.0));
    fragColor = vec4(shaded, clamp(accumulated * volumeTint.a, 0.0, 0.94));
}
