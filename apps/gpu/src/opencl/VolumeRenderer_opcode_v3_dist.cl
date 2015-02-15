#define tstep (2.0 / maxSteps)
#define sstep (2.0 / maxShadowSteps)
#define clearColor (1,1,1)
#define clearInt 33554431 // White color
//#define clearInt 23004431 // purple color

#define boxMin ((float4)(-1.0f, -1.0f, -1.0f,1.0f))
#define boxMax ((float4)(1.0f, 1.0f, 1.0f,1.0f))

/*  Alan: removed as values are different in ShapeJS_opcode_v3_dist.cl
#define oSPHERE  0
#define oBOX  1
#define oGYROID  2
#define oINTERSECTION  3
#define oUNION  4
#define oTORUS  5
#define oINTERSECTION_START  6
#define oINTERSECTION_MID  7
#define oINTERSECTION_END  8
#define oUNION_START  9
#define oUNION_MID  10
#define oUNION_END  11
#define oSUBTRACTION_START  12
#define oSUBTRACTION_END  13
#define oRESET 1000
#define oSCALE  1001       
#define oTRANSLATION 1002
#define oROTATION 1003
*/

////////////////////////////////////////////////////////////////////////////////////////////////////
// intersect ray with a box
// http://www.siggraph.org/education/materials/HyperGraph/raytrace/rtinter3.htm

int intersectBox(float4 r_o, float4 r_d, float4 boxmin, float4 boxmax, float *tnear, float *tfar)
{
    // compute intersection of ray with all six bbox planes
    float4 invR = (float4)(1.0f,1.0f,1.0f,1.0f) / r_d;
    float4 tbot = invR * (boxmin - r_o);
    float4 ttop = invR * (boxmax - r_o);

    // re-order intersections to find smallest and largest on each axis
    float4 tmin = min(ttop, tbot);
    float4 tmax = max(ttop, tbot);

    // find the largest tmin and the smallest tmax
    float largest_tmin = max(max(tmin.x, tmin.y), max(tmin.x, tmin.z));
    float smallest_tmax = min(min(tmax.x, tmax.y), min(tmax.x, tmax.z));

	*tnear = largest_tmin;
	*tfar = smallest_tmax;

	return smallest_tmax > largest_tmin;
}

int intersectBox3(float3 r_o, float3 r_d, float3 boxmin, float3 boxmax, float *tnear, float *tfar)
{
    // compute intersection of ray with all six bbox planes
    float3 invR = (float3)(1.0f,1.0f,1.0f) / r_d;
    float3 tbot = invR * (boxmin - r_o);
    float3 ttop = invR * (boxmax - r_o);

    // re-order intersections to find smallest and largest on each axis
    float3 tmin = min(ttop, tbot);
    float3 tmax = max(ttop, tbot);

    // find the largest tmin and the smallest tmax
    float largest_tmin = max(max(tmin.x, tmin.y), max(tmin.x, tmin.z));
    float smallest_tmax = min(min(tmax.x, tmax.y), min(tmax.x, tmax.z));

	*tnear = largest_tmin;
	*tfar = smallest_tmax;

	return smallest_tmax > largest_tmin;
}

uint rgbaFloatToInt(float3 rgb)
{
    rgb.x = clamp(rgb.x,0.0f,1.0f);  
    rgb.y = clamp(rgb.y,0.0f,1.0f);  
    rgb.z = clamp(rgb.z,0.0f,1.0f);  
    return ((uint)(1)<<24) | ((uint)(rgb.z*255.0f)<<16) | ((uint)(rgb.y*255.0f)<<8) | (uint)(rgb.x*255.0f);
}

float4 mulMatVec4(global const float*mat, float4 vec){
	float f0 = dot(vec, ((float4)(mat[0],mat[1],mat[2],mat[3])));
	float f1 = dot(vec, ((float4)(mat[4],mat[5],mat[6],mat[7])));
	float f2 = dot(vec, ((float4)(mat[8],mat[9],mat[10],mat[11])));
	float f3 = 0;
	return (float4)(f0, f1, f2, f3);
}

// Utility functions
float step01_(float x, float x0, float vs){  // this is used for density
    if(x <= -vs)
        return 0.0;

    if(x >=  vs)
        return 1.0;

    return (x-(x0-vs))/(2*vs);
}

float step01(float x, float x0, float vs){ // used for distance
    return (x0-x);
}

float step10_(float x, float x0, float vs) { // this is used for density
   if(x <= x0 - vs)
       return 1.0;

   if(x >= x0 + vs)
       return 0.0;

    return ((x0+vs)-x)/(2*vs);
}


