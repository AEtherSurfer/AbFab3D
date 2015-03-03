function main(args) {
    var R = 20 * MM;
	var r = 5*MM;
	
    var grid = createGrid(-25*MM,25*MM,-25*MM,25*MM,-25*MM,25*MM,0.1*MM);
	
    var torus = new Torus(new Vector3d(0,0,0),R,r);
    var sphere = new Sphere(new Vector3d(0,0,0), R-r);
	sphere.setTransform(new Translation(2*r,0,0));
	
	var union = new Union(torus, sphere);
	
	var s = 25*MM;
	return new Shape(union,new Bounds(-s,s,-s,s,-s,s));

}
