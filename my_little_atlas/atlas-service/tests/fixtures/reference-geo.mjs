export function wgsToGcj(lng,lat) {
  if(lng<72.004||lng>137.8347||lat<0.8293||lat>55.8271)return[lng,lat];
  const x=lng-105,y=lat-35,pi=Math.PI;
  let a=-100+2*x+3*y+.2*y*y+.1*x*y+.2*Math.sqrt(Math.abs(x));
  a+=(20*Math.sin(6*x*pi)+20*Math.sin(2*x*pi))*2/3+(20*Math.sin(y*pi)+40*Math.sin(y/3*pi))*2/3+(160*Math.sin(y/12*pi)+320*Math.sin(y*pi/30))*2/3;
  let b=300+x+2*y+.1*x*x+.1*x*y+.1*Math.sqrt(Math.abs(x));
  b+=(20*Math.sin(6*x*pi)+20*Math.sin(2*x*pi))*2/3+(20*Math.sin(x*pi)+40*Math.sin(x/3*pi))*2/3+(150*Math.sin(x/12*pi)+300*Math.sin(x/30*pi))*2/3;
  const rad=lat/180*pi,magic=1-.00669342162296594323*Math.sin(rad)**2,root=Math.sqrt(magic);
  return[lng+b*180/(6378245/root*Math.cos(rad)*pi),lat+a*180/(6378245*(1-.00669342162296594323)/(magic*root)*pi)];
}
export function distanceMeters(a,b) {
  const r=Math.PI/180,dLat=(b.lat-a.lat)*r,dLng=(b.lng-a.lng)*r;
  const h=Math.sin(dLat/2)**2+Math.cos(a.lat*r)*Math.cos(b.lat*r)*Math.sin(dLng/2)**2;
  return 6371000*2*Math.atan2(Math.sqrt(Math.min(1,h)),Math.sqrt(Math.max(0,1-h)));
}
export function gcjToWgs(lng,lat){
  let x=lng,y=lat;
  for(let i=0;i<4;i++){const [a,b]=wgsToGcj(x,y);x-=a-lng;y-=b-lat;}
  return [x,y];
}
// Stored places and browser positions use WGS-84. AMap renders GCJ-02.
export function toMapLatLng(place){
  const [lng,lat]=Number.isFinite(place.gcjLng)&&Number.isFinite(place.gcjLat)
    ?[place.gcjLng,place.gcjLat]:wgsToGcj(place.lng,place.lat);
  return [lat,lng];
}
export function fromMapLatLng(point){
  const [lng,lat]=gcjToWgs(point.lng,point.lat);
  return {lng,lat};
}
