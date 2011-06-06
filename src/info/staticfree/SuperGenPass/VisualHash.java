package info.staticfree.SuperGenPass;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.Paint.Style;
import android.graphics.Path;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;

public class VisualHash extends Drawable {

	private MessageDigest mHash;

	private final Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);

	public VisualHash() {
		super();
		try {
			mHash = MessageDigest.getInstance("SHA-1");
		} catch (final NoSuchAlgorithmException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();

		}

		p.setStyle(Style.FILL);
		p.setStrokeWidth(0);
	}

	public void updateData(byte[] input){
		mHash.update(input);
		invalidateSelf();
	}

	public void setData(byte[] input){
		mHash.reset();
		mHash.update(input);
		invalidateSelf();
	}

	private static final int

		TYPE_CIRCLE = 0x00,
		TYPE_SQUARE = 0x01,
		TYPE_STAR   = 0x02,
		TYPE_TRIANGLE = 0x03;

	private static final int
		BYTES_PER_SHAPE = 2,

		TYPEMASK = 0x3,
		TYPEBYTE = 0,

		X_OFFSET = 3,
		X_MAX = 0x3,
		X_MASK = X_MAX << X_OFFSET,
		X_BYTE = 0,

		Y_OFFSET = 6,
		Y_MAX = 0x3,
		Y_MASK = Y_MAX << Y_OFFSET,
		Y_BYTE = 0,

		R_OFFSET = 0,
		R_MAX = 0x3,
		R_MASK = R_MAX << R_OFFSET,
		R_BYTE = 1,

		G_OFFSET = 3,
		G_MAX = 0x3,
		G_MASK = G_MAX << G_OFFSET,
		G_BYTE = 1,

		B_OFFSET = 6,
		B_MAX = 0x2,
		B_MASK = B_MAX << B_OFFSET,
		B_BYTE = 1
		;

	private static int
		PADDING = 8,
		RADIUS = 8;
	private static Path TRIANGLE = new Path();
	private static Path STAR = new Path();


	static {
		TRIANGLE.moveTo(-RADIUS, RADIUS);
		TRIANGLE.lineTo(RADIUS, RADIUS); // _
		TRIANGLE.lineTo(0, -RADIUS);
		TRIANGLE.lineTo(-RADIUS, RADIUS);

		STAR.lineTo(0.000000f,-8.475681f);
		STAR.lineTo(1.893601f,-2.597389f);
		STAR.lineTo(8.069343f,-2.612960f);
		STAR.lineTo(3.063910f,1.004453f);
		STAR.lineTo(4.987128f,6.873122f);
		STAR.lineTo(0.000000f,3.230514f);
		STAR.lineTo(-4.987129f,6.873122f);
		STAR.rLineTo(1.923218f,-5.868669f);
		STAR.rLineTo(-5.005433f,-3.617414f);
		STAR.rLineTo(6.175743f,0.015571f);
		STAR.lineTo(0.000000f,-8.475681f);
	}

	@Override
	public void draw(Canvas canvas) {
		final byte[] hash = mHash.digest();

		final Rect bounds = getBounds();
		final int width = bounds.width() - PADDING * 2;
		final int height = bounds.height() - PADDING * 2;

		for (int offset = 0; offset < (hash.length); offset += BYTES_PER_SHAPE){
			final int type = hash[TYPEBYTE + offset] & TYPEMASK;
			final int x = (hash[X_BYTE + offset] & X_MASK) >> X_OFFSET;
			final int y = (hash[Y_BYTE + offset] & Y_MASK) >> Y_OFFSET;

			p.setARGB(200,
					scaleInt(R_MAX, (hash[R_BYTE + offset] & R_MASK) >> R_OFFSET, 255),
					scaleInt(G_MAX, (hash[G_BYTE + offset] & G_MASK) >> G_OFFSET, 255),
					scaleInt(B_MAX, (hash[B_BYTE + offset] & B_MASK) >> B_OFFSET, 255)
					);
			final float xScaled = PADDING + scale(X_MAX, x, width);
			final float yScaled = PADDING + scale(Y_MAX, y, height);
			canvas.save();
			canvas.translate(xScaled, yScaled);

			switch (type){
			case TYPE_STAR:
				canvas.drawPath(STAR, p);
				break;
			case TYPE_CIRCLE:

				canvas.drawCircle(0, 0, RADIUS, p);
				break;

			case TYPE_TRIANGLE:

				canvas.drawPath(TRIANGLE, p);
				break;
			case TYPE_SQUARE:

				canvas.drawRect( -RADIUS, -RADIUS, RADIUS, RADIUS, p);
				break;
			}
			canvas.restore();

		}
	}

	private int scaleInt(int valueMax, int value, int max){
		return (int)((value / ((float)valueMax)) * max);
	}

	private float scale(int valueMax, int value, int max){
		return (value / ((float)valueMax)) * max;
	}

	@Override
	public int getOpacity() {
		return PixelFormat.OPAQUE;
	}

	@Override
	public void setAlpha(int arg0) {
		// TODO Auto-generated method stub

	}

	@Override
	public void setColorFilter(ColorFilter arg0) {
		// TODO Auto-generated method stub

	}

	@Override
	public int getIntrinsicWidth() {

		return 100;
	}

	@Override
	public int getIntrinsicHeight() {

		return 100;
	}
}
