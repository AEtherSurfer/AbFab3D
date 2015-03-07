var params = [
    {
        "id": "textpos0",
        "displayName": "Text Pos1",
        "type": "3dloc",
        "required": false,
        "default": ""
    },
    {
        "id": "textpos1",
        "displayName": "Text Pos2",
        "type": "3dloc",
        "required": false,
        "default": ""
    }
];

function getRotation(from, to){

	var a = new Vector3d();
	a.cross(from, to);
	var sina = a.length();
	   
	var cosa = to.dot(from);
	var angle = Math.atan2(sina, cosa);

	if(Math.abs(sina) < 1.e-8)
	   return new AxisAngle4d(1,0,0,0);

	a.normalize();
	return new AxisAngle4d(a.x,a.y,a.z,angle);
}
// calculates transformation of text to be placed between point p0 and p1 wih normals n0 and n1
function getTextTransform(p0, n0, p1, n1){

	// calculation of text transform
	var nn = new Vector3d();
	nn.add(n0,n1);
	nn.normalize();
	
	var center = new Vector3d();
	center.add(p0, p1);
	center.scale(0.5);
	var xdir = new Vector3d();
	xdir.sub(p1,p0);
	var len = xdir.length();
	xdir.normalize();
	
	var xaxis = new Vector3d(1,0,0);
	
	var aa1 = getRotation(xaxis, xdir);
	
	var m1 = new Matrix3d();
	m1.set(aa1);
	var zaxis = new Vector3d(0,0,1);
	m1.transform(zaxis); // zaxis after first rotation
	
	var aa2 = getRotation(zaxis, nn);
	var trans = new CompositeTransform();
	trans.add(new Rotation(aa1.x,aa1.y,aa1.z, aa1.angle));
	trans.add(new Rotation(aa2.x,aa2.y,aa2.z, aa2.angle));
	trans.add(new Translation(center));
	return trans;
}

function main(args) {
	var textpos0 = args['textpos0'];
	var textpos1 = args['textpos1'];
	var textBox = null;
	
	if (textpos0 && textpos1) {
	  var pos0Str = textpos0.split(",");
	  var pos1Str = textpos1.split(",");
	
	  var pos0 = [parseFloat(pos0Str[0]),parseFloat(pos0Str[1]),parseFloat(pos0Str[2])];
	  var normal0 = [parseFloat(pos0Str[3]),parseFloat(pos0Str[4]),parseFloat(pos0Str[5])];
	  var pos1 = [parseFloat(pos1Str[0]),parseFloat(pos1Str[1]),parseFloat(pos1Str[2])];
	  var normal1 = [parseFloat(pos1Str[3]),parseFloat(pos1Str[4]),parseFloat(pos1Str[5])];
	
	  var p0 = new Vector3d(pos0[0],pos0[1],pos0[2]);
	  var p1 = new Vector3d(pos1[0],pos1[1],pos1[2]);
	  var n0 = new Vector3d(normal0[0],normal0[1],normal0[2]);
	  var n1 = new Vector3d(normal1[0],normal1[1],normal1[2]);
	  n0.normalize();
	  n1.normalize();
	
	  var ttrans = getTextTransform(p0,n0,p1,n1);
	  var p01 = new Vector3d();
	  p01.sub(p1,p0);
	
	  var bx = p01.length();
	  var by = 5*MM;
	  var bz = 10*MM;
	  var vs = 0.1*MM;
		
	  textBox = new Text("TEXT text", "Times New Roman", bx, by, bz, vs);
	
	  textBox.getParam("rounding").setValue(0.*MM);
	  textBox.getParam("blurWidth").setValue(0.1*MM);
	  textBox.setTransform(getTextTransform(p0,n0,p1,n1));
	}
	
	var r = 12*MM;
	var shape = new Sphere(12*MM);
	r = 13*MM;
	
	if (textBox !== null) {
	  var eng = new Engraving(shape, textBox);
	  eng.getParam("depth").setValue(0.5*MM);
	  eng.getParam("blend").setValue(0.2*MM);
	  return new Shape(eng,new Bounds(-r,r,-r,r,-r,r));
	} else {
	  return new Shape(shape,new Bounds(-r,r,-r,r,-r,r));
	}
}
