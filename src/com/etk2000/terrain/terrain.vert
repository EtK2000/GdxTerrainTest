attribute vec4 a_position;
attribute vec3 a_normal;
attribute vec2 a_texCoord;
attribute vec4 a_color;

uniform mat4 u_MVPMatrix;
uniform mat3 u_normalMatrix;

uniform vec3 u_lightPosition;

varying float intensity;
varying vec2 texCoords;
varying vec4 v_color;

void main()
{
	vec3 normal = normalize(u_normalMatrix * a_normal);
	vec3 light = normalize(u_lightPosition);
	intensity = max(dot(normal, light), 0.0);

	v_color = a_color;
	texCoords = a_texCoord;

	gl_Position = u_MVPMatrix * a_position;
}