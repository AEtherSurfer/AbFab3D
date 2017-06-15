
/*
 * ***************************************************************************
 *                   Shapeways, Inc Copyright (c) 2017
 *                                Java Source
 *
 * This source is licensed under the GNU LGPL v2.1
 * Please read http://www.gnu.org/copyleft/lgpl.html for more information
 *
 * This software comes with the standard NO WARRANTY disclaimer for any
 * purpose. Use it at your own risk. If there's a problem you get to fix it.
 *
 * ***************************************************************************
 */

var types = [
  {
    name: "Gyroid",
    type: "UserDefined",
    properties: [
      {
        name: "period",
        desc: "Period",
        type: "double",
        rangeMin: 1,
        rangeMax: 21,
        step: 1,
        defaultVal: 18
      },
      {
        name: "thickness",
        desc: "Thickness",
        type: "double",
        rangeMin: 1,
        rangeMax: 5,
        step: 0.5,
        defaultVal: 2
      }
    ]
  }
];

var params = [
  {
    name: "radius",
    desc: "Radius",
    type: "double",
    rangeMin: 5,
    rangeMax: 50,
    step: 1,
    defaultVal: 25
  },

  {
    name: "gyroid1",
    desc: "Gyroid1",
    type: "Gyroid",
    group: "Gyroid 1",
    defaultVal: {period:17,thickness:1}
  }
];
function main(args) {
  print("args['radius']: " + args['radius']);
  print("args['gyroid1'].period: " + args['gyroid1'].period);
  print("args['gyroid1'].thickness: " + args['gyroid1'].thickness);
  var radius = args['radius'] * MM;
  var sphere = new Sphere(radius);
  var gyroid = new VolumePatterns.Gyroid(args['gyroid1'].period*MM, args['gyroid1'].thickness*MM);
  var intersect = new Intersection();
  intersect.setBlend(2*MM);
  intersect.add(sphere);
  intersect.add(gyroid);

  var s = radius;
  return new Scene(intersect,new Bounds(-s,s,-s,s,-s,s));
}