inline float getDensity(const global int * op, int opCount, float worldScale, float3 pos){

    sVec data;
    sVec pnt;
    pnt.v.xyz = pos * worldScale;

    getShapeJSData(op, opCount, &pnt, &data);

    float d = data.v.x;
    // convert distance to density 
    d = step10_(d,0,0.0001);
    return d;
}

float3 renderPixel(uint x, uint y, float u, float v, float tnear, float tfar, uint imageW, uint imageH, global const float* invViewMatrix,float worldScale, global const int * op, int opCount) {

    // calculate eye ray in world space
    float4 eyeRay_o;    // eye origin
    float4 eyeRay_d;    // eye direction

    eyeRay_o = (float4)(invViewMatrix[3], invViewMatrix[7], invViewMatrix[11], 1.0f);
				   
    float4 temp = normalize(((float4)(u, v, -2.f,0.0f)));
	eyeRay_d = mulMatVec4(invViewMatrix,temp);

//printf("x: %d y: %d eye_o: %v4f eye_d: %v4f\n",x,y,eyeRay_o,eyeRay_d);
    int hit = -1;
    // march along ray from tnear till we hit something
    float t = tnear;

    float4 tpos;
    float3 pos;
    float density;

    tpos = eyeRay_o + eyeRay_d*t;
	pos = tpos.xyz; 
    density = getDensity(op,opCount,worldScale,pos);
	if (density > 0.5){  // solid on the boundary
		return (float3)(1.f,0, 0);
	}

    for(uint i=0; i < maxSteps; i++) {
        tpos = eyeRay_o + eyeRay_d*t;
        pos = tpos.xyz; 

        density = getDensity(op,opCount,worldScale,pos);

        if (density > 0.5){  // overshot the surface 
			//return (float3)(1,0,0);
			int backcount = 10;
			while(density > 0.5 && backcount-- > 0){
			   t -= 0.1*tstep;  // back off
			   pos = (eyeRay_o + eyeRay_d*t).xyz;
			   density = getDensity(op,opCount,worldScale,pos);
			}			   
			// && density <= 1.) {
           hit = i;
		   // calculate gradient along the ray 
		   float dt = 0.01*tstep;
		   float3 p1 = (eyeRay_o + eyeRay_d*(t+dt)).xyz;
		   float gp = (getDensity(op,opCount,worldScale,p1)-density)/dt;
		   float ddt = (0.5-density)/gp;
		   //if( true ){
		   if( true) {//ddt > -0.5*tstep && ddt < 0.5*tstep){
				// adjust hit based on density to reduce aliasing
				pos = (eyeRay_o + eyeRay_d*(t + ddt)).xyz;
				break;
			}
		}
        t += tstep;
        if (t > tfar) break;
    }

    if ((hit != -1) && (x < imageW) && (y < imageH)) {
        // write output color
        uint i =(y * imageW) + x;

/*
        // fake shading shows steps distance
        float color = ((float) (maxSteps - hit))/maxSteps;
        float4 shading = (float4)(color,color,color,0.25f);
*/

        // use exact answer for a sphere
        //float3 grad = normalize((float3)(pos.x,pos.y,pos.z));

        // Gradient Calc - http://stackoverflow.com/questions/21272817/compute-gradient-for-voxel-data-efficiently
        float3 grad;
//        float dist = voxelSize / 2.0; // This is what I expect we should use or just voxelSize
        float dist = tstep*0.01;

        // second order precision formula for gradient
        // x
        float xd0 = getDensity(op,opCount,worldScale,(float3) (pos.x + dist, pos.y, pos.z));
        float xd2 = getDensity(op,opCount,worldScale,(float3) (pos.x - dist, pos.y, pos.z));
        grad.x = (xd2 - xd0)/(2*dist);
        //grad.x = (xd1 - xd0) * (1.0f - dist) + (xd2 - xd1) * dist; // lerp
        // y
        float yd0 = getDensity(op,opCount,worldScale,(float3) (pos.x,pos.y + dist, pos.z));
        float yd2 = getDensity(op,opCount,worldScale,(float3) (pos.x, pos.y - dist, pos.z));
        grad.y = (yd2 - yd0)/(2*dist);
        //grad.y = (yd1 - yd0) * (1.0f - dist) + (yd2 - yd1) * dist; // lerp
        // z
        float zd0 = getDensity(op,opCount,worldScale,(float3) (pos.x,pos.y, pos.z + dist));
        float zd2 = getDensity(op,opCount,worldScale,(float3) (pos.x, pos.y, pos.z - dist));
        grad.z = (zd2 - zd0)/(2*dist);
        //grad.z = (zd1 - zd0) * (1.0f - dist) + (zd2 - zd1) * dist; // lerp

        // TODO: hardcode headlight from eye direction
        // from this equation: http://en.wikipedia.org/wiki/Phong_reflection_model
/*
        float ambient = 0.1;
        float3 lm = (float3) (eyeRay_o.x - pos.x,eyeRay_o.y - pos.y, eyeRay_o.z - pos.z);
        float3 n = normalize(grad);  //  use gradient for normal at the surface
        float3 shading = dot(normalize(lm),n) + ambient;
        //float3 shading = normalize(grad);
*/

        float3 n = normalize(grad);  //  use gradient for normal at the surface

        // matlab style lighting
        float3 ambient = (float3) (0.1,0.1,0.1);
        float4 light1a =  (float4)(10.f,0, 20.f,0);//float (float3)(-10,0,20);
        float3 light1_color = (float3) (0.8f,0,0);
        float4 light2a = (float4)(10.f, 10.f, 20.f,0);// (float3)(-10,-10,20);
        float3 light2_color = (float3) (0,0.8f,0);
        float4 light3a = (float4)(0.f, 10.f, 20.f,0);//(float3)(0,-10,20);
        float3 light3_color = (float3) (0,0,0.8f);

/*
        // tony lighting
        //float3 ambient = (float3) (0.1,0.1,0.1);
//        float3 ambient = (float3) (0.1,0.1,0.1);
        float3 ambient = (float3) (0.4,0.4,0.4);
        float lscale = 0.65;
        float key = 0.8f * lscale;
        float fill = 0.25f * lscale;
        float rim = 1.0f * lscale;
        float3 light_color = (float3) (255.0/255.0 * lscale, 255/255.0 * lscale, 251.0 / 255.0 * lscale);  // high noon sun
        float4 light1a =  (float4)(6.5f,-6.5f, 10.f,0);  // key light
//        float3 light1_color = (float3) (key,key,key);
        float3 light1_color = key * light_color;
        float4 light2a = (float4)(10.f, 1.f, -10.f,0);  // fill light
        float3 light2_color = fill * light_color;
        float4 light3a = (float4)(-10.f, 9.0f, 10.f,0);  // rim light
        float3 light3_color = rim * light_color;
*/
        // WSF params
//        float3 mat_diffuse = (float3) 0.831;
        float3 mat_diffuse = (float3) 1;

		float3 light1, light2, light3;

        light1 = mulMatVec4(invViewMatrix,light1a).xyz; 
        light2 = mulMatVec4(invViewMatrix,light2a).xyz; 
        light3 = mulMatVec4(invViewMatrix,light3a).xyz; 

/*
        // fixed lighting
        light1 = light1a;
        light2 = light2a;
        light3 = light3a;
*/
		float3 light1_sum = 0;
		float3 light2_sum = 0;
		float3 light3_sum = 0;

        #ifdef SHADOWS
        if (canSee(pos,light1
           #ifdef DEBUG
              ,debug
           #endif
        )) light1_sum = dot(normalize(light1),n) * light1_color;
        if (canSee(pos,light2
           #ifdef DEBUG
              ,debug
           #endif
        )) light2_sum = dot(normalize(light2),n) * light2_color;
        if (canSee(pos,light3
           #ifdef DEBUG
              ,debug
           #endif
        )) light3_sum = dot(normalize(light3),n) * light3_color;
        #endif

        #ifndef SHADOWS
           light1_sum = dot(normalize(light1),n) * light1_color * mat_diffuse;
           light2_sum = dot(normalize(light2),n) * light2_color * mat_diffuse;
           light3_sum = dot(normalize(light3),n) * light3_color * mat_diffuse;
        #endif

//        float3 shading = light1_sum + light2_sum + light3_sum + ambient;
        float3 shading = fabs(light1_sum) + fabs(light2_sum) + fabs(light3_sum) + ambient;

        return shading;
    }

    return (float3)clearColor;
}


