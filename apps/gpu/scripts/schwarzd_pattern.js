var uiParams = [
    {
        name: "thickness",
        desc: "Thickness",
        type: "double",
        rangeMin: 0.005,
        rangeMax: 0.05,
        step: 0.001,
        defaultVal: 0.01
    }
];
function main(args) {
    var radius = 25 * MM;

    var base = new Sphere(radius);
    var pattern = new VolumePatterns.SchwarzD();
    pattern.set("period", 0.5*radius);
    pattern.set("thickness", 0.5*MM);
    pattern.set("level", 0);

    var shape = new Embossing(base, pattern);
    shape.set("minValue", -0.4*MM);
    shape.set("maxValue", 0.0*MM);
    shape.set("factor", 0.8);
    shape.set("blend", 0.5*MM);

    var r = radius+1*MM;
    return new Shape(shape,new Bounds(-r,r,-r,r,-r,r));

}
