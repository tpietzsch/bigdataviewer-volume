__kernel void filltexture(
		__write_only image2d_t target )
{
	const int2 opos =
	{
		get_global_id(0),
		get_global_id(1)
	};
	float f = ( ( float ) opos.x + opos.y ) / 2560;
	write_imagef( target, opos, f );
}
