#ifdef GL_ES
	precision mediump float;
#endif

//uniform vec4 u_ambientColor;
//uniform vec4 u_diffuseColor;
//uniform vec4 u_specularColor;

uniform sampler2D u_texture;
varying vec2 texCoords;
varying vec4 v_color;

varying float intensity;

void main()
{
	gl_FragColor = v_color * intensity * texture2D(u_texture, texCoords);
}