function boxes(sizex, sizey, sizez, gap, count){
    var union = new Union();
	var step = sizex + gap;
	var step = sizex + gap;//10*MM;
	var x0 = -(step * (count-1)-gap)/2.;
	
	for(var i = 0; i < count; i++){
		
		union.add(new Box(x0 + i * step, 0, sizez/2, sizex, sizey, sizez));
	}
    return union;
}

function main(args) {
    var size = 0.04;
    var thickness = 0.01;
    var grid = createGrid(-16*MM,16*MM,-16*MM,16*MM,-16*MM,16*MM,0.1*MM);
	var sphere = new Sphere(15*MM);
	
	var box = new Box(0,0,15*MM, 25*MM, 10*MM, 25*MM);
	
	var box = boxes(3.5*MM, 8.*MM, 30.*MM, 0.5*MM, 6);
	
	var eng = new Engraving(sphere, box);
	
	eng.getParam("depth").setValue(0.5*MM);
	eng.getParam("blend").setValue(0.1*MM);
    var maker = new GridMaker();
    maker.setSource(eng);
    maker.makeGrid(grid);
    return grid;
}