kernel void renderSuper(global uint *d_output, uint x0, uint y0, uint tileW, uint tileH, uint imageW, uint imageH, global const float* invViewMatrix, float worldScale, global const int * op, int opCount) {
    uint x = get_global_id(0);
    uint y = get_global_id(1);

    if ((x > tileW) || (y > tileH)) {
        return;
    }

    if ((x > tileW) || (y > tileH)) {
        return;
    }

    float u = ((x + x0) / (float) imageW)*2.0f-1.0f;
    float v = ((y + y0) / (float) imageH)*2.0f-1.0f;


    // calculate eye ray in world space
    float4 eyeRay_o;    // eye origin
    float4 eyeRay_d;    // eye direction

    eyeRay_o = (float4)(invViewMatrix[3], invViewMatrix[7], invViewMatrix[11], 1.0f);

    float4 temp = normalize(((float4)(u, v, -2.0f,0.0f)));
    eyeRay_d.x = dot(temp, ((float4)(invViewMatrix[0],invViewMatrix[1],invViewMatrix[2],invViewMatrix[3])));
    eyeRay_d.y = dot(temp, ((float4)(invViewMatrix[4],invViewMatrix[5],invViewMatrix[6],invViewMatrix[7])));
    eyeRay_d.z = dot(temp, ((float4)(invViewMatrix[8],invViewMatrix[9],invViewMatrix[10],invViewMatrix[11])));
    eyeRay_d.w = 0.0f;


    // find intersection with box
    float tnear, tfar;
    int hit = intersectBox(eyeRay_o, eyeRay_d, boxMin, boxMax, &tnear, &tfar);

    if (!hit) {
        if ((x < tileW) && (y < tileH)) {
            // write output color
            uint i =(y * tileW) + x;
            d_output[i] = clearInt;

            return;
        }
    }

    tnear = clamp(tnear,0.0f,tfar);   // clamp to near plane

    float subPixel = (1 / (float) imageW)*2.0f / (2) / 2;  // harcoded samples as define not working

    // TODO: we should change to rotated grid pattern: http://en.wikipedia.org/wiki/Supersampling
    // TODO: we can factor out the bounding box test to speed this up

    float3 sum = (float3)(0,0,0);

    sum += renderPixel(x,y,u - subPixel,v - subPixel,tnear,tfar,imageW,imageH,invViewMatrix,worldScale,op,opCount);
    sum += renderPixel(x,y,u + subPixel,v - subPixel,tnear,tfar,imageW,imageH,invViewMatrix,worldScale,op,opCount);
    sum += renderPixel(x,y,u - subPixel,v + subPixel,tnear,tfar,imageW,imageH,invViewMatrix,worldScale,op,opCount);
    sum += renderPixel(x,y,u + subPixel,v + subPixel,tnear,tfar,imageW,imageH,invViewMatrix,worldScale,op,opCount);

    float3 shading = sum / 4;

    shading = clamp(shading, 0.0f, 1.0f);

    uint idx =(y * tileW) + x;
    d_output[idx] = rgbaFloatToInt(shading);
}

kernel void render(global uint *d_output, uint x0, uint y0, uint tileW, uint tileH, uint imageW, uint imageH, global const float* invViewMatrix, float worldScale, global const int * op, int opCount) {

    uint x = get_global_id(0);
    uint y = get_global_id(1);


    if ((x > tileW) || (y > tileH)) {
        return;
    }

    float3 sum = (float3)(0,0,0);
    float u = ((x + x0) / (float) imageW)*2.0f-1.0f;
    float v = ((y + y0) / (float) imageH)*2.0f-1.0f;


    // calculate eye ray in world space
    float4 eyeRay_o;    // eye origin
    float4 eyeRay_d;    // eye direction

    eyeRay_o = (float4)(invViewMatrix[3], invViewMatrix[7], invViewMatrix[11], 1.0f);

    float4 temp = normalize(((float4)(u, v, -2.0f,0.0f)));
    eyeRay_d.x = dot(temp, ((float4)(invViewMatrix[0],invViewMatrix[1],invViewMatrix[2],invViewMatrix[3])));
    eyeRay_d.y = dot(temp, ((float4)(invViewMatrix[4],invViewMatrix[5],invViewMatrix[6],invViewMatrix[7])));
    eyeRay_d.z = dot(temp, ((float4)(invViewMatrix[8],invViewMatrix[9],invViewMatrix[10],invViewMatrix[11])));
    eyeRay_d.w = 0.0f;


    // find intersection with box
    float tnear, tfar;
    int hit = intersectBox(eyeRay_o, eyeRay_d, boxMin, boxMax, &tnear, &tfar);

    if (!hit) {
        if ((x < tileW) && (y < tileH)) {
            // write output color
            uint i =(y * tileW) + x;
            d_output[i] = clearInt;

            return;
        }
    }
    tnear = clamp(tnear,0.0f,tfar);   // clamp to near plane

    float3 shading = renderPixel(x,y,u,v,tnear,tfar,imageW,imageH,invViewMatrix,worldScale,op,opCount);

    shading = clamp(shading, 0.0f, 1.0f);

    uint idx =(y * tileW) + x;
    d_output[idx] = rgbaFloatToInt(shading);
}
